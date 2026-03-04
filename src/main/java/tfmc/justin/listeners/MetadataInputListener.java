package tfmc.justin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.menu.RecordingMetadataMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ====================================
// Listener that handles chat input for recording metadata
// ====================================
public class MetadataInputListener implements Listener {
    
    private final InstrumentPlugin plugin;
    private final RecordingMetadataMenu metadataMenu;
    
    // Track which players are waiting for metadata input and which field
    private final Map<UUID, String> awaitingMetadataInput = new HashMap<>();
    
    public MetadataInputListener(InstrumentPlugin plugin, RecordingMetadataMenu metadataMenu) {
        this.plugin = plugin;
        this.metadataMenu = metadataMenu;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if this player is waiting for metadata input
        if (!awaitingMetadataInput.containsKey(playerId)) {
            return;
        }
        
        // Cancel the chat event so the message doesn't appear in chat
        event.setCancelled(true);
        
        String message = ChatColor.translateAlternateColorCodes('&', event.getMessage().trim());
        String fieldType = awaitingMetadataInput.get(playerId);
        
        // Check for cancel
        if (message.equalsIgnoreCase("cancel")) {
            awaitingMetadataInput.remove(playerId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§c§lCancelled §7metadata input.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                metadataMenu.openMenu(player);
            });

            return;
        }
        
        // Validate input length
        int maxLength = fieldType.equals("description") ? 200 : 50;

        if (message.length() > maxLength) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cInput is too long! Maximum " + maxLength + " characters.");
                player.sendMessage("§ePlease enter the " + fieldType + " or type §c'cancel' §eto abort.");
            });

            return;
        }
        
        if (message.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cInput cannot be empty!");
                player.sendMessage("§ePlease enter the " + fieldType + " or type §c'cancel' §eto abort.");
            });

            return;
        }
        
        // Store the metadata
        awaitingMetadataInput.remove(playerId);
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (fieldType) {
                case "name":
                    metadataMenu.setRecordingName(player, message);
                    player.sendMessage("§a§lRecording Name Set! §7" + message);

                    break;
                case "author":
                    metadataMenu.setAuthor(player, message);
                    player.sendMessage("§a§lAuthor Set! §7" + message);

                    break;
                case "description":
                    metadataMenu.setDescription(player, message);
                    player.sendMessage("§a§lDescription Set! §7" + message);

                    break;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            metadataMenu.openMenu(player);
        });
    }
    
    // ====================================
    // Request metadata input from a player
    // ====================================
    public void requestMetadataInput(Player player, String fieldType) {
        awaitingMetadataInput.put(player.getUniqueId(), fieldType);
        
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        switch (fieldType) {
            case "name":
                player.sendMessage("§e§lSet Recording Name");
                player.sendMessage("");
                player.sendMessage("§7Please enter the name for your recording.");
                player.sendMessage("§7Maximum 50 characters.");

                break;
            case "author":
                player.sendMessage("§e§lSet Author");
                player.sendMessage("");
                player.sendMessage("§7Please enter the author name.");
                player.sendMessage("§7Maximum 50 characters.");

                break;
            case "description":
                player.sendMessage("§e§lSet Description");
                player.sendMessage("");
                player.sendMessage("§7Please enter a description for your recording.");
                player.sendMessage("§7Maximum 200 characters. §8(Optional)");
                
                break;
        }
        
        player.sendMessage("");
        player.sendMessage("§7Type §c'cancel' §7to abort.");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }
    
    // ====================================
    // Clean up when player disconnects
    // ====================================
    public void cleanup(Player player) {
        awaitingMetadataInput.remove(player.getUniqueId());
    }
}
