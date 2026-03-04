package tfmc.justin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitRunnable;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.InstrumentManager;

import java.util.HashMap;
import java.util.Map;

// ====================================
// Handles instrument-related events.
// Responsible for playing sounds when players change hotbar slots while holding an instrument.
// ====================================
public class InstrumentListener implements Listener {
    
    private final InstrumentPlugin plugin;
    private final InstrumentManager manager;
    private final Map<Player, Integer> instrumentTasks;
    
    public InstrumentListener(InstrumentPlugin plugin, InstrumentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.instrumentTasks = new HashMap<>();
    }
    
    @EventHandler
    public void onPlayerHotbarChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        String instrument = manager.getInstrument(player.getInventory().getItemInOffHand());
        
        if (instrument == null) {
            return;
        }
        
        startInstrumentDisplay(player, instrument);
        
        // Get hotbar slot (1-9)
        int newSlot = event.getNewSlot() + 1;
        
        // Get sound for this slot and sneak(shifting) state
        String soundKey = manager.getSoundKey(instrument, newSlot, player.isSneaking());
        
        if (soundKey == null) {
            return;
        }
        
        // Get volume and pitch
        double volume = manager.getVolume(instrument);
        double pitch = manager.getPitch(instrument);
        
        // Play sound at player's location
        player.getWorld().playSound(
            player.getLocation(), 
            soundKey, 
            SoundCategory.RECORDS, 
            (float) volume, 
            (float) pitch
        );
        
        // Spawn particle effect
        player.getWorld().spawnParticle(
            Particle.NOTE, 
            player.getLocation().add(0.0, 2.0, 0.0), 
            1, 
            0.0, 
            0.0, 
            0.0, 
            1.0
        );
        
        // Switch back to 9th hotbar slot after playing (so we can use the same note multiple times)
        player.getInventory().setHeldItemSlot(8);
    }
    
    // ====================================
    // Check which instrument is being held by the player.
    // ====================================
    private void startInstrumentDisplay(Player player, String instrument) {
        if (instrumentTasks.containsKey(player)) {
            return;
        }
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                String currentInstrument = manager.getInstrument(player.getInventory().getItemInOffHand());
                if (!instrument.equals(currentInstrument)) {
                    stopInstrumentDisplay(player);
                }
            }
        };
        
        int taskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
        instrumentTasks.put(player, taskId);
    }
    
    // ====================================
    // Stops monitoring if the player is holding an instrument.
    // ====================================
    private void stopInstrumentDisplay(Player player)
    {
        Integer taskId = instrumentTasks.remove(player);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
