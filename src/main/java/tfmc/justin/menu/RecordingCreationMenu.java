package tfmc.justin.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tfmc.justin.InstrumentPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ====================================
// Manages the recording creation menu GUI
// Shows a formatted menu with gray glass panes and paper items
// ====================================
public class RecordingCreationMenu {
    
    private final InstrumentPlugin plugin;
    
    public static final String MENU_TITLE = "Recording Editor: Page %page%";
    
    // Slot positions for navigation and actions
    public static final int PREV_PAGE_SLOT = 45;
    public static final int PREVIEW_SLOT = 46;
    public static final int SAVE_SLOT = 47;
    public static final int PAGE_INFO_SLOT = 49;
    public static final int CANCEL_SLOT = 50;
    public static final int NEXT_PAGE_SLOT = 53;
    
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, Map<Integer, Double>> playerRowDelays = new HashMap<>();
    private final Map<UUID, Map<Integer, AssignedSound>> playerSlotSounds = new HashMap<>();
    private final Map<UUID, String> playerEditingId = new HashMap<>();

    public RecordingCreationMenu(InstrumentPlugin plugin) {
        this.plugin = plugin;
    }
    
    // ====================================
    // Load an existing recording into the editor for a player
    // ====================================
    public void loadRecordingForEditing(Player player, String recordingId) {
        UUID playerId = player.getUniqueId();

        // Clear all previous state
        playerSlotSounds.remove(playerId);
        playerRowDelays.remove(playerId);
        playerPages.remove(playerId);

        // Mark which recording is being edited
        playerEditingId.put(playerId, recordingId);

        // Load slot sounds
        Map<Integer, java.util.Map<String, String>> savedSlots = plugin.getRecordingsManager().getSavedSlots(recordingId);

        for (Map.Entry<Integer, java.util.Map<String, String>> entry : savedSlots.entrySet()) {
            int slot = entry.getKey();
            java.util.Map<String, String> data = entry.getValue();

            String soundName = data.get("sound");
            String type      = data.get("type");
            String source    = data.get("source");

            if (soundName == null || soundName.isEmpty())
            {
                continue;
            }

            AssignedSound assignedSound;

            if ("vanilla".equals(type)) {
                try {
                    VanillaSoundCategory cat = VanillaSoundCategory.valueOf(source);
                    assignedSound = new AssignedSound(cat, soundName);
                } catch (Exception e) {
                    // Unknown category. Fall back to OTHER so instrumentId stays
                    assignedSound = new AssignedSound(VanillaSoundCategory.OTHER, soundName);
                }
            } else {
                assignedSound = new AssignedSound(source, soundName);
            }

            setAssignedSound(player, slot, assignedSound);
        }

        // Load row delays
        Map<Integer, Double> savedDelays = plugin.getRecordingsManager().getSavedRowDelays(recordingId);
        for (Map.Entry<Integer, Double> entry : savedDelays.entrySet()) {
            setRowDelay(player, entry.getKey(), entry.getValue());
        }

        openMenu(player, 1);
    }

    // Opens the recording creation menu for a player
    public void openMenu(Player player) {
        openMenu(player, 1);
    }

    // ====================================
    // Opens the recording creation menu for a player at a specific page
    // ====================================
    public void openMenu(Player player, int page) {
        // Store current page for player
        playerPages.put(player.getUniqueId(), page);
        
        // Create a 54-slot inventory (6 rows)
        Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE.replace("%page%", String.valueOf(page)));
        
        // Fill rows 1-5 with gray glass panes and paper
        for (int row = 0; row < 5; row++) {
            fillRow(menu, row, player, page);
        }
        
        // Add bottom row navigation
        addNavigationRow(menu, page);
        
