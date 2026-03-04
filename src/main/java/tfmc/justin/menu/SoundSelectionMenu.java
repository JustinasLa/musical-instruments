package tfmc.justin.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.InstrumentManager;

import java.util.*;

// ====================================
// Manages the sound selection menu GUI
// Displays all available sounds for a specific instrument
// ====================================
public class SoundSelectionMenu {
    
    private final InstrumentPlugin plugin;
    private final InstrumentManager instrumentManager;
    
    public static final String MENU_TITLE_PREFIX = "Sound Selection";
    public static final int BACK_BUTTON_SLOT = 18; // Bottom left (3-row menu)
    public static final int PREVIOUS_PAGE_SLOT = 19; // Bottom row
    public static final int PAGE_INFO_SLOT = 22; // Bottom center
    public static final int NEXT_PAGE_SLOT = 25; // Bottom row
    public static final int ADD_BUTTON_SLOT = 26; // Bottom right
    
    private static final int SOUNDS_PER_PAGE_CUSTOM = 16; // 2 rows of 8 sounds each (skipping last slot)
    private static final int SOUNDS_PER_PAGE_VANILLA = 18; // 2 rows of 9 sounds each (using all slots)
    
    // Track which slot the player is filling
    private final Map<UUID, Integer> playerTargetSlots = new HashMap<>();
    private final Map<UUID, String> playerSelectedInstruments = new HashMap<>();
    private final Map<UUID, String> playerSelectedSounds = new HashMap<>();
    private final Map<UUID, VanillaSoundCategory> playerSelectedVanillaCategory = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>(); // Track current page per player
    private final Map<UUID, String> playerCurrentlyPlayingSound = new HashMap<>(); // Track currently playing preview sound
    
    public SoundSelectionMenu(InstrumentPlugin plugin, InstrumentManager instrumentManager) {
        this.plugin = plugin;
        this.instrumentManager = instrumentManager;
    }
    
    // Get sounds per page based on whether it's vanilla or custom
    private int getSoundsPerPage(Player player) {
        VanillaSoundCategory category = playerSelectedVanillaCategory.get(player.getUniqueId());

        return (category != null) ? SOUNDS_PER_PAGE_VANILLA : SOUNDS_PER_PAGE_CUSTOM;
    }
    
    // Check if currently viewing vanilla sounds
    private boolean isViewingVanillaSounds(Player player) {
        return playerSelectedVanillaCategory.get(player.getUniqueId()) != null;
    }
    
    // Opens the sound selection menu for a player
    public void openMenu(Player player, String instrumentId, int targetSlot) {
        // Store the target slot and instrument for this player
        playerTargetSlots.put(player.getUniqueId(), targetSlot);
        playerSelectedInstruments.put(player.getUniqueId(), instrumentId);
        playerSelectedVanillaCategory.remove(player.getUniqueId()); // Not a vanilla category
        playerCurrentPage.put(player.getUniqueId(), 0); // Start at page 0
        
        // Get all sounds for this instrument
        List<String> sounds = getSoundsForInstrument(instrumentId);
        
        openSoundMenu(player, sounds, formatInstrumentName(instrumentId));
    }
    
    // ====================================
    // Opens the sound selection menu for a vanilla category
    // ====================================
   public void openMenuForVanillaCategory(Player player, VanillaSoundCategory category, int targetSlot) {
        // Store the target slot and category for this player
        playerTargetSlots.put(player.getUniqueId(), targetSlot);
        playerSelectedVanillaCategory.put(player.getUniqueId(), category);
        playerSelectedInstruments.remove(player.getUniqueId()); // Not a custom instrument
        playerCurrentPage.put(player.getUniqueId(), 0); // Start at page 0
        
        // Get all sounds for this category
        List<String> sounds = category.getSounds();
        
        openSoundMenu(player, sounds, category.getName());
    }
    
