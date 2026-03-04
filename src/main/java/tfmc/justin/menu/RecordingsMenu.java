package tfmc.justin.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.RecordingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// ====================================
// Manages the recordings menu GUI
// A 54-slot inventory for players to view/manage recordings
// ====================================
public class RecordingsMenu {
    
    private final InstrumentPlugin plugin;
    private final RecordingsManager recordingsManager;
    
    public static final String MENU_TITLE = "Recordings List";
    public static final int CREATE_BUTTON_SLOT = 49; // Bottom row, center

    private final Map<UUID, Map<Integer, String>> playerSlotMap = new HashMap<>();

    public RecordingsMenu(InstrumentPlugin plugin, RecordingsManager recordingsManager) {
        this.plugin = plugin;
        this.recordingsManager = recordingsManager;
    }
    
    // ====================================
    // Opens the recordings menu for a player
    // ====================================
    public void openMenu(Player player) {
        // Create a 54-slot inventory (6 rows)
        Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE);

        // Reset slot map for this player
        Map<Integer, String> slotMap = new HashMap<>();
        playerSlotMap.put(player.getUniqueId(), slotMap);

        // Load and display recordings visible to this player
        Set<String> recordingIds = recordingsManager.getAccessibleRecordingIds(player.getUniqueId());
        int slot = 0;

        for (String id : recordingIds) {
            if (slot >= 45) break; // Leave bottom row for buttons

            ItemStack recordingItem = createRecordingItem(id, player);
            menu.setItem(slot, recordingItem);
            slotMap.put(slot, id);

            slot++;
        }
        
        // Add the "Create New Recording" button at slot 49
        ItemStack createButton = createNewRecordingButton();
        menu.setItem(CREATE_BUTTON_SLOT, createButton);
        
        // Open the inventory for the player
        player.openInventory(menu);
    }
    
    // ====================================
    // Create an item representing a recording
    // ====================================
    private ItemStack createRecordingItem(String id, Player player) {
        String creator = recordingsManager.getRecordingCreator(id);
        boolean isOwner = !creator.isEmpty() && creator.equals(player.getUniqueId().toString());
        
        Material material = isOwner ? Material.GREEN_CONCRETE : Material.GRAY_CONCRETE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = recordingsManager.getRecordingName(id);
        String author = recordingsManager.getRecordingAuthor(id);
        String description = recordingsManager.getRecordingDescription(id);
        
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Author: §e" + author);
        if (description != null && !description.isEmpty()) {
            lore.add("§7" + description);
        }

        lore.add("");

        if (isOwner) {
            lore.add("§7Click to §amanage §7recording");
        } else {
            lore.add("§7Click to §6inspect §7recording");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    // ====================================
    // Get the recording ID for a clicked slot (null if none)
    // ====================================
    public String getRecordingIdFromSlot(Player player, int slot) {
        Map<Integer, String> slotMap = playerSlotMap.get(player.getUniqueId());

        if (slotMap == null)
        {
            return null;
        }

        return slotMap.get(slot);
    }

    // ====================================
    // Check whether the recording in a slot is owned by the player
    // ====================================
    public boolean isOwnerSlot(Player player, int slot) {
        String id = getRecordingIdFromSlot(player, slot);

        if (id == null)
        {
            return false;
        }

        String creator = plugin.getRecordingsManager().getRecordingCreator(id);

        return !creator.isEmpty() && creator.equals(player.getUniqueId().toString());
    }

    // ====================================
    // Create the "Create New Recording" button
    // ====================================
    private ItemStack createNewRecordingButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§a§lCreate New Recording");
        
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Click to create a new recording");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
}