        // Open the inventory for the player
        player.openInventory(menu);
    }
    
    // ====================================
    // Fill a row with gray glass panes and paper at the end
    // ====================================
    private void fillRow(Inventory menu, int row, Player player, int page) {
        int startSlot = row * 9;

        // Slots 0-7 of the row: glass panes keyed by absolute sound slot
        for (int i = 0; i < 8; i++) {
            int physSlot    = startSlot + i;
            int absoluteSlot = toAbsoluteSlot(physSlot, page);
            ItemStack glassPane = createGlassPane(player, absoluteSlot);

            menu.setItem(physSlot, glassPane);
        }

        // Slot 8 of the row: paper, keyed by absolute row number
        int absoluteRow = toAbsoluteRow(row + 1, page);
        ItemStack paper = createPaperItem(absoluteRow, player);

        menu.setItem(startSlot + 8, paper);
    }

    // ====================================
    // Convert a physical inventory slot + current page to an absolute sound slot index.
    // Each page holds 40 sound slots (5 rows × 8 cols).
    // ====================================
    public int toAbsoluteSlot(int physicalSlot, int page) {
        int row = physicalSlot / 9; // 0-4
        int col = physicalSlot % 9; // 0-7
        return (page - 1) * 40 + row * 8 + col;
    }

    // Convert a physical row number (1-5) + current page to an absolute row number.
    public int toAbsoluteRow(int physicalRow, int page) {
        return (page - 1) * 5 + physicalRow;
    }
    
    // Create a gray stained glass pane item
    private ItemStack createGlassPane(Player player, int absoluteSlot) {
        AssignedSound assignedSound = getAssignedSound(player, absoluteSlot);

        Material material = (assignedSound != null) ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (assignedSound != null) {
            meta.setDisplayName("§a§l✓ Slot " + (absoluteSlot + 1));

            List<String> lore = new ArrayList<>();
            lore.add("§7Instrument: §e" + assignedSound.getSourceDisplayName());
            lore.add("§7Sound: §e" + assignedSound.getSoundName());
            lore.add("");
            lore.add("§7Left-click to change sound");
            lore.add("§cRight-click to remove");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§7");
        }

        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create a paper item for a row
    // ====================================
    private ItemStack createPaperItem(int absoluteRow, Player player) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        Double delay = getRowDelay(player, absoluteRow);
        boolean hasDelay = delay != null;

        meta.setDisplayName("§e§lRow " + absoluteRow);

        List<String> lore = new ArrayList<>();
        lore.add("§7Recording slot " + absoluteRow);
        lore.add("");

        if (hasDelay) {
            lore.add("§a✓ Delay: §e" + delay + "§7 seconds");
            lore.add("§7Click to change delay");
        } else {
            lore.add("§7Delay: §e0.1§7 seconds§8 (default)");
            lore.add("§7Click to set custom delay");
        }

        meta.setLore(lore);

        if (hasDelay) {
            Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));

            if (unbreaking != null) {
                meta.addEnchant(unbreaking, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
        }

        item.setItemMeta(meta);

        return item;
    }

    // ====================================
    // Add navigation controls to the bottom row
    // ====================================
    private void addNavigationRow(Inventory menu, int currentPage) {
        // Previous page button (slot 45)
        if (currentPage > 1) {
            ItemStack prevButton = createNavigationButton(Material.ARROW, "§c§lPrevious Page", currentPage - 1);
            menu.setItem(PREV_PAGE_SLOT, prevButton);
        }
        
        // Preview button (slot 46)
        ItemStack previewButton = createActionButton(Material.ENDER_EYE, "§b§lPreview", "§7Preview the recording");
        menu.setItem(PREVIEW_SLOT, previewButton);
        
        // Save button (slot 47)
        ItemStack saveButton = createActionButton(Material.EMERALD, "§a§lSave", "§7Save this recording");
        menu.setItem(SAVE_SLOT, saveButton);
        
        // Page info (slot 49)
        ItemStack pageInfo = createPageInfoItem(currentPage);
        menu.setItem(PAGE_INFO_SLOT, pageInfo);
        
        // Cancel button (slot 50)
        ItemStack cancelButton = createActionButton(Material.BARRIER, "§c§lCancel", "§7Close without saving");
        menu.setItem(CANCEL_SLOT, cancelButton);
        
        // Next page button (slot 53)
        ItemStack nextButton = createNavigationButton(Material.ARROW, "§a§lNext Page", currentPage + 1);
        menu.setItem(NEXT_PAGE_SLOT, nextButton);
    }
    
    // ====================================
    // Create a navigation button
    // ====================================
    private ItemStack createNavigationButton(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Go to page " + targetPage);
        meta.setLore(lore);
        
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the page info display item
    // ====================================
    private ItemStack createPageInfoItem(int page) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§6§lPage " + page);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Current page: §e" + page);
        meta.setLore(lore);
        
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create an action button (Preview, Save, Cancel)
    // ====================================
    private ItemStack createActionButton(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        lore.add(description);
        meta.setLore(lore);
        
        item.setItemMeta(meta);

        return item;
    }
    
    // Get the current page for a player
    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 1);
    }
    
    // Handle clicking the previous page button
    public void handlePreviousPage(Player player) {
        int currentPage = getCurrentPage(player);

        if (currentPage > 1) {
            openMenu(player, currentPage - 1);
        }
    }
    
    // Handle clicking the next page button
    public void handleNextPage(Player player) {
        int currentPage = getCurrentPage(player);

        openMenu(player, currentPage + 1);
    }
    
    // Set the delay for a specific row
    public void setRowDelay(Player player, int rowNumber, double delay) {
        UUID playerId = player.getUniqueId();

        playerRowDelays.putIfAbsent(playerId, new HashMap<>());
        playerRowDelays.get(playerId).put(rowNumber, delay);
    }
    
    // Get the delay for a specific row (returns null if not set)
    public Double getRowDelay(Player player, int rowNumber) {
        UUID playerId = player.getUniqueId();
        Map<Integer, Double> rowDelays = playerRowDelays.get(playerId);

        if (rowDelays == null) {
            return null;
        }

        return rowDelays.get(rowNumber);
    }
    
    // Get the row number from a slot (returns 1-5, or -1 if not a paper slot)
    public static int getRowNumberFromSlot(int slot) {

        // Paper slots are 8, 17, 26, 35, 44 (last slot of each row)
        if (slot == 8) return 1;
        if (slot == 17) return 2;
        if (slot == 26) return 3;
        if (slot == 35) return 4;
        if (slot == 44) return 5;

        return -1;
    }
    
    // Check if a slot is a paper slot
    public static boolean isPaperSlot(int slot) {
        return getRowNumberFromSlot(slot) != -1;
    }
    
    // Remove the assigned sound from a specific slot
    public void removeAssignedSound(Player player, int slot) {
        UUID playerId = player.getUniqueId();
        Map<Integer, AssignedSound> slotSounds = playerSlotSounds.get(playerId);

        if (slotSounds != null) {
            slotSounds.remove(slot);
        }
    }

    // Set an assigned sound for a specific slot
    public void setAssignedSound(Player player, int slot, AssignedSound sound) {
        UUID playerId = player.getUniqueId();

        playerSlotSounds.putIfAbsent(playerId, new HashMap<>());
        playerSlotSounds.get(playerId).put(slot, sound);
    }
    
    // Get the assigned sound for a specific slot (returns null if not set)
    public AssignedSound getAssignedSound(Player player, int slot) {
        UUID playerId = player.getUniqueId();
        Map<Integer, AssignedSound> slotSounds = playerSlotSounds.get(playerId);

        if (slotSounds == null) {
            return null;
        }

        return slotSounds.get(slot);
    }
    
    // Get all assigned sounds for a player (returns empty map if none)
    public Map<Integer, AssignedSound> getAllAssignedSounds(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Integer, AssignedSound> slotSounds = playerSlotSounds.get(playerId);

        if (slotSounds == null) {
            return new HashMap<>();
        }

        return new HashMap<>(slotSounds); // Return a copy
    }
    
    // ====================================
    // Preview the recording by playing all assigned sounds with delays
    // ====================================
    public void previewRecording(Player player) {
        Map<Integer, AssignedSound> assignedSounds = getAllAssignedSounds(player);

        if (assignedSounds.isEmpty()) {
            player.sendMessage("§cNo sounds have been added yet!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            return;
        }

        player.sendMessage("§a§lPreviewing Recording...");

        final double DEFAULT_DELAY_SECONDS = 0.1;
        long totalDelay = 0;

        // Sort absolute slots so playback is in order
        List<Integer> sortedSlots = new ArrayList<>(assignedSounds.keySet());
        java.util.Collections.sort(sortedSlots);

        int currentAbsRow = -1;
        double currentDelaySeconds = DEFAULT_DELAY_SECONDS;
        long currentDelayTicks = (long) (DEFAULT_DELAY_SECONDS * 20);

        for (int absSlot : sortedSlots) {
            // Determine which absolute row this slot belongs to (each row = 8 slots)
            int absRow = absSlot / 8 + 1;

            if (absRow != currentAbsRow) {
                currentAbsRow = absRow;
                Double rowDelay = getRowDelay(player, absRow);

                currentDelaySeconds = (rowDelay != null) ? rowDelay : DEFAULT_DELAY_SECONDS;
                currentDelayTicks = (long) (currentDelaySeconds * 20);
            }

            AssignedSound assignedSound = assignedSounds.get(absSlot);
            final int fSlot = absSlot;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playSoundForSlot(player, assignedSound, fSlot);
            }, totalDelay);

            totalDelay += currentDelayTicks;
        }

        if (totalDelay > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§a§lPreview Complete!");
            }, totalDelay + 10);
        }
    }
    
    // ====================================
    // Play a single sound for preview
    // ====================================
    private void playSoundForSlot(Player player, AssignedSound assignedSound, int slot) {
        try {
            String soundName = assignedSound.getSoundName();
            float masterVolume = (float) plugin.getRecordingsPluginConfig().getDouble("playback.volume", 1.0);
            float masterPitch  = (float) plugin.getRecordingsPluginConfig().getDouble("playback.pitch",  1.0);
            float volume = masterVolume;
            float pitch  = masterPitch;

            // Multiply by custom instrument volume/pitch if applicable
            if (!assignedSound.isVanilla()) {
                String instrumentId = assignedSound.getInstrumentId();

                volume = (float) plugin.getManager().getVolume(instrumentId) * masterVolume;
                pitch  = (float) plugin.getManager().getPitch(instrumentId)  * masterPitch;
            }

            // Play the sound on the world so all nearby players can hear it
            player.getWorld().playSound(player.getLocation(), soundName, SoundCategory.RECORDS, volume, pitch);

        } catch (Exception e) {
            player.sendMessage("§cCould not play sound at slot " + (slot + 1) + ": " + assignedSound.getSoundName());
            plugin.getLogger().warning("Error playing sound: " + e.getMessage());
        }
    }
    
    // Clear the editing ID for a player (e.g. on cancel)
    public void clearEditingId(Player player) {
        playerEditingId.remove(player.getUniqueId());
    }

    // Clear ALL persistent data for a player (used when starting a fresh recording)
    public void clearPlayerData(Player player) {
        UUID playerId = player.getUniqueId();

        playerSlotSounds.remove(playerId);
        playerRowDelays.remove(playerId);
        playerPages.remove(playerId);
        playerEditingId.remove(playerId);
    }

    // Get the recording ID being edited for a player, or null if new
    public String getEditingId(Player player) { return playerEditingId.get(player.getUniqueId()); }

    // Play a recording loaded from file by its ID
    public void playRecordingById(org.bukkit.entity.Player player, String recordingId) {
        java.util.Map<Integer, java.util.Map<String, String>> slots = plugin.getRecordingsManager().getSavedSlots(recordingId);
        java.util.Map<Integer, Double> rowDelays = plugin.getRecordingsManager().getSavedRowDelays(recordingId);

        if (slots.isEmpty()) {
            player.sendMessage("§cThis recording has no sounds!");

            return;
        }

        final double DEFAULT_DELAY = 0.1;
        long totalDelay = 0;

        // Sort absolute slots so playback is in correct order
        List<Integer> sortedSlots = new ArrayList<>(slots.keySet());
        java.util.Collections.sort(sortedSlots);

        int currentAbsRow = -1;
        long currentDelayTicks = (long) (DEFAULT_DELAY * 20);

        for (int absSlot : sortedSlots) {
            int absRow = absSlot / 8 + 1;

            if (absRow != currentAbsRow) {
                currentAbsRow = absRow;

                double d = rowDelays.getOrDefault(absRow, DEFAULT_DELAY);

                currentDelayTicks = (long) (d * 20);
            }

            java.util.Map<String, String> data = slots.get(absSlot);
            String soundName = data.get("sound");
            String type      = data.get("type");
            String source    = data.get("source");

            if (soundName == null || soundName.isEmpty())
            {
                continue;
            }

            final String fSound = soundName, fType = type, fSource = source;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    float masterVolume = (float) plugin.getRecordingsPluginConfig().getDouble("playback.volume", 1.0);
                    float masterPitch  = (float) plugin.getRecordingsPluginConfig().getDouble("playback.pitch",  1.0);
                    float volume = masterVolume;
                    float pitch  = masterPitch;

                    if ("custom".equals(fType) && fSource != null && !fSource.isEmpty()) {
                        volume = (float) plugin.getManager().getVolume(fSource) * masterVolume;
                        pitch  = (float) plugin.getManager().getPitch(fSource)  * masterPitch;
                    }

                    player.getWorld().playSound(player.getLocation(), fSound, SoundCategory.RECORDS, volume, pitch);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error playing sound from copy: " + e.getMessage());
                }
            }, totalDelay);

            totalDelay += currentDelayTicks;
        }
    }

    // ====================================
    // Save the recording with full metadata
    // ====================================
    public void saveRecording(Player player, String name, String author, String description) {
        Map<Integer, AssignedSound> assignedSounds = getAllAssignedSounds(player);
        
        if (assignedSounds.isEmpty()) {
            player.sendMessage("§cCannot save an empty recording!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            return;
        }
        
        // Convert assigned sounds to a saveable format
        Map<Integer, Map<String, Object>> slotsData = new HashMap<>();
        for (Map.Entry<Integer, AssignedSound> entry : assignedSounds.entrySet()) {
            int slot = entry.getKey();
            AssignedSound sound = entry.getValue();
            
            Map<String, Object> soundData = new HashMap<>();
            
            soundData.put("sound", sound.getSoundName());
            soundData.put("type", sound.isVanilla() ? "vanilla" : "custom");
            soundData.put("source", sound.isVanilla() ? sound.getCategory().name() : sound.getInstrumentId());
            
            slotsData.put(slot, soundData);
        }
        
        // Get row delays (all absolute rows, not just page 1)
        Map<Integer, Double> rowDelaysRaw = playerRowDelays.get(player.getUniqueId());
        Map<Integer, Double> rowDelays = rowDelaysRaw != null ? new HashMap<>(rowDelaysRaw) : new HashMap<>();
        
        // Save to file with full metadata and creator UUID,
        // overwriting the old recording if we were editing one
        String editingId = playerEditingId.remove(player.getUniqueId());
        if (editingId != null) {
            plugin.getRecordingsManager().deleteRecording(editingId);
        }

        String id = plugin.getRecordingsManager().saveRecording(name, author, description, player.getUniqueId(), slotsData, rowDelays);
        
        // Send success message
        player.sendMessage("§a§lRecording Saved!");
        player.sendMessage("§7Name: §e" + name);
        player.sendMessage("§7Author: §e" + author);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        
        // Close menu
        player.closeInventory();
    }
}
