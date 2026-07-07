package tfmc.justin.managers;

import me.Plugins.TLibs.Objects.API.ItemAPI;
import org.bukkit.inventory.ItemStack;
import tfmc.justin.InstrumentPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// ====================================
// Manages instrument detection and validation.
// Identifies instruments with TLibs.
// ====================================
public class InstrumentManager {

    private final InstrumentPlugin plugin;
    private final ItemAPI api;
    private final Map<String, ItemStack> templates;

    public InstrumentManager(InstrumentPlugin plugin, ItemAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.templates = new LinkedHashMap<>();
    }

    // ====================================
    // Resolves every configured instrument item once and caches the result,
    // so hotbar events compare against cached templates instead of calling
    // TLibs for each instrument on every slot change.
    // Called on enable and on /instruments reload.
    // ====================================
    public void loadTemplates() {
        templates.clear();

        for (String instrument : plugin.getConfig().getKeys(false)) {
            String configPath = plugin.getConfig().getString(instrument + ".item");
            if (configPath == null) {
                plugin.getLogger().warning("Instrument '" + instrument + "' has no 'item' defined in config.");
                continue;
            }

            try {
                ItemStack template = api.getCreator().getItemFromPath(configPath);
                if (template == null) {
                    plugin.getLogger().warning("Could not resolve item '" + configPath + "' for instrument '" + instrument + "'.");
                    continue;
                }

                templates.put(instrument, template);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load instrument '" + instrument + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + templates.size() + " instrument(s).");
    }

    // ====================================
    // Gets the instrument ID from an ItemStack.
    // ====================================
    public String getInstrument(ItemStack item) {
        if (item == null) {
            return null;
        }

        for (Map.Entry<String, ItemStack> entry : templates.entrySet()) {
            if (item.isSimilar(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    // Gets the sound key for an instrument slot and sneak state.
    public String getSoundKey(String instrument, int slot, boolean sneaking)
    {
        String commandKey = sneaking ? slot + "+sneak" : String.valueOf(slot);

        return plugin.getConfig().getString(instrument + ".hotbar-sounds." + commandKey);
    }

    public String getKeybindMessage(String instrument){ return plugin.getConfig().getString(instrument + ".keybind-message"); }
    public double getVolume(String instrument) { return plugin.getConfig().getDouble(instrument + ".hotbar-sounds.volume", 1.0);}
    public double getPitch(String instrument){ return plugin.getConfig().getDouble(instrument + ".hotbar-sounds.pitch", 1.0); }
    public Set<String> getAllInstruments() { return Collections.unmodifiableSet(templates.keySet()); }

    // Gets a copy of an instrument's cached item template.
    public ItemStack getInstrumentItem(String instrument) {
        ItemStack template = templates.get(instrument);

        return template == null ? null : template.clone();
    }
}
