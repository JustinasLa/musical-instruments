package tfmc.justin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.InstrumentManager;

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
            sender.sendMessage("§cUsage: /instruments <keybinds>");
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
            default:
                sender.sendMessage("§cUsage: /instruments <keybinds>");
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
    // Provides tab completion for the command
    // When typing /instruments
    // ====================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return List.of("keybinds");
        }

        return Collections.emptyList();
    }
}
