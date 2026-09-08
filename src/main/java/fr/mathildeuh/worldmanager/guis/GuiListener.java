package fr.mathildeuh.worldmanager.guis;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Single global listener routing inventory events to whichever {@link WMGui} a player has
 * open (tracked by {@link GuiManager}). Every click while a WMGui is open is cancelled - both
 * in the GUI's own inventory (so players can't take/rearrange the display items) and in the
 * player's own inventory below it (so a shift-click can't dump a real item into the fake
 * screen) - only clicks inside the GUI's own inventory are then forwarded to it.
 */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        WMGui gui = GuiManager.get(player.getUniqueId());
        if (gui == null) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory().equals(gui.getInventory())) {
            GuiUtils.playClick(player);
            try {
                gui.onClick(event);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[WorldManager] GUI click handler threw an exception: " + e);
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (GuiManager.get(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        WMGui gui = GuiManager.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui.getInventory())) {
            // Either nothing tracked, or this close event is for a screen the player already
            // navigated away from (a new GUI is opened, and thus tracked, before the old
            // inventory's close event fires) - nothing to clean up in that case.
            return;
        }
        GuiManager.remove(player.getUniqueId());
        gui.onClose(event);
    }
}
