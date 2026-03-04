package tfmc.justin.menu;

// ====================================
// Represents a sound assigned to a slot in the recording
// ====================================
public class AssignedSound {
    
    private final String instrumentId;  // null if vanilla sound
    private final VanillaSoundCategory category;  // null if custom instrument
    private final String soundName;
    
    // Constructor for custom instrument sound
    public AssignedSound(String instrumentId, String soundName) {
        this.instrumentId = instrumentId;
        this.category = null;
        this.soundName = soundName;
    }
    
    // Constructor for vanilla sound
    public AssignedSound(VanillaSoundCategory category, String soundName) {
        this.instrumentId = null;
        this.category = category;
        this.soundName = soundName;
    }
    
    public boolean isVanilla() { return category != null; }
    public String getInstrumentId() { return instrumentId; }
    public VanillaSoundCategory getCategory() { return category; }
    public String getSoundName() { return soundName; }
    
    // Get display name for the instrument/category
    public String getSourceDisplayName() {
        if (isVanilla()) {
            return category.getName();
        } else if (instrumentId == null) {
            return "Unknown";
        } else {
            // Format instrument ID (e.g., "celtic_harp" -> "Celtic Harp")
            String[] words = instrumentId.split("_");
            StringBuilder result = new StringBuilder();

            for (String word : words) {
                if (result.length() > 0) {
                    result.append(" ");
                }

                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
            }
            
            return result.toString();
        }
    }
}