    // ====================================
    // Common method to open sound menu with given sounds
    // ====================================
    private void openSoundMenu(Player player, List<String> sounds, String displayName) {
        // Get current page
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = getSoundsPerPage(player);
        boolean isVanilla = isViewingVanillaSounds(player);
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) sounds.size() / soundsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        // Ensure current page is within bounds
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
            playerCurrentPage.put(player.getUniqueId(), currentPage);
        }
        if (currentPage < 0) {
            currentPage = 0;
            playerCurrentPage.put(player.getUniqueId(), currentPage);
        }
        
        // Use 27 slots (3 rows) 2 rows for sounds, 1 row for buttons
        int size = 27;
        
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, size, MENU_TITLE_PREFIX + displayName);
        
        // Calculate which sounds to show on this page
        int startIndex = currentPage * soundsPerPage;
        int endIndex = Math.min(startIndex + soundsPerPage, sounds.size());
        
        // Add each sound to the menu
        int soundIndex = startIndex;
        for (int slot = 0; slot < 18 && soundIndex < endIndex; slot++) {
            // Skip the 9th slot of each row ONLY for custom instruments (not vanilla)
            if (!isVanilla && (slot + 1) % 9 == 0) {
                continue;
            }
            
            String sound = sounds.get(soundIndex);
            ItemStack soundItem = createSoundMenuItem(sound, false);
            menu.setItem(slot, soundItem);

            soundIndex++;
        }
        
        // Add the "Back" button at slot 18 (bottom left)
        ItemStack backButton = createBackButton();
        menu.setItem(BACK_BUTTON_SLOT, backButton);
        
        // Add pagination buttons if more than one page
        if (totalPages > 1) {
            // Add previous page button
            if (currentPage > 0) {
                ItemStack prevButton = createPreviousPageButton();
                menu.setItem(PREVIOUS_PAGE_SLOT, prevButton);
            }
            
            // Add page info
            ItemStack pageInfo = createPageInfoItem(currentPage + 1, totalPages, soundsPerPage);
            menu.setItem(PAGE_INFO_SLOT, pageInfo);
            
            // Add next page button
            if (currentPage < totalPages - 1) {
                ItemStack nextButton = createNextPageButton();
                menu.setItem(NEXT_PAGE_SLOT, nextButton);
            }
        }
        
        // Add the "Add" button at slot 26 (bottom right)
        ItemStack addButton = createAddButton();
        menu.setItem(ADD_BUTTON_SLOT, addButton);
        
        // Open the inventory for the player
        player.openInventory(menu);
    }
    
    // ====================================
    // Get all sounds for an instrument from config
    // Ordered: single sounds first (row 1), then sneak/chord sounds (row 2)
    // ====================================
    private List<String> getSoundsForInstrument(String instrumentId) {
        Map<Integer, String> singleSounds = new TreeMap<>();
        Map<Integer, String> sneakSounds = new TreeMap<>();
        
        Set<String> keys = plugin.getConfig().getConfigurationSection(instrumentId + ".hotbar-sounds").getKeys(false);
        
        for (String key : keys) {
            // Skip volume and pitch
            if (key.equals("volume") || key.equals("pitch")) {
                continue;
            }
            
            String sound = plugin.getConfig().getString(instrumentId + ".hotbar-sounds." + key);
            if (sound == null) {
                continue;
            }
            
            // Determine if this is a sneak sound or single sound
            if (key.contains("+sneak")) {
                // Extract the number from the key (e.g., "1+sneak" -> 1)
                String numStr = key.replace("+sneak", "").trim();
                try {
                    int num = Integer.parseInt(numStr);
                    sneakSounds.put(num, sound);
                } catch (NumberFormatException e) {
                    // Skip if we cant parse the number
                }
            } else {
                // Single sound. Extract the number
                try {
                    int num = Integer.parseInt(key);
                    singleSounds.put(num, sound);
                } catch (NumberFormatException e) {
                    // Skip if we cant parse the number
                }
            }
        }
        
        // Combine: single sounds first, then sneak sounds
        List<String> sounds = new ArrayList<>();
        sounds.addAll(singleSounds.values());
        sounds.addAll(sneakSounds.values());
        
        return sounds;
    }
    
    // ====================================
    // Create a menu item for a sound
    // ====================================
    private ItemStack createSoundMenuItem(String sound, boolean selected) {
        // Change material when selected. Use lime dye for selected, note block for unselected
        Material material = selected ? Material.LIME_DYE : Material.NOTE_BLOCK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Format sound name
        String displayName = formatSoundName(sound);
        meta.setDisplayName((selected ? "§a§l" : "§e§l") + displayName);
        
        // Add lore
        List<String> lore = new ArrayList<>();
        lore.add("§7Sound: §f" + sound);
        lore.add("");

        if (selected) {
            lore.add("§a✓ Selected. Will be added");
            lore.add("§7Click to preview again");
        } else {
            lore.add("§7Click to preview this sound");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the "Add" button
    // ====================================
    private ItemStack createAddButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§a§lAdd Sound");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to add the selected");
        lore.add("§7sound to your recording");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the "Back" button
    // ====================================
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§lBack");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Return to instrument selection");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the "Previous Page" button
    // ====================================
    private ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§e§l← Previous Page");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to go to");
        lore.add("§7the previous page");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the "Next Page" button
    // ====================================
    private ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§e§lNext Page →");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to go to");
        lore.add("§7the next page");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the page info display item
    // ====================================
    private ItemStack createPageInfoItem(int currentPage, int totalPages, int soundsPerPage) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§6§lPage " + currentPage + " / " + totalPages);
        
        List<String> lore = new ArrayList<>();

        lore.add("§7Viewing sounds");
        lore.add("§7" + ((currentPage - 1) * soundsPerPage + 1) + " - " +  Math.min(currentPage * soundsPerPage, (currentPage - 1) * soundsPerPage + soundsPerPage));
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Format instrument ID into a nice display name
    // Example: "celtic_harp" -> "Celtic Harp"
    // ====================================
    private String formatInstrumentName(String instrumentId) {
        String[] words = instrumentId.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        
        return result.toString();
    }
    
    // ====================================
    // Format sound name into a nice display name
    // Example: "instruments.accordion_1c_single" -> "Accordion 1C Single"
    // ====================================
    private String formatSoundName(String sound) {
        // Remove "instruments." prefix if present
        String name = sound.replace("instruments.", "");
        
        // Replace underscores with spaces and capitalize
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        
        return result.toString();
    }
    
    // ====================================
    // Get the sound from a slot in the menu (accounting for skipped slots)
    // ====================================
    public String getSoundFromSlot(Player player, int slot) {
        List<String> sounds = getCurrentSounds(player);
        if (sounds == null) {
            return null;
        }
        
        // Get current page
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = getSoundsPerPage(player);
        boolean isVanilla = isViewingVanillaSounds(player);
        
        // Calculate the actual sound index
        int slotIndex;
        if (isVanilla) {
            // Vanilla: all slots used, no skipping
            slotIndex = slot;
        } else {
            // Custom instruments: skip slots 8 and 17
            slotIndex = slot - (slot / 9);
        }
        
        int soundIndex = (currentPage * soundsPerPage) + slotIndex;
        
        if (soundIndex >= 0 && soundIndex < sounds.size()) {
            return sounds.get(soundIndex);
        }
        
        return null;
    }
    
    // ====================================
    // Get current sounds (custom instrument or vanilla category)
    // ====================================
    private List<String> getCurrentSounds(Player player) {
        String instrumentId = playerSelectedInstruments.get(player.getUniqueId());
        VanillaSoundCategory category = playerSelectedVanillaCategory.get(player.getUniqueId());
        
        if (instrumentId != null) {
            return getSoundsForInstrument(instrumentId);
        } else if (category != null) {
            return category.getSounds();
        }
        
        return null;
    }
    
    // Set the currently selected sound for a player
    public void setSelectedSound(Player player, String sound) { playerSelectedSounds.put(player.getUniqueId(), sound);}
    
    // Get the currently selected sound for a player
    public String getSelectedSound(Player player) { return playerSelectedSounds.get(player.getUniqueId()); }
    
    // ====================================
    // Reopen the menu to refresh the display (e.g., after selecting a sound)
    // ====================================
    public void refreshMenu(Player player) {
        Integer targetSlot = playerTargetSlots.get(player.getUniqueId());

        if (targetSlot == null) {
            return;
        }
        
        player.closeInventory();
        
        String instrumentId = playerSelectedInstruments.get(player.getUniqueId());
        VanillaSoundCategory category = playerSelectedVanillaCategory.get(player.getUniqueId());
        
        if (instrumentId != null) {
            openMenuWithSelection(player, instrumentId, targetSlot, false);
        } else if (category != null) {
            openMenuWithSelection(player, null, targetSlot, true);
        }
    }
    
    // ====================================
    // Update menu items in place without closing inventory
    // Prevents cursor reset when selecting a sound
    // ====================================
    public void updateMenuInPlace(Player player) {
        List<String> sounds = getCurrentSounds(player);
        if (sounds == null) {
            return;
        }
        
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        if (openInventory == null) {
            return;
        }
        
        String selectedSound = getSelectedSound(player);
        
        // Get current page
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = getSoundsPerPage(player);
        boolean isVanilla = isViewingVanillaSounds(player);
        int startIndex = currentPage * soundsPerPage;
        int endIndex = Math.min(startIndex + soundsPerPage, sounds.size());
        
        // Update each sound item in the menu
        int soundIndex = startIndex;
        for (int slot = 0; slot < 18 && soundIndex < endIndex; slot++) {
            // Skip the 9th slot of each row ONLY for custom instruments (not vanilla)
            if (!isVanilla && (slot + 1) % 9 == 0) {
                continue;
            }
            
            String sound = sounds.get(soundIndex);
            boolean isSelected = sound.equals(selectedSound);
            ItemStack soundItem = createSoundMenuItem(sound, isSelected);

            openInventory.setItem(slot, soundItem);
            soundIndex++;
        }
    }
    
    // ====================================
    // Update entire page in place including pagination buttons
    // Prevents cursor reset when changing pages
    // ====================================
    private void updatePageInPlace(Player player) {
        List<String> sounds = getCurrentSounds(player);

        if (sounds == null) {
            return;
        }
        
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        if (openInventory == null) {
            return;
        }
        
        // Get current page and calculate totals
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = getSoundsPerPage(player);
        boolean isVanilla = isViewingVanillaSounds(player);
        int totalPages = (int) Math.ceil((double) sounds.size() / soundsPerPage);

        if (totalPages == 0)
        {
            totalPages = 1;
        }
        
        String selectedSound = getSelectedSound(player);
        int startIndex = currentPage * soundsPerPage;
        int endIndex = Math.min(startIndex + soundsPerPage, sounds.size());
        
        // Clear and update sound slots
        for (int slot = 0; slot < 18; slot++) {
            openInventory.setItem(slot, null); // Clear slot first
        }
        
        // Add sounds for current page
        int soundIndex = startIndex;
        for (int slot = 0; slot < 18 && soundIndex < endIndex; slot++) {
            // Skip the 9th slot of each row ONLY for custom instruments (not vanilla)
            if (!isVanilla && (slot + 1) % 9 == 0) {
                continue;
            }
            
            String sound = sounds.get(soundIndex);
            boolean isSelected = sound.equals(selectedSound);
            ItemStack soundItem = createSoundMenuItem(sound, isSelected);

            openInventory.setItem(slot, soundItem);
            soundIndex++;
        }
        
        // Update pagination buttons
        // Clear pagination slots first
        openInventory.setItem(PREVIOUS_PAGE_SLOT, null);
        openInventory.setItem(PAGE_INFO_SLOT, null);
        openInventory.setItem(NEXT_PAGE_SLOT, null);
        
        if (totalPages > 1) {
            // Add previous page button if not on first page
            if (currentPage > 0) {
                ItemStack prevButton = createPreviousPageButton();
                openInventory.setItem(PREVIOUS_PAGE_SLOT, prevButton);
            }
            
            // Add page info
            ItemStack pageInfo = createPageInfoItem(currentPage + 1, totalPages, soundsPerPage);
            openInventory.setItem(PAGE_INFO_SLOT, pageInfo);
            
            // Add next page button if not on last page
            if (currentPage < totalPages - 1) {
                ItemStack nextButton = createNextPageButton();
                openInventory.setItem(NEXT_PAGE_SLOT, nextButton);
            }
        }
    }
    
    // ====================================
    // Opens the sound selection menu with the current selection highlighted
    // ====================================
    private void openMenuWithSelection(Player player, String instrumentId, int targetSlot, boolean isVanilla) {
        // Get all sounds
        List<String> sounds;
        String displayName;
        
        if (isVanilla) {
            VanillaSoundCategory category = playerSelectedVanillaCategory.get(player.getUniqueId());
            if (category == null)
            {
                return;
            }

            sounds = category.getSounds();
            displayName = category.getName();
        } else {
            if (instrumentId == null)
            {
                return;
            }

            sounds = getSoundsForInstrument(instrumentId);
            displayName = formatInstrumentName(instrumentId);
        }
        
        String selectedSound = getSelectedSound(player);
        
        // Get current page
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = isVanilla ? SOUNDS_PER_PAGE_VANILLA : SOUNDS_PER_PAGE_CUSTOM;
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) sounds.size() / soundsPerPage);
        if (totalPages == 0)
        {
            totalPages = 1;
        }
        
        // Ensure current page is within bounds
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
            playerCurrentPage.put(player.getUniqueId(), currentPage);
        }
        if (currentPage < 0) {
            currentPage = 0;
            playerCurrentPage.put(player.getUniqueId(), currentPage);
        }
        
        // Use 27 slots (3 rows) - 2 rows for sounds, 1 row for buttons
        int size = 27;
        
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, size, MENU_TITLE_PREFIX + displayName);
        
        // Calculate which sounds to show on this page
        int startIndex = currentPage * soundsPerPage;
        int endIndex = Math.min(startIndex + soundsPerPage, sounds.size());
        
        // Add each sound to the menu
        int soundIndex = startIndex;
        for (int slot = 0; slot < 18 && soundIndex < endIndex; slot++) {
            // Skip the 9th slot of each row ONLY for custom instruments (not vanilla)
            if (!isVanilla && (slot + 1) % 9 == 0) {
                continue;
            }
            
            String sound = sounds.get(soundIndex);
            boolean isSelected = sound.equals(selectedSound);
            ItemStack soundItem = createSoundMenuItem(sound, isSelected);

            menu.setItem(slot, soundItem);
            soundIndex++;
        }
        
        // Add the "Back" button at slot 18 (bottom left)
        ItemStack backButton = createBackButton();
        menu.setItem(BACK_BUTTON_SLOT, backButton);
        
        // Add pagination buttons if more than one page
        if (totalPages > 1) {
            // Add previous page button
            if (currentPage > 0) {
                ItemStack prevButton = createPreviousPageButton();
                menu.setItem(PREVIOUS_PAGE_SLOT, prevButton);
            }
            
            // Add page info
            ItemStack pageInfo = createPageInfoItem(currentPage + 1, totalPages, soundsPerPage);
            menu.setItem(PAGE_INFO_SLOT, pageInfo);
            
            // Add next page button
            if (currentPage < totalPages - 1) {
                ItemStack nextButton = createNextPageButton();
                menu.setItem(NEXT_PAGE_SLOT, nextButton);
            }
        }
        
        // Add the "Add" button at slot 26 (bottom right)
        ItemStack addButton = createAddButton();
        menu.setItem(ADD_BUTTON_SLOT, addButton);
        
        // Open the inventory for the player
        player.openInventory(menu);
    }
    
    // ====================================
    // Preview a sound for the player
    // ====================================
    public void previewSound(Player player, String sound) {
        // Stop any currently playing preview sound first
        stopCurrentPreview(player);
        
        String instrumentId = playerSelectedInstruments.get(player.getUniqueId());
        VanillaSoundCategory category = playerSelectedVanillaCategory.get(player.getUniqueId());
        
        float volume = 1.0f;
        float pitch = 1.0f;
        
        // Get custom volume and pitch if this is a custom instrument
        if (instrumentId != null) {
            volume = (float) instrumentManager.getVolume(instrumentId);
            pitch = (float) instrumentManager.getPitch(instrumentId);
        }
        // Play the sound for the player
        player.playSound(player.getLocation(), sound, volume, pitch);
        
        // Track this sound as currently playing
        playerCurrentlyPlayingSound.put(player.getUniqueId(), sound);
    }
    
    // ====================================
    // Stop the current preview sound for a player
    // ====================================
    private void stopCurrentPreview(Player player) {
        String currentSound = playerCurrentlyPlayingSound.get(player.getUniqueId());

        if (currentSound != null) {
            player.stopSound(currentSound);
            playerCurrentlyPlayingSound.remove(player.getUniqueId());
        }
    }
    
    public int getTargetSlot(Player player) { return playerTargetSlots.getOrDefault(player.getUniqueId(), -1); }
    public String getSelectedInstrument(Player player) { return playerSelectedInstruments.get(player.getUniqueId()); }
    public VanillaSoundCategory getSelectedVanillaCategory(Player player) { return playerSelectedVanillaCategory.get(player.getUniqueId()); }
    
    // ====================================
    // Handle going to the next page
    // ====================================
    public void handleNextPage(Player player) {
        List<String> sounds = getCurrentSounds(player);
        if (sounds == null) return;
        
        // Stop any currently playing preview sound
        stopCurrentPreview(player);
        
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        int soundsPerPage = getSoundsPerPage(player);
        int totalPages = (int) Math.ceil((double) sounds.size() / soundsPerPage);
        
        if (currentPage < totalPages - 1) {
            playerCurrentPage.put(player.getUniqueId(), currentPage + 1);
            updatePageInPlace(player);
        }
    }
    
    // ====================================
    // Handle going to the previous page
    // ====================================
    public void handlePreviousPage(Player player) {
        // Stop any currently playing preview sound
        stopCurrentPreview(player);
        
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        
        if (currentPage > 0) {
            playerCurrentPage.put(player.getUniqueId(), currentPage - 1);
            updatePageInPlace(player);
        }
    }
    
    // ====================================
    // Clean up player data
    // ====================================
    public void cleanup(Player player) {
        // Stop any currently playing preview sound
        stopCurrentPreview(player);
        
        playerTargetSlots.remove(player.getUniqueId());
        playerSelectedInstruments.remove(player.getUniqueId());
        playerSelectedSounds.remove(player.getUniqueId());
        playerSelectedVanillaCategory.remove(player.getUniqueId());
        playerCurrentPage.remove(player.getUniqueId());
    }
}
