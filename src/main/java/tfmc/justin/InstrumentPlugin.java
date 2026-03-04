package tfmc.justin;

import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.TLibs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import tfmc.justin.commands.InstrumentCommand;
import tfmc.justin.listeners.DelayInputListener;
import tfmc.justin.listeners.InstrumentListener;
import tfmc.justin.listeners.MenuListener;
import tfmc.justin.listeners.MetadataInputListener;
import tfmc.justin.listeners.RecordingCopyListener;
import tfmc.justin.managers.InstrumentManager;
import tfmc.justin.managers.RecordingsManager;
import tfmc.justin.menu.InstrumentSelectionMenu;
import tfmc.justin.menu.RecordingCreationMenu;
import tfmc.justin.menu.RecordingMetadataMenu;
import tfmc.justin.menu.RecordingOptionsMenu;
import tfmc.justin.menu.RecordingsMenu;
import tfmc.justin.menu.SoundSelectionMenu;

// ====================================
// Main plugin class for MusicalInstruments.
// ====================================
public class InstrumentPlugin extends JavaPlugin {
    
    private static InstrumentPlugin instance;
    private ItemAPI api;
    private InstrumentManager manager;
    private RecordingsManager recordingsManager;
    private RecordingsMenu recordingsMenu;
    private RecordingCreationMenu recordingCreationMenu;
    private InstrumentSelectionMenu instrumentSelectionMenu;
    private SoundSelectionMenu soundSelectionMenu;
    private DelayInputListener delayInputListener;
    private RecordingMetadataMenu recordingMetadataMenu;
    private MetadataInputListener metadataInputListener;
    private RecordingOptionsMenu recordingOptionsMenu;
    private RecordingCopyListener recordingCopyListener;
    private FileConfiguration recordingsPluginConfig;
    
    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("MusicalInstruments is enabled!");
        
        saveDefaultConfig();

        // Load recordingsConfig.yml
        saveResource("recordingsConfig.yml", false);
        recordingsPluginConfig = YamlConfiguration.loadConfiguration( new java.io.File(getDataFolder(), "recordingsConfig.yml"));
        
        api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
        
        manager = new InstrumentManager(this, api);
        recordingsManager = new RecordingsManager(this);
        recordingsMenu = new RecordingsMenu(this, recordingsManager);
        recordingCreationMenu = new RecordingCreationMenu(this);
        instrumentSelectionMenu = new InstrumentSelectionMenu(this, manager);
        soundSelectionMenu = new SoundSelectionMenu(this, manager);
        delayInputListener = new DelayInputListener(this, recordingCreationMenu);
        recordingMetadataMenu = new RecordingMetadataMenu(this);
        metadataInputListener = new MetadataInputListener(this, recordingMetadataMenu);
        recordingOptionsMenu = new RecordingOptionsMenu(this);
        recordingCopyListener = new RecordingCopyListener(this);

        InstrumentCommand commandHandler = new InstrumentCommand(this, manager);
        getCommand("instruments").setExecutor(commandHandler);
        getCommand("instruments").setTabCompleter(commandHandler);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new InstrumentListener(this, manager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(
            this,
            recordingsManager,
            recordingsMenu,
            recordingCreationMenu,
            instrumentSelectionMenu,
            soundSelectionMenu,
            delayInputListener,
            recordingMetadataMenu,
            metadataInputListener,
            recordingOptionsMenu), this);
        getServer().getPluginManager().registerEvents(delayInputListener, this);
        getServer().getPluginManager().registerEvents(metadataInputListener, this);
        getServer().getPluginManager().registerEvents(recordingCopyListener, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MusicalInstruments is disabled!");
    }
    
    public static InstrumentPlugin getInstance() { return instance; }
    public ItemAPI getApi() { return api; }
    public InstrumentManager getManager() { return manager; }
    public RecordingsManager getRecordingsManager() { return recordingsManager; }
    public RecordingsMenu getRecordingsMenu() { return recordingsMenu; }
    public RecordingCreationMenu getRecordingCreationMenu() { return recordingCreationMenu; }
    public InstrumentSelectionMenu getInstrumentSelectionMenu() { return instrumentSelectionMenu; }
    public SoundSelectionMenu getSoundSelectionMenu() { return soundSelectionMenu; }
    public RecordingMetadataMenu getRecordingMetadataMenu() { return recordingMetadataMenu; }
    public MetadataInputListener getMetadataInputListener() { return metadataInputListener; }
    public RecordingOptionsMenu getRecordingOptionsMenu() { return recordingOptionsMenu; }
    public RecordingCopyListener getRecordingCopyListener() { return recordingCopyListener; }
    public FileConfiguration getRecordingsPluginConfig() { return recordingsPluginConfig; }
}
