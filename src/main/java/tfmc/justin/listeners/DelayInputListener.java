package tfmc.justin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.menu.RecordingCreationMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ====================================
// Listener that handles chat input for setting note delays
// ====================================
public class DelayInputListener implements Listener {
    
    private final InstrumentPlugin plugin;
    private final RecordingCreationMenu recordingCreationMenu;
    
    // Track which players are waiting for delay input and for which row
    private final Map<UUID, Integer> awaitingDelayInput = new HashMap<>();
    
    public DelayInputListener(InstrumentPlugin plugin, RecordingCreationMenu recordingCreationMenu) {
        this.plugin = plugin;
        this.recordingCreationMenu = recordingCreationMenu;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if this player is waiting for delay input
        if (!awaitingDelayInput.containsKey(playerId)) {
            return;
        }
        
        // Cancel the chat event so the message doesn't appear in chat
        event.setCancelled(true);
        
        String message = event.getMessage().trim();
        int rowNumber = awaitingDelayInput.get(playerId);
        
        // Check for cancel
        if (message.equalsIgnoreCase("cancel")) {
            awaitingDelayInput.remove(playerId);

            int cancelPage = (rowNumber - 1) / 5 + 1;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§c§lCancelled §7delay input.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                recordingCreationMenu.openMenu(player, cancelPage);
            });

            return;
        }
        
        // Try to parse the input as a double
        double delay;
        try {
            delay = Double.parseDouble(message);
            
            // Validate the delay value
            if (delay < 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cDelay must be a positive number!");
                    player.sendMessage("§ePlease enter a delay in seconds (e.g., 0.5, 1.0, 2.5) or type §c'cancel' §eto abort.");
                });

                return;
            }
            
            if (delay > 10) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cDelay is too large! Maximum is 10 seconds.");
                    player.sendMessage("§ePlease enter a delay in seconds (e.g., 0.5, 1.0, 2.5) or type §c'cancel' §eto abort.");
                });

                return;
            }
            
        } catch (NumberFormatException e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§c'" + message + "' is not a valid number!");
                player.sendMessage("§ePlease enter a delay in seconds (e.g., 0.5, 1.0, 2.5) or type §c'cancel' §eto abort.");
            });

            return;
        }
        
        // Valid delay received. Store it and reopen the menu
        awaitingDelayInput.remove(playerId);

        int targetPage = (rowNumber - 1) / 5 + 1;
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            recordingCreationMenu.setRowDelay(player, rowNumber, delay);
            player.sendMessage("§a§lDelay Set! §7Row " + rowNumber + " delay set to §e" + delay + " §7seconds.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            recordingCreationMenu.openMenu(player, targetPage);
        });
    }
    
    // ====================================
    // Request delay input from a player for a specific row
    // ====================================
    public void requestDelayInput(Player player, int rowNumber) {
        awaitingDelayInput.put(player.getUniqueId(), rowNumber);
        
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lSet Note Delay for Row " + rowNumber);
        player.sendMessage("");
        player.sendMessage("§7Please enter the delay between notes in §eseconds§7.");
        player.sendMessage("§7Examples: §e0.5§7, §e1.0§7, §e2.5");
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
        awaitingDelayInput.remove(player.getUniqueId());
    }
}
