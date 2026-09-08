package fr.mathildeuh.worldmanager.guis;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Generic reusable yes/no confirmation screen for destructive actions. */
public final class ConfirmGui implements WMGui {

    private static final int CANCEL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 13;
    private static final int CONFIRM_SLOT = 15;

    private final Inventory inventory;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    private ConfirmGui(Component title, Component description, Runnable onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        this.inventory = Bukkit.createInventory(null, 27, title);
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.DESTRUCTIVE);
        inventory.setItem(DESCRIPTION_SLOT, GuiUtils.item(Material.PAPER, description));
        inventory.setItem(CANCEL_SLOT, GuiUtils.item(Material.RED_CONCRETE, GuiUtils.miniFromLang("gui.confirm.no")));
        inventory.setItem(CONFIRM_SLOT, GuiUtils.item(Material.LIME_CONCRETE, GuiUtils.miniFromLang("gui.confirm.yes")));
    }

    public static void open(Player player, Component title, Component description, Runnable onConfirm, Runnable onCancel) {
        GuiManager.open(player, new ConfirmGui(title, description, onConfirm, onCancel));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case CONFIRM_SLOT -> onConfirm.run();
            case CANCEL_SLOT -> onCancel.run();
            default -> { }
        }
    }
}
