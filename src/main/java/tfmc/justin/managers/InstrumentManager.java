package tfmc.justin.managers;

import me.Plugins.TLibs.Objects.API.ItemAPI;
import org.bukkit.inventory.ItemStack;
import tfmc.justin.InstrumentPlugin;

import java.util.Set;

// ====================================
// Manages instrument detection and validation.
// Identifies instruments with TLibs.
// ====================================
public class InstrumentManager {
    
    private final InstrumentPlugin plugin;
    private final ItemAPI api;
    
    public InstrumentManager(InstrumentPlugin plugin, ItemAPI api) {
        this.plugin = plugin;
        this.api = api;
    }
    
    // ====================================
    // Gets the instrument ID from an ItemStack.
    // ====================================
    public String getInstrument(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        Set<String> instruments = plugin.getConfig().getKeys(false);
        
        for (String instrument : instruments) {
            String configPath = plugin.getConfig().getString(instrument + ".item");
            if (configPath == null) {
                continue;
            }
            
            try {
                // Get the item from TLibs using the config
                ItemStack template = api.getCreator().getItemFromPath(configPath);
                if (template == null) {
                    continue;
                }
                
                // If the item matches this instrument's template, return the instrument ID
                if (item.isSimilar(template)) {
                    return instrument;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to validate instrument '" + instrument + "': " + e.getMessage());
            }
        }
        
        return null;
    }
    
    // Gets the keybind message for an instrument.
    public String getSoundKey(String instrument, int slot, boolean sneaking)
    {
        String commandKey = sneaking ? slot + "+sneak" : String.valueOf(slot);

        return plugin.getConfig().getString(instrument + ".hotbar-sounds." + commandKey);
    }

    public String getKeybindMessage(String instrument){ return plugin.getConfig().getString(instrument + ".keybind-message"); }
    public double getVolume(String instrument) { return plugin.getConfig().getDouble(instrument + ".hotbar-sounds.volume", 1.0);}
    public double getPitch(String instrument){ return plugin.getConfig().getDouble(instrument + ".hotbar-sounds.pitch", 1.0); }
    public Set<String> getAllInstruments() { return plugin.getConfig().getKeys(false); }

    // Gets an instrument item from TLibs using the config path.
    public ItemStack getInstrumentItem(String instrument) {
        String configPath = plugin.getConfig().getString(instrument + ".item");
        if (configPath == null) {
            return null;
        }
        
        try {
            return api.getCreator().getItemFromPath(configPath);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get instrument item '" + instrument + "': " + e.getMessage());
            
            return null;
        }
    }
}
