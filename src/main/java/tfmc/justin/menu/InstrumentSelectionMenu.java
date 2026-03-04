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
// Manages the instrument selection menu GUI for recording creation
// Displays all available instruments
// ====================================
public class InstrumentSelectionMenu {
    
    private final InstrumentPlugin plugin;
    private final InstrumentManager instrumentManager;
    
    public static final String MENU_TITLE = "Instrument Selection";
    public static final int BACK_BUTTON_SLOT = 49; // Bottom center
    
    // Track which slot the player is filling
    private final Map<UUID, Integer> playerTargetSlots = new HashMap<>();
    
    // Track whether a slot contains a vanilla category (vs custom instrument)
    private final Map<UUID, Map<Integer, String>> playerSlotToVanillaCategory = new HashMap<>();
    
    public InstrumentSelectionMenu(InstrumentPlugin plugin, InstrumentManager instrumentManager) {
        this.plugin = plugin;
        this.instrumentManager = instrumentManager;
    }
    
    // ====================================
    // Opens the instrument selection menu for a player
    // ====================================
    public void openMenu(Player player, int targetSlot) {
        // Store the target slot for this player
        playerTargetSlots.put(player.getUniqueId(), targetSlot);
        
        // Initialize tracking maps
        playerSlotToVanillaCategory.putIfAbsent(player.getUniqueId(), new HashMap<>());
        playerSlotToVanillaCategory.get(player.getUniqueId()).clear();
        
        // Get all instruments from config
        Set<String> instruments = instrumentManager.getAllInstruments();
        
        // Always use 54 slots to have space for Back button
        int size = 54;
        
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, size, MENU_TITLE);
        
        // Add custom instruments first (top 2 rows)
        int slot = 0;
        for (String instrumentId : instruments) {
            if (slot >= 18) break; // Leave space for vanilla categories
            
            ItemStack instrumentItem = createInstrumentMenuItem(instrumentId);
            if (instrumentItem != null) {
                menu.setItem(slot, instrumentItem);
                slot++;
            }
        }
        
        // Add visual separator at row 3
        for (int i = 18; i < 27; i++) {
            ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = separator.getItemMeta();

            meta.setDisplayName("§7§m-----------");
            separator.setItemMeta(meta);
            menu.setItem(i, separator);
        }
        
        // Add center label for separator row
        ItemStack label = new ItemStack(Material.PAPER);
        ItemMeta labelMeta = label.getItemMeta();

        labelMeta.setDisplayName("§e§lVanilla Sounds");
        label.setItemMeta(labelMeta);
        menu.setItem(22, label); // Center of row 3
        
        // Add vanilla sound categories (starting row 4)
        slot = 27;
        for (VanillaSoundCategory category : VanillaSoundCategory.values()) {
            if (slot >= 45) break; // Leave bottom row for buttons
            
            ItemStack categoryItem = createVanillaCategoryItem(category);
            menu.setItem(slot, categoryItem);
            
            // Track this slot as containing a vanilla category
            playerSlotToVanillaCategory.get(player.getUniqueId()).put(slot, category.name());
            
            slot++;
        }
        
        // Add the "Back" button at slot 49
        ItemStack backButton = createBackButton();
        menu.setItem(BACK_BUTTON_SLOT, backButton);
        
        // Open the inventory for the player
        player.openInventory(menu);
    }
    
    // ====================================
    // Create a menu item for an instrument
    // Uses TLibs to get the actual instrument item
    // ====================================
    private ItemStack createInstrumentMenuItem(String instrumentId) {
        // Get the instrument item from TLibs
        ItemStack item = instrumentManager.getInstrumentItem(instrumentId);
        
        if (item == null) {
            // Fallback to a generic item if TLibs fails
            item = new ItemStack(Material.MUSIC_DISC_CAT);
        }
        
        // Clone the item to avoid modifying the original
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Format the instrument name nicely
        String displayName = formatInstrumentName(instrumentId);
        meta.setDisplayName("§6§l" + displayName);
        
        // Add lore
        List<String> lore = new ArrayList<>();

        lore.add("§7Click to view sounds");
        lore.add("§7for this instrument");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ====================================
    // Create a menu item for a vanilla sound category
    // ====================================
    private ItemStack createVanillaCategoryItem(VanillaSoundCategory category) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(category.getDisplayName());
        
        // Add lore showing number of sounds
        List<String> lore = new ArrayList<>();
        int soundCount = category.getSounds().size();

        lore.add("§7" + soundCount + " sounds available");
        lore.add("");
        lore.add("§7Click to view sounds");
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
            // Capitalize first letter
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        
        return result.toString();
    }
    
    // ====================================
    // Create the "Back" button
    // ====================================
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§lBack");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Return to recording creation");
        meta.setLore(lore);
        
        item.setItemMeta(meta);

        return item;
    }
    
    // Get the instrument ID from a slot in the menu
    public String getInstrumentFromSlot(int slot) {
        // Instruments are only in slots 0-17 (first 2 rows)
        if (slot < 0 || slot >= 18) {
            return null;
        }
        
        Set<String> instruments = instrumentManager.getAllInstruments();
        List<String> instrumentList = new ArrayList<>(instruments);
        
        if (slot < instrumentList.size()) {
            return instrumentList.get(slot);
        }
        
        return null;
    }
    
    // Check if a slot contains a vanilla sound category
    public boolean isVanillaCategory(Player player, int slot) {
        Map<Integer, String> categoryMap = playerSlotToVanillaCategory.get(player.getUniqueId());

        return categoryMap != null && categoryMap.containsKey(slot);
    }
    
    // Get the vanilla sound category from a slot
    public VanillaSoundCategory getVanillaCategoryFromSlot(Player player, int slot) {
        Map<Integer, String> categoryMap = playerSlotToVanillaCategory.get(player.getUniqueId());

        if (categoryMap == null) {
            return null;
        }
        
        String categoryName = categoryMap.get(slot);
        if (categoryName == null) {
            return null;
        }
        
        return VanillaSoundCategory.fromString(categoryName);
    }
    
    // Get the target slot for a player
    public int getTargetSlot(Player player) { return playerTargetSlots.getOrDefault(player.getUniqueId(), -1); }
    
    // Clean up player data
    public void cleanup(Player player) {
        playerTargetSlots.remove(player.getUniqueId());
        playerSlotToVanillaCategory.remove(player.getUniqueId());
    }
}
