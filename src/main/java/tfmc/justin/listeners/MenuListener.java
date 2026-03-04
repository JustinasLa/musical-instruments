package tfmc.justin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tfmc.justin.InstrumentPlugin;
import tfmc.justin.managers.RecordingsManager;
import tfmc.justin.menu.AssignedSound;
import tfmc.justin.menu.InstrumentSelectionMenu;
import tfmc.justin.menu.RecordingCreationMenu;
import tfmc.justin.menu.RecordingMetadataMenu;
import tfmc.justin.menu.RecordingOptionsMenu;
import tfmc.justin.menu.RecordingsMenu;
import tfmc.justin.menu.SoundSelectionMenu;
import tfmc.justin.menu.VanillaSoundCategory;

// ====================================
// Handles clicks in all menu GUIs
// ====================================
public class MenuListener implements Listener {
    
    private final InstrumentPlugin plugin;
    private final RecordingsManager recordingsManager;
    private final RecordingsMenu recordingsMenu;
    private final RecordingCreationMenu recordingCreationMenu;
    private final InstrumentSelectionMenu instrumentSelectionMenu;
    private final SoundSelectionMenu soundSelectionMenu;
    private final DelayInputListener delayInputListener;
    private final RecordingMetadataMenu recordingMetadataMenu;
    private final MetadataInputListener metadataInputListener;
    private final RecordingOptionsMenu recordingOptionsMenu;

    public MenuListener(InstrumentPlugin plugin, RecordingsManager recordingsManager,
                       RecordingsMenu recordingsMenu, RecordingCreationMenu recordingCreationMenu,
                       InstrumentSelectionMenu instrumentSelectionMenu, SoundSelectionMenu soundSelectionMenu,
                       DelayInputListener delayInputListener, RecordingMetadataMenu recordingMetadataMenu,
                       MetadataInputListener metadataInputListener, RecordingOptionsMenu recordingOptionsMenu) {
        this.plugin = plugin;
        this.recordingsManager = recordingsManager;
        this.recordingsMenu = recordingsMenu;
        this.recordingCreationMenu = recordingCreationMenu;
        this.instrumentSelectionMenu = instrumentSelectionMenu;
        this.soundSelectionMenu = soundSelectionMenu;
        this.delayInputListener = delayInputListener;
        this.recordingMetadataMenu = recordingMetadataMenu;
        this.metadataInputListener = metadataInputListener;
        this.recordingOptionsMenu = recordingOptionsMenu;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        String title = event.getView().getTitle();

        // Only handle our plugin menus
        boolean isPluginMenu = title.equals(RecordingsMenu.MENU_TITLE)
                || title.startsWith(RecordingCreationMenu.MENU_TITLE.replace("%page%", ""))
                || title.equals(InstrumentSelectionMenu.MENU_TITLE)
                || title.startsWith(SoundSelectionMenu.MENU_TITLE_PREFIX)
                || title.equals(RecordingMetadataMenu.MENU_TITLE)
                || title.equals(RecordingOptionsMenu.MENU_TITLE);

        if (!isPluginMenu) {
            return;
        }

        // Cancel the event to prevent taking items from plugin menus
        event.setCancelled(true);

        // Check if player clicked in the top inventory (not their own)
        if (event.getClickedInventory() == null || event.getClickedInventory() != inventory) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Handle recordings menu
        if (title.equals(RecordingsMenu.MENU_TITLE)) {
            handleRecordingsMenuClick(player, slot);
        }

        // Handle recording creation menu
        else if (title.startsWith(RecordingCreationMenu.MENU_TITLE.replace("%page%", ""))) {
            handleRecordingCreationMenuClick(player, slot, event);
        }

        // Handle instrument selection menu
        else if (title.equals(InstrumentSelectionMenu.MENU_TITLE)) {
            handleInstrumentSelectionClick(player, slot);
        }

        // Handle sound selection menu
        else if (title.startsWith(SoundSelectionMenu.MENU_TITLE_PREFIX)) {
            handleSoundSelectionClick(player, slot, event);
        }

        // Handle recording metadata menu
        else if (title.equals(RecordingMetadataMenu.MENU_TITLE)) {
            handleMetadataMenuClick(player, slot);
        }

        // Handle recording options menu
        else if (title.equals(RecordingOptionsMenu.MENU_TITLE)) {
            handleRecordingOptionsClick(player, slot);
        }
    }
    
