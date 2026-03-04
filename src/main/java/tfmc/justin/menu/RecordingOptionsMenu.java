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
// Options menu shown when a player clicks their own recording
// Provides: Delete, Edit, Get a Copy, Share
// ====================================
public class RecordingOptionsMenu {

    private final InstrumentPlugin plugin;

    public static final String MENU_TITLE = "Recording Options";

    public static final int DELETE_SLOT = 10;
    public static final int EDIT_SLOT   = 12;
    public static final int COPY_SLOT   = 14;
    public static final int SHARE_SLOT  = 16;
    public static final int BACK_SLOT   = 22;
    public static final int OTHER_COPY_SLOT = 13; // Center slot used in non-owner view

    // Track which recording each player is currently viewing options for
    private final Map<UUID, String> playerRecordingId = new HashMap<>();

    public RecordingOptionsMenu(InstrumentPlugin plugin) {
        this.plugin = plugin;
    }

    // ====================================
    // Open the options menu for a player and recording
    // ====================================
    public void openMenu(Player player, String recordingId) {
        playerRecordingId.put(player.getUniqueId(), recordingId);

        String recordingName = plugin.getRecordingsManager().getRecordingName(recordingId);

        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        menu.setItem(DELETE_SLOT, createOption(
                Material.RED_CONCRETE,
                "§c§lDelete",
                "§7Permanently delete this recording.",
                "§cThis cannot be undone!"
        ));

        menu.setItem(EDIT_SLOT, createOption(
                Material.ORANGE_CONCRETE,
                "§6§lEdit",
                "§7Re-open the recording editor",
                "§7to make changes."
        ));

        menu.setItem(COPY_SLOT, createOption(
                Material.CYAN_CONCRETE,
                "§b§lGet a Copy",
                "§7Receive a copy of this recording",
                "§7as an item in your inventory."
        ));

        menu.setItem(SHARE_SLOT, createOption(
                Material.LIME_CONCRETE,
                "§a§lShare",
                "§7Share this recording with",
                "§7another character."
        ));

        menu.setItem(BACK_SLOT, createOption(
                Material.BARRIER,
                "§c§lBack",
                "§7Return to the recordings menu."
        ));

        player.openInventory(menu);
    }

    // ====================================
    // Open a limited options menu for recordings the player doesn't own
    // Only shows: Get a Copy + Back
    // ====================================
    public void openMenuForOther(Player player, String recordingId) {
        playerRecordingId.put(player.getUniqueId(), recordingId);

        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        menu.setItem(OTHER_COPY_SLOT, createOption(
                Material.CYAN_CONCRETE,
                "§b§lGet a Copy",
                "§7Receive a copy of this recording",
                "§7as an item in your inventory."
        ));

        menu.setItem(BACK_SLOT, createOption(
                Material.BARRIER,
                "§c§lBack",
                "§7Return to the recordings menu."
        ));

        player.openInventory(menu);
    }

    // Get the recording ID the player currently has options open for
    public String getRecordingId(Player player) { return playerRecordingId.get(player.getUniqueId()); }

    // Clean up player data
    public void cleanup(Player player) {
        playerRecordingId.remove(player.getUniqueId());
    }

    // ====================================
    // Helper to build option items
    // ====================================
    private ItemStack createOption(Material material, String name, String... lorelines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();

        for (String line : lorelines) {
            lore.add(line);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
}
