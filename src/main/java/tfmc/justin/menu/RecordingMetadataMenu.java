package tfmc.justin.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
// Manages the recording metadata input menu
// Prompts for name, author, and description before saving
// ====================================
public class RecordingMetadataMenu {
    
    private final InstrumentPlugin plugin;
    
    public static final String MENU_TITLE = "Recording Info";
    public static final int NAME_SLOT = 11;
    public static final int AUTHOR_SLOT = 13;
    public static final int DESCRIPTION_SLOT = 15;
    public static final int SAVE_SLOT = 22;
    public static final int CANCEL_SLOT = 26;
    
    // Track metadata for each player
    private final Map<UUID, String> playerRecordingName = new HashMap<>();
    private final Map<UUID, String> playerRecordingAuthor = new HashMap<>();
    private final Map<UUID, String> playerRecordingDescription = new HashMap<>();
    
    public RecordingMetadataMenu(InstrumentPlugin plugin) {
        this.plugin = plugin;
    }
    
    // ====================================
    // Open the metadata input menu for a player
    // ====================================
    public void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);
        
        // Add name input
        ItemStack nameItem = createMetadataItem(player, "name");
        menu.setItem(NAME_SLOT, nameItem);
        
        // Add author input
        ItemStack authorItem = createMetadataItem(player, "author");
        menu.setItem(AUTHOR_SLOT, authorItem);
        
        // Add description input
        ItemStack descItem = createMetadataItem(player, "description");
        menu.setItem(DESCRIPTION_SLOT, descItem);
        
        // Add save button
        ItemStack saveButton = createSaveButton(player);
        menu.setItem(SAVE_SLOT, saveButton);
        
        // Add cancel button
        ItemStack cancelButton = createCancelButton();
        menu.setItem(CANCEL_SLOT, cancelButton);
        
        player.openInventory(menu);
    }
    
    // ====================================
    // Create a metadata input item
    // ====================================
    private ItemStack createMetadataItem(Player player, String type) {
        Material material;
        String displayName;
        String currentValue;
        boolean required;
        
        UUID playerId = player.getUniqueId();
        
        switch (type) {
            case "name":
                currentValue = playerRecordingName.get(playerId);
                material = (currentValue != null && !currentValue.isEmpty()) ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
                displayName = "§c§lRecording Name";
                required = true;

                break;
            case "author":
                currentValue = playerRecordingAuthor.get(playerId);
                material = (currentValue != null && !currentValue.isEmpty()) ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
                displayName = "§c§lAuthor";
                required = true;

                break;
            case "description":
                currentValue = playerRecordingDescription.get(playerId);
                material = (currentValue != null && !currentValue.isEmpty()) ? Material.GREEN_CONCRETE : Material.ORANGE_CONCRETE;
                displayName = "§6§lDescription";
                required = false;
            
                break;
            default:
                return new ItemStack(Material.BARRIER);
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        if (currentValue != null && !currentValue.isEmpty()) {
            lore.add("§a✓ Set: §7" + currentValue);
        } else {
            if (required) {
                lore.add("§c§l✗ Required");
            } else {
                lore.add("§7Optional");
            }
        }
        lore.add("");
        lore.add("§7Click to set " + type);
        meta.setLore(lore);
        
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the save button
    // ====================================
    private ItemStack createSaveButton(Player player) {
        UUID playerId = player.getUniqueId();
        boolean hasName = playerRecordingName.get(playerId) != null;
        boolean hasAuthor = playerRecordingAuthor.get(playerId) != null;
        boolean canSave = hasName && hasAuthor;
        
        Material material = canSave ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(canSave ? "§a§lSave Recording" : "§7§lSave Recording");
        
        List<String> lore = new ArrayList<>();
        if (canSave) {
            lore.add("§7Click to save the recording");
        } else {
            lore.add("§cMissing required fields:");
            if (!hasName) lore.add("§c - Recording Name");
            if (!hasAuthor) lore.add("§c - Author");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Create the cancel button
    // ====================================
    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§lCancel");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Return to recording editor");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
    
    // ====================================
    // Pre-fill metadata from an existing recording (used when editing)
    // Only sets fields that are not already set by the player
    // ====================================
    public void prefillFromRecording(Player player, String recordingId) {
        UUID playerId = player.getUniqueId();
        tfmc.justin.managers.RecordingsManager rm = plugin.getRecordingsManager();

        if (!playerRecordingName.containsKey(playerId)) {
            String name = rm.getRecordingName(recordingId);

            if (name != null && !name.isEmpty()){
                playerRecordingName.put(playerId, name);
            }
        }
        if (!playerRecordingAuthor.containsKey(playerId)) {
            String author = rm.getRecordingAuthor(recordingId);

            if (author != null && !author.isEmpty()){
                playerRecordingAuthor.put(playerId, author);
            }
        }
        if (!playerRecordingDescription.containsKey(playerId)) {
            String desc = rm.getRecordingDescription(recordingId);

            if (desc != null && !desc.isEmpty())
            {
                playerRecordingDescription.put(playerId, desc);
            }
        }
    }

    // ====================================
    // Set metadata values
    // ====================================
    public void setRecordingName(Player player, String name) { playerRecordingName.put(player.getUniqueId(), name); }
    public void setAuthor(Player player, String author) { playerRecordingAuthor.put(player.getUniqueId(), author); }
    public void setDescription(Player player, String description) { playerRecordingDescription.put(player.getUniqueId(), description); }
    
    // ====================================
    // Get metadata values
    // ====================================
    public String getRecordingName(Player player) { return playerRecordingName.get(player.getUniqueId()); }
    public String getAuthor(Player player) { return playerRecordingAuthor.get(player.getUniqueId()); }
    public String getDescription(Player player) { return playerRecordingDescription.getOrDefault(player.getUniqueId(), ""); }
    
    // ====================================
    // Check if player can save
    // ====================================
    public boolean canSave(Player player) {
        UUID playerId = player.getUniqueId();

        return playerRecordingName.get(playerId) != null && playerRecordingAuthor.get(playerId) != null;
    }
    
    // ====================================
    // Clear player data
    // ====================================
    public void clearPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        playerRecordingName.remove(playerId);
        playerRecordingAuthor.remove(playerId);
        playerRecordingDescription.remove(playerId);
    }
}
