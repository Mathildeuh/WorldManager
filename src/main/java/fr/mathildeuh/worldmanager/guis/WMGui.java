package fr.mathildeuh.worldmanager.guis;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * A single chest-GUI screen. Implementations own one {@link Inventory} instance (returned
 * unchanged across calls to {@link #getInventory()}) and handle their own clicks - there is
 * no central slot/action dispatch table, each screen is self-contained, mirroring how the
 * previous Dialog-based screens each owned their own callbacks.
 */
public interface WMGui {

    Inventory getInventory();

    /** Called for every click inside this GUI's own inventory. {@link GuiListener} already cancels the event. */
    void onClick(InventoryClickEvent event);

    /** Called when this GUI's inventory is closed (including when the player navigates away). */
    default void onClose(InventoryCloseEvent event) { }
}
