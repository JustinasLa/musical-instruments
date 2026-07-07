package tfmc.justin;

import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.TLibs;
import org.bukkit.plugin.java.JavaPlugin;
import tfmc.justin.commands.InstrumentCommand;
import tfmc.justin.listeners.InstrumentListener;
import tfmc.justin.managers.InstrumentManager;

// ====================================
// Main plugin class for MusicalInstruments.
// ====================================
public class InstrumentPlugin extends JavaPlugin {

    private static InstrumentPlugin instance;
    private ItemAPI api;
    private InstrumentManager manager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("MusicalInstruments is enabled!");

        saveDefaultConfig();

        api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);

        manager = new InstrumentManager(this, api);

        // Resolve instrument templates on the first tick, after every plugin
        // (MMOItems, ItemsAdder) has finished enabling and registered its items.
        getServer().getScheduler().runTask(this, manager::loadTemplates);

        InstrumentCommand commandHandler = new InstrumentCommand(this, manager);
        getCommand("instruments").setExecutor(commandHandler);
        getCommand("instruments").setTabCompleter(commandHandler);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new InstrumentListener(this, manager), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MusicalInstruments is disabled!");
    }

    public static InstrumentPlugin getInstance() { return instance; }
    public ItemAPI getApi() { return api; }
    public InstrumentManager getManager() { return manager; }
}