    // ====================================
    // Handle clicks in the recordings menu
    // ====================================
    private void handleRecordingsMenuClick(Player player, int slot) {
        // Handle "Create New Recording" button click
        if (slot == RecordingsMenu.CREATE_BUTTON_SLOT) {
            recordingCreationMenu.clearPlayerData(player);
            recordingCreationMenu.openMenu(player);

            return;
        }

        // Handle clicking a recording the player owns
        if (recordingsMenu.isOwnerSlot(player, slot)) {
            String id = recordingsMenu.getRecordingIdFromSlot(player, slot);
            if (id != null) {
                recordingOptionsMenu.openMenu(player, id);
            }

            return;
        }

        // Handle clicking a recording the player does NOT own
        String id = recordingsMenu.getRecordingIdFromSlot(player, slot);
        if (id != null) {
            recordingOptionsMenu.openMenuForOther(player, id);
        }
    }

    // ====================================
    // Handle clicks in the recording options menu
    // ====================================
    private void handleRecordingOptionsClick(Player player, int slot) {
        String id = recordingOptionsMenu.getRecordingId(player);

        if (slot == RecordingOptionsMenu.BACK_SLOT) {
            recordingOptionsMenu.cleanup(player);
            recordingsMenu.openMenu(player);

            return;
        }

        if (id == null)
        {
            return;
        }

        if (slot == RecordingOptionsMenu.DELETE_SLOT) {
            recordingOptionsMenu.cleanup(player);
            recordingsManager.deleteRecording(id);
            player.closeInventory();
            player.sendMessage("§c§lRecording deleted.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);

            return;
        }

        if (slot == RecordingOptionsMenu.EDIT_SLOT) {
            recordingOptionsMenu.cleanup(player);
            recordingCreationMenu.loadRecordingForEditing(player, id);

            return;
        }

        if (slot == RecordingOptionsMenu.COPY_SLOT || slot == RecordingOptionsMenu.OTHER_COPY_SLOT) {
            if (id == null)
            {
                return;
            }

            recordingOptionsMenu.cleanup(player);

            ItemStack copyItem = plugin.getRecordingCopyListener().createCopyItem(id);

            player.closeInventory();
            player.getInventory().addItem(copyItem);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);

            return;
        }

        if (slot == RecordingOptionsMenu.SHARE_SLOT) {
            if (id == null)
            {
                return;
            }

            recordingOptionsMenu.cleanup(player);

            ItemStack scroll = plugin.getRecordingCopyListener().createShareScroll(id);

            player.closeInventory();
            player.getInventory().addItem(scroll);
            player.sendMessage("§6§lBard's Notation §7added to your inventory!");
            player.sendMessage("§7Hand it to another player. They can right-click it to receive a playable copy.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);

            return;
        }
    }
    
    // ====================================
    // Handle clicks in the recording creation menu
    // ====================================
    private void handleRecordingCreationMenuClick(Player player, int slot, InventoryClickEvent event) {
        // Handle previous page button
        if (slot == RecordingCreationMenu.PREV_PAGE_SLOT) {
            recordingCreationMenu.handlePreviousPage(player);

            return;
        }
        
        // Handle next page button
        if (slot == RecordingCreationMenu.NEXT_PAGE_SLOT) {
            recordingCreationMenu.handleNextPage(player);

            return;
        }
        
        // Handle preview button
        if (slot == RecordingCreationMenu.PREVIEW_SLOT) {
            recordingCreationMenu.previewRecording(player);

            return;
        }
        
        // Handle save button
        if (slot == RecordingCreationMenu.SAVE_SLOT) {
            // If editing an existing recording, pre-fill metadata fields
            String editingId = recordingCreationMenu.getEditingId(player);

            if (editingId != null) {
                recordingMetadataMenu.prefillFromRecording(player, editingId);
            }

            // Open metadata menu to collect name, author, and description before saving
            recordingMetadataMenu.openMenu(player);

            return;
        }
        
        // Handle cancel button
        if (slot == RecordingCreationMenu.CANCEL_SLOT) {
            recordingCreationMenu.clearEditingId(player);
            player.closeInventory();
            player.sendMessage("§cCancelled recording creation.");

            return;
        }
        
        // Handle page info (does nothing)
        if (slot == RecordingCreationMenu.PAGE_INFO_SLOT) {
            return;
        }
        
        // Check if clicked on a glass pane (gray or lime)
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null &&
            (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE ||
             clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE)) {

            // Right-click on a filled slot removes the sound
            if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE && event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                int absSlot = recordingCreationMenu.toAbsoluteSlot(slot, recordingCreationMenu.getCurrentPage(player));

                recordingCreationMenu.removeAssignedSound(player, absSlot);
                player.sendMessage("§cSound removed from slot §e" + (absSlot + 1));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                recordingCreationMenu.openMenu(player, recordingCreationMenu.getCurrentPage(player));

                return;
            }

            // Left-click opens instrument selection (pass absolute slot)
            int absSlot = recordingCreationMenu.toAbsoluteSlot(slot, recordingCreationMenu.getCurrentPage(player));
            instrumentSelectionMenu.openMenu(player, absSlot);

            return;
        }
        
