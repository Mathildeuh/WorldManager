package fr.mathildeuh.worldmanager.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** The {@code /wm gui} entry point: Create / Edit / Load. */
public final class MainMenuGui implements WMGui {

    private static final int CREATE_SLOT = 11;
    private static final int EDIT_SLOT = 13;
    private static final int LOAD_SLOT = 15;

    private final Player player;
    private final Inventory inventory;

    private MainMenuGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, GuiUtils.miniFromLang("gui.main.title"));
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.NEUTRAL);

        inventory.setItem(CREATE_SLOT, GuiUtils.glinted(GuiUtils.item(Material.GRASS_BLOCK,
                GuiUtils.miniFromLang("gui.main.create"), GuiUtils.miniFromLang("gui.main.create_lore"))));
        inventory.setItem(EDIT_SLOT, GuiUtils.item(Material.WRITABLE_BOOK,
                GuiUtils.miniFromLang("gui.main.edit"), GuiUtils.miniFromLang("gui.main.edit_lore")));
        inventory.setItem(LOAD_SLOT, GuiUtils.item(Material.CHEST,
                GuiUtils.miniFromLang("gui.main.load"), GuiUtils.miniFromLang("gui.main.load_lore")));
    }

    public static void open(Player player) {
        GuiManager.open(player, new MainMenuGui(player));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case CREATE_SLOT -> CreatorGui.open(player);
            case EDIT_SLOT -> EditorListGui.open(player, 0);
            case LOAD_SLOT -> LoaderGui.open(player, 0);
            default -> { }
        }
    }
}
