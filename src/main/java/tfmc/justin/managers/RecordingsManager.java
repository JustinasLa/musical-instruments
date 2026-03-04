package tfmc.justin.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tfmc.justin.InstrumentPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

// ====================================
// Manages loading and saving recordings from/to recordings.yml
// ====================================
public class RecordingsManager {
    
    private final InstrumentPlugin plugin;
    private File recordingsFile;
    private FileConfiguration recordingsConfig;
    
    public RecordingsManager(InstrumentPlugin plugin) {
        this.plugin = plugin;
        loadRecordingsConfig();
    }
    
    // ====================================
    // Load or create the recordings.yml file
    // ====================================
    private void loadRecordingsConfig() {
        recordingsFile = new File(plugin.getDataFolder(), "recordings.yml");
        
        // Create the file if it doesn't exist
        if (!recordingsFile.exists()) {
            plugin.saveResource("recordings.yml", false);
        }
        
        recordingsConfig = YamlConfiguration.loadConfiguration(recordingsFile);
    }
    
    // Save the recordings config to disk
    public void saveRecordings() {
        try {
            recordingsConfig.save(recordingsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save recordings.yml!");
            e.printStackTrace();
        }
    }
    
    // Reload the recordings from disk
    public void reloadRecordings() {
        recordingsConfig = YamlConfiguration.loadConfiguration(recordingsFile);
    }
    
    // Get all recording IDs
    public Set<String> getAllRecordingIds() {
        if (recordingsConfig.getConfigurationSection("recordings") == null) {

            return Set.of();
        }

        return recordingsConfig.getConfigurationSection("recordings").getKeys(false);
    }
    
    // Get a recording's display name (checks metadata path first)
    public String getRecordingName(String id) {
        String metaName = recordingsConfig.getString("recordings." + id + ".metadata.name");

        if (metaName != null)
        {
            return metaName;
        }

        return recordingsConfig.getString("recordings." + id + ".name", "§7Unknown");
    }

    public String getRecordingCreator(String id) { return recordingsConfig.getString("recordings." + id + ".metadata.creator", ""); }
    public String getRecordingAuthor(String id) {  return recordingsConfig.getString("recordings." + id + ".metadata.author", "Unknown"); }
    public String getRecordingDescription(String id) { return recordingsConfig.getString("recordings." + id + ".metadata.description", ""); }
    public String getRecordingDate(String id) { return recordingsConfig.getString("recordings." + id + ".created", ""); }
    public String getRecordingInstrument(String id) { return recordingsConfig.getString("recordings." + id + ".instrument", "unknown");}
    
    // Create a new recording with a default name
    public String createNewRecording() {
        return createNewRecording("none");
    }
    
    // Create a new recording with a specified instrument
    public String createNewRecording(String instrument) {
        // Generate a unique ID based on timestamp
        String id = "recording_" + System.currentTimeMillis();
        
        // Get the next recording number for display
        int nextNumber = getAllRecordingIds().size() + 1;
        
        // Format instrument name for display
        String instrumentDisplay = formatInstrumentName(instrument);
        
        // Set default values
        recordingsConfig.set("recordings." + id + ".name", "§6" + instrumentDisplay + " Recording #" + nextNumber);
        recordingsConfig.set("recordings." + id + ".created", java.time.LocalDate.now().toString());
        recordingsConfig.set("recordings." + id + ".instrument", instrument);
        recordingsConfig.set("recordings." + id + ".notes", java.util.Collections.emptyList());
        
        // Save immediately
        saveRecordings();
        
        return id;
    }
    
    // ====================================
    // Format instrument name for display
    // Example: "celtic_harp" -> "Celtic Harp"
    // ====================================
    private String formatInstrumentName(String instrumentId) {
        if (instrumentId == null || instrumentId.equals("none")) {
            return "Unknown";
        }
        
        String[] words = instrumentId.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }

            // Capitalize first letter
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        
        return result.toString();
    }
    
    // Delete a recording by ID
    public void deleteRecording(String id) {
        recordingsConfig.set("recordings." + id, null);
        saveRecordings();
    }
    