        // Handle clicking on paper items
        if (RecordingCreationMenu.isPaperSlot(slot)) {
            int physicalRow = RecordingCreationMenu.getRowNumberFromSlot(slot);

            if (physicalRow != -1) {
                int absoluteRow = recordingCreationMenu.toAbsoluteRow(physicalRow, recordingCreationMenu.getCurrentPage(player));
                delayInputListener.requestDelayInput(player, absoluteRow);
            }

            return;
        }
    }
    
    // ====================================
    // Handle clicks in the instrument selection menu
    // ====================================
    private void handleInstrumentSelectionClick(Player player, int slot) {

        // Handle Back button click
        if (slot == InstrumentSelectionMenu.BACK_BUTTON_SLOT) {
            player.closeInventory();

            // Reopen the recording creation menu on the same page
            recordingCreationMenu.openMenu(player, recordingCreationMenu.getCurrentPage(player));

            // Clean up
            instrumentSelectionMenu.cleanup(player);

            return;
        }
        
        // Check if this is a vanilla sound category
        if (instrumentSelectionMenu.isVanillaCategory(player, slot)) {
            VanillaSoundCategory category = instrumentSelectionMenu.getVanillaCategoryFromSlot(player, slot);

            if (category != null) {
                int targetSlot = instrumentSelectionMenu.getTargetSlot(player);

                // Open sound selection menu for vanilla category
                soundSelectionMenu.openMenuForVanillaCategory(player, category, targetSlot);
            }

            return;
        }
        
        // Get the instrument from the slot
        String instrumentId = instrumentSelectionMenu.getInstrumentFromSlot(slot);
        
        if (instrumentId == null) {
            return; // Empty slot or separator, do nothing
        }
        
        // Get the target slot from the previous menu
        int targetSlot = instrumentSelectionMenu.getTargetSlot(player);
        
        // Open sound selection menu for this instrument
        soundSelectionMenu.openMenu(player, instrumentId, targetSlot);
        
        // Clean up
        instrumentSelectionMenu.cleanup(player);
    }
    
    // ====================================
    // Handle clicks in the sound selection menu
    // ====================================
    private void handleSoundSelectionClick(Player player, int slot, InventoryClickEvent event) {
        // Handle Back button click
        if (slot == SoundSelectionMenu.BACK_BUTTON_SLOT) {

            // Get the target slot to pass back
            int targetSlot = soundSelectionMenu.getTargetSlot(player);

            // Clean up first
            soundSelectionMenu.cleanup(player);

            // Reopen instrument selection menu
            instrumentSelectionMenu.openMenu(player, targetSlot);

            return;
        }
        
        // Handle "Add" button click
        if (slot == SoundSelectionMenu.ADD_BUTTON_SLOT) {
            handleAddSoundClick(player);

            return;
        }
        
        // Handle Previous Page button click
        if (slot == SoundSelectionMenu.PREVIOUS_PAGE_SLOT) {
            soundSelectionMenu.handlePreviousPage(player);

            return;
        }
        
        // Handle Next Page button click
        if (slot == SoundSelectionMenu.NEXT_PAGE_SLOT) {
            soundSelectionMenu.handleNextPage(player);

            return;
        }
        
        // Check if clicked on a note block or lime dye (sound preview)
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && (clickedItem.getType() == Material.NOTE_BLOCK || clickedItem.getType() == Material.LIME_DYE)) {
            // Get the sound from the slot
            String sound = soundSelectionMenu.getSoundFromSlot(player, slot);
            
            if (sound != null) {
                // Preview the sound
                soundSelectionMenu.previewSound(player, sound);
                
                // Mark this sound as selected
                soundSelectionMenu.setSelectedSound(player, sound);
                
                // Update the menu in place (without closing) to show the selection
                soundSelectionMenu.updateMenuInPlace(player);
            }
            return;
        }
    }
    
    // ====================================
    // Handle clicks in the recording metadata menu
    // ====================================
    private void handleMetadataMenuClick(Player player, int slot) {
        if (slot == RecordingMetadataMenu.NAME_SLOT) {
            metadataInputListener.requestMetadataInput(player, "name");

            return;
        }
        if (slot == RecordingMetadataMenu.AUTHOR_SLOT) {
            metadataInputListener.requestMetadataInput(player, "author");

            return;
        }
        if (slot == RecordingMetadataMenu.DESCRIPTION_SLOT) {
            metadataInputListener.requestMetadataInput(player, "description");

            return;
        }
        if (slot == RecordingMetadataMenu.SAVE_SLOT) {
            if (!recordingMetadataMenu.canSave(player)) {
                player.sendMessage("§cPlease fill in the required fields first!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                return;
            }

            String name = recordingMetadataMenu.getRecordingName(player);
            String author = recordingMetadataMenu.getAuthor(player);
            String description = recordingMetadataMenu.getDescription(player);

            recordingMetadataMenu.clearPlayerData(player);
            recordingCreationMenu.saveRecording(player, name, author, description);

            return;
        }

        if (slot == RecordingMetadataMenu.CANCEL_SLOT) {
            recordingMetadataMenu.clearPlayerData(player);
            player.closeInventory();
            recordingCreationMenu.openMenu(player);

            return;
        }
    }

    // ====================================
    // Handle clicking the "Add" button in sound selection menu
    // ====================================
    private void handleAddSoundClick(Player player) {
        // Get the selected sound
        String sound = soundSelectionMenu.getSelectedSound(player);
        
        if (sound == null) {
            player.sendMessage("§cPlease select a sound first!");
            
            return;
        }
        
        // Get the target slot and instrument/category
        int targetSlot = soundSelectionMenu.getTargetSlot(player);
        String instrumentId = soundSelectionMenu.getSelectedInstrument(player);
        VanillaSoundCategory category = soundSelectionMenu.getSelectedVanillaCategory(player);
        
        // Create the assigned sound object
        AssignedSound assignedSound;
        if (category != null) {
            // Vanilla sound
            assignedSound = new AssignedSound(category, sound);
        } else {
            // Custom instrument sound
            assignedSound = new AssignedSound(instrumentId, sound);
        }
        
        // Store the sound for this slot
        recordingCreationMenu.setAssignedSound(player, targetSlot, assignedSound);
        
        // Send confirmation message
        player.sendMessage("§aAdded sound: §e" + sound + "§a to slot §e" + (targetSlot + 1));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Clean up
        soundSelectionMenu.cleanup(player);

        // Return to the same page the slot belongs to
        int targetPage = (targetSlot / 40) + 1;
        recordingCreationMenu.openMenu(player, targetPage);
    }
}
