package tfmc.justin.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tfmc.justin.InstrumentPlugin;

// ====================================
// Handles right-clicking a recording copy item.
// Plays consumes the item and gives a recording copy.
// ====================================
public class RecordingCopyListener implements Listener {

    private final InstrumentPlugin plugin;

    // Key used to tag disc copy items in their PersistentDataContainer
    public static final String RECORDING_ID_KEY  = "recording_copy_id";

    // Key used to tag share scrolls
    public static final String SHARE_SCROLL_KEY  = "recording_share_scroll";

    public RecordingCopyListener(InstrumentPlugin plugin) {
        this.plugin = plugin;
    }

    // ====================================
    // Build the NamespacedKey for the recording ID tag
    // ====================================
    public NamespacedKey getRecordingIdKey() {
        return new NamespacedKey(plugin, RECORDING_ID_KEY);
    }

    public NamespacedKey getShareScrollKey() {
        return new NamespacedKey(plugin, SHARE_SCROLL_KEY);
    }

    // ====================================
    // Create a tagged copy item for a recording
    // ====================================
    public ItemStack createCopyItem(String recordingId) {
        String name        = plugin.getRecordingsManager().getRecordingName(recordingId);
        String author      = plugin.getRecordingsManager().getRecordingAuthor(recordingId);
        String description = plugin.getRecordingsManager().getRecordingDescription(recordingId);

        ItemStack item = new ItemStack(Material.MUSIC_DISC_CAT);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        java.util.List<String> lore = new java.util.ArrayList<>();

        lore.add("§7Author: §e" + author);

        if (description != null && !description.isEmpty()) {
            lore.add("§7" + description);
        }

        lore.add("");
        lore.add("§cOne-time use. Right-click to play");

        meta.setLore(lore);

        // Tag with recording ID
        meta.getPersistentDataContainer().set(getRecordingIdKey(), PersistentDataType.STRING, recordingId);

        item.setItemMeta(meta);
        return item;
    }

    // ====================================
    // Create a shareable scroll for a recording
    // Players right-click it to receive a disc copy
    // ====================================
    public ItemStack createShareScroll(String recordingId) {
        String name        = plugin.getRecordingsManager().getRecordingName(recordingId);
        String author      = plugin.getRecordingsManager().getRecordingAuthor(recordingId);
        String description = plugin.getRecordingsManager().getRecordingDescription(recordingId);

        ItemStack item = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6§lBard's Notation");

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§e\u266a " + name);
        lore.add("§7Composed by: §f" + author);

        if (description != null && !description.isEmpty()) {
            lore.add("§7" + description);
        }

        lore.add("");
        lore.add("§8A faithful transcription of a musical");
        lore.add("§8composition. Right-click to receive");
        lore.add("§8a playable copy.");

        meta.setLore(lore);

        // Tag with recording ID
        meta.getPersistentDataContainer().set(getShareScrollKey(),  PersistentDataType.STRING, recordingId);

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only main hand, only right-click
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // Right click with Share scroll -> give player a recording copy
        if (item.getType() == Material.PAPER) {
            org.bukkit.inventory.meta.ItemMeta scrollMeta = item.getItemMeta();

            if (scrollMeta == null)
            {
                return;
            }

            PersistentDataContainer scrollPdc = scrollMeta.getPersistentDataContainer();

            if (!scrollPdc.has(getShareScrollKey(), PersistentDataType.STRING))
            {
                return;
            }

            event.setCancelled(true);
            Player player = event.getPlayer();
            String recordingId = scrollPdc.get(getShareScrollKey(), PersistentDataType.STRING);

            // Grant access so the recording appears in the players recordings menu
            plugin.getRecordingsManager().addAccessor(recordingId, player.getUniqueId());

            // Consume the scroll
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            // Give a playable disc copy
            ItemStack disc = createCopyItem(recordingId);
            player.getInventory().addItem(disc);

            player.sendMessage("§6§lBard's Notation §7transcribed. Disc added to your inventory!");
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);

            return;
        }

        if (item.getType() != Material.MUSIC_DISC_CAT) return;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(getRecordingIdKey(), PersistentDataType.STRING)) return;

        // Its a recording copy. Cancel default disc behaviour
        event.setCancelled(true);

        String recordingId = pdc.get(getRecordingIdKey(), PersistentDataType.STRING);
        Player player = event.getPlayer();

        // Consume the item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Play the recording
        plugin.getRecordingCreationMenu().playRecordingById(player, recordingId);

        player.sendMessage("§aPlaying: §e" + plugin.getRecordingsManager().getRecordingName(recordingId));
    }
}