    // Save a recording with slots data and metadata
    public String saveRecording(String name, String author, String description, java.util.UUID creatorUUID,
                                java.util.Map<Integer, java.util.Map<String, Object>> slotsData, 
                                java.util.Map<Integer, Double> rowDelays) {

        // Generate a unique ID based on timestamp
        String id = "recording_" + System.currentTimeMillis();
        
        // Set metadata values
        recordingsConfig.set("recordings." + id + ".metadata.name", name);
        recordingsConfig.set("recordings." + id + ".metadata.author", author);

        if (description != null && !description.isEmpty()) {
            recordingsConfig.set("recordings." + id + ".metadata.description", description);
        }

        recordingsConfig.set("recordings." + id + ".metadata.creator", creatorUUID.toString());
        recordingsConfig.set("recordings." + id + ".created", java.time.LocalDate.now().toString());
        
        // Save slots data
        if (slotsData != null && !slotsData.isEmpty()) {
            for (java.util.Map.Entry<Integer, java.util.Map<String, Object>> entry : slotsData.entrySet()) {
                int slot = entry.getKey();
                java.util.Map<String, Object> data = entry.getValue();
                
                recordingsConfig.set("recordings." + id + ".slots." + slot + ".sound", data.get("sound"));
                recordingsConfig.set("recordings." + id + ".slots." + slot + ".type", data.get("type"));
                recordingsConfig.set("recordings." + id + ".slots." + slot + ".source", data.get("source"));
            }
        }
        
        // Save row delays
        if (rowDelays != null && !rowDelays.isEmpty()) {
            for (java.util.Map.Entry<Integer, Double> entry : rowDelays.entrySet()) {
                recordingsConfig.set("recordings." + id + ".delays.row" + entry.getKey(), entry.getValue());
            }
        }
        
        // Save immediately
        saveRecordings();
        
        return id;
    }
    
    public FileConfiguration getRecordingsConfig() {
        return recordingsConfig;
    }

    // Grant a player access to a recording
    public void addAccessor(String id, java.util.UUID playerUUID) {
        String path = "recordings." + id + ".metadata.accessors";
        java.util.List<String> accessors = recordingsConfig.getStringList(path);
        String uuidStr = playerUUID.toString();

        if (!accessors.contains(uuidStr)) {
            accessors.add(uuidStr);
            recordingsConfig.set(path, accessors);

            saveRecordings();
        }
    }

    // ====================================
    // Check if a player has access to a recording
    // (creator always has access)
    // ====================================
    public boolean hasAccess(String id, java.util.UUID playerUUID) {
        String uuidStr = playerUUID.toString();
        String creator = getRecordingCreator(id);

        if (uuidStr.equals(creator))
        {
            return true;
        }

        return recordingsConfig.getStringList("recordings." + id + ".metadata.accessors").contains(uuidStr);
    }

    // Get all recording IDs visible to a specific player
    public java.util.Set<String> getAccessibleRecordingIds(java.util.UUID playerUUID) {
        java.util.Set<String> all = getAllRecordingIds();
        java.util.Set<String> visible = new java.util.LinkedHashSet<>();

        for (String id : all) {
            if (hasAccess(id, playerUUID)) {
                visible.add(id);
            }
        }

        return visible;
    }

    // ====================================
    // Get saved slot sounds for a recording
    // Returns map of slot -> {sound, type, source}
    // ====================================
    public java.util.Map<Integer, java.util.Map<String, String>> getSavedSlots(String id) {
        java.util.Map<Integer, java.util.Map<String, String>> result = new java.util.TreeMap<>();
        org.bukkit.configuration.ConfigurationSection section = recordingsConfig.getConfigurationSection("recordings." + id + ".slots");
        
        if (section == null)
        {
            return result;
        }

        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                java.util.Map<String, String> data = new java.util.HashMap<>();

                data.put("sound",  section.getString(key + ".sound", ""));
                data.put("type",   section.getString(key + ".type",  "vanilla"));
                data.put("source", section.getString(key + ".source", ""));

                result.put(slot, data);
            } catch (NumberFormatException ignored) {}
        }

        return result;
    }

    // ====================================
    // Get saved row delays for a recording
    // Returns map of row number (1-5) -> delay in seconds
    // ====================================
    public java.util.Map<Integer, Double> getSavedRowDelays(String id) {
        java.util.Map<Integer, Double> result = new java.util.HashMap<>();
        org.bukkit.configuration.ConfigurationSection section = recordingsConfig.getConfigurationSection("recordings." + id + ".delays");
        
        if (section == null)
        {
            return result;
        }

        for (String key : section.getKeys(false)) {
            // Keys are stored as "row1", "row2", etc.
            try {
                int row = Integer.parseInt(key.replace("row", ""));
                double val = section.getDouble(key);

                result.put(row, val);
            } catch (NumberFormatException ignored) {}
        }
        
        return result;
    }
}
