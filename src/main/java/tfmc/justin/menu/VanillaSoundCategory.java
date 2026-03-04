package tfmc.justin.menu;

import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

// ====================================
// Represents different categories of vanilla Minecraft sounds
// Used to organize sounds in the instrument selection menu
// ====================================
public enum VanillaSoundCategory {
    
    NOTE_BLOCKS("Note Block Sounds", Material.NOTE_BLOCK, "§e§lNote Block Sounds"),
    BLOCKS("Block Sounds", Material.STONE, "§7§lBlock Sounds"),
    MUSIC("Music Discs", Material.MUSIC_DISC_CAT, "§d§lMusic & Discs"),
    ENTITIES("Entity Sounds", Material.ZOMBIE_HEAD, "§a§lEntity/Mob Sounds"),
    AMBIENT("Ambient Sounds", Material.SOUL_LANTERN, "§b§lAmbient Sounds"),
    UI("UI Sounds", Material.BELL, "§6§lUI Sounds"),
    OTHER("Other Sounds", Material.JUKEBOX, "§f§lOther Sounds");
    
    private final String name;
    private final Material icon;
    private final String displayName;
    
    VanillaSoundCategory(String name, Material icon, String displayName) {
        this.name = name;
        this.icon = icon;
        this.displayName = displayName;
    }
    
    public String getName() { return name;  }
    public Material getIcon() { return icon; }
    public String getDisplayName() { return displayName; }
    
    // ====================================
    // Get all sounds for this category
    // ====================================
    public List<String> getSounds() {
        List<String> sounds = new ArrayList<>();
        
        for (Sound sound : Sound.values()) {
            String soundName = sound.name().toLowerCase();
            
            switch (this) {
                case NOTE_BLOCKS:
                    if (soundName.contains("note_block") || soundName.startsWith("block_note_block")) {
                        sounds.add(sound.getKey().toString());
                    }
                    break;
                    
                case BLOCKS:
                    if (soundName.startsWith("block_") && !soundName.contains("note_block")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
                    
                case MUSIC:
                    if (soundName.startsWith("music") || soundName.contains("music_disc")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
                    
                case ENTITIES:
                    if (soundName.startsWith("entity_")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
                    
                case AMBIENT:
                    if (soundName.startsWith("ambient_")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
                    
                case UI:
                    if (soundName.startsWith("ui_")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
                    
                case OTHER:
                    // Get sounds that don't fit in other categories
                    if (!soundName.startsWith("block_") && 
                        !soundName.startsWith("entity_") && 
                        !soundName.startsWith("ambient_") && 
                        !soundName.startsWith("ui_") && 
                        !soundName.startsWith("music") && 
                        !soundName.contains("music_disc")) {
                        sounds.add(sound.getKey().toString());
                    }

                    break;
            }
        }
        
        // Sort alphabetically
        Collections.sort(sounds);
        
        return sounds;
    }
    
    // ====================================
    // Get category from string identifier
    // ====================================
    public static VanillaSoundCategory fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
