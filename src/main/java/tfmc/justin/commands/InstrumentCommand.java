package tfmc.justin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.InstrumentManager;
import tfmc.justin.menu.RecordingsMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ====================================
// Handles the /instruments command.
// Shows keybinds for the instrument held in the player's off-hand.
// ====================================
public class InstrumentCommand implements CommandExecutor, TabCompleter {
    
    private final InstrumentPlugin plugin;
    private final InstrumentManager manager;
    
    public InstrumentCommand(InstrumentPlugin plugin, InstrumentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the correct subcommand was provided
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /instruments <keybinds|recordings>");
            return true;
        }
        
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Handle different subcommands
        switch (args[0].toLowerCase()) {
            case "keybinds":
                handleKeybinds(player);
                break;
            case "recordings":
                handleRecordings(player, args);
                break;
            default:
                sender.sendMessage("§cUsage: /instruments <keybinds|recordings>");
                break;
        }
        
        return true;
    }
    
    // ====================================
    // Handles the /instruments keybinds command
    // ====================================
    private void handleKeybinds(Player player) {
        // Check what instrument is in the players off-hand
        String instrument = manager.getInstrument(player.getInventory().getItemInOffHand());
        
        // If no instrument is found, inform the player (if command is used without an instrument in hand)
        if (instrument == null) {
            player.sendMessage("§cYou must be holding an instrument in your off-hand!");
            return;
        }

        // Get the keybind message from config for this specific instrument
        String keybindMessage = manager.getKeybindMessage(instrument);
        if (keybindMessage != null)
        {
            // Split message into new lines and send each one
            String[] lines = keybindMessage.trim().split("\n");

            for (String line : lines)
            {
                player.sendMessage(line);
            }

        } else {
            // Fallback
            player.sendMessage("§aYour instrument keybinds were not defined in the config.");
        }
    }
    
    // ====================================
    // Handles the /instruments recordings command
    // Opens the recordings menu GUI, or grants access via notation
    // ====================================
    private void handleRecordings(Player player, String[] args) {

        // /instruments recordings access <recording_id>
        if (args.length >= 3 && args[1].equalsIgnoreCase("access")) {
            String permission = plugin.getRecordingsPluginConfig().getString("permissions.recordings-access", "musicalinstruments.recordings.access");
            
            if (!player.hasPermission(permission)) {
                player.sendMessage("§cYou do not have permission to use this command.");

                return;
            }

            String recordingId = args[2];

            if (!plugin.getRecordingsManager().getAllRecordingIds().contains(recordingId)) {
                player.sendMessage("§cRecording '" + recordingId + "' does not exist.");

                return;
            }

            org.bukkit.inventory.ItemStack scroll = plugin.getRecordingCopyListener().createShareScroll(recordingId);

            player.getInventory().addItem(scroll);
            player.sendMessage("§6§lBard's Notation §7for §e" + plugin.getRecordingsManager().getRecordingName(recordingId) + "§7 added to your inventory.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);

            return;
        }

        // /instruments recordings -> open menu
        plugin.getRecordingsMenu().openMenu(player);
    }
    
    // ====================================
    // Provides tab completion for the command
    // When typing /instruments
    // ====================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("keybinds");
            completions.add("recordings");

            return completions;
        }

        // /instruments recordings <access>. Only show "access" to players with permissions.
        if (args.length == 2 && args[0].equalsIgnoreCase("recordings")) {

            if (sender instanceof Player) {
                String permission = plugin.getRecordingsPluginConfig().getString("permissions.recordings-access", "musicalinstruments.recordings.access");
                
                if (sender.hasPermission(permission)) {
                    return List.of("access");
                }
            }

            return Collections.emptyList();
        }

        // /instruments recordings access <recording_id>
        if (args.length == 3 && args[0].equalsIgnoreCase("recordings") && args[1].equalsIgnoreCase("access") && sender instanceof Player) {
            String permission = plugin.getRecordingsPluginConfig().getString("permissions.recordings-access", "musicalinstruments.recordings.access");
            
            if (!sender.hasPermission(permission))
            {
                return Collections.emptyList();
            }

            List<String> ids = new ArrayList<>(plugin.getRecordingsManager().getAllRecordingIds());
            String partial = args[2].toLowerCase();

            ids.removeIf(id -> !id.toLowerCase().startsWith(partial));
            
            return ids;
        }

        return Collections.emptyList();
    }
}
