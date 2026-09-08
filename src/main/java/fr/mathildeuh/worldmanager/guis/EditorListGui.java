package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.util.WorldFolders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** Paginated list of every world folder on disk, leading into {@link WorldOptionsGui}. */
public final class EditorListGui implements WMGui {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int EMPTY_SLOT = 22;

    private final Player player;
    private final Inventory inventory;
    private final List<String> worldsOnPage;
    private final GuiUtils.Page page;

    private EditorListGui(Player player, int requestedPage) {
        this.player = player;
        List<String> worlds = WorldFolders.listAllWorldFolders();
        this.page = GuiUtils.paginate(requestedPage, worlds.size(), PAGE_SIZE);
        this.worldsOnPage = worlds.subList(page.fromInclusive(), page.toExclusive());

        Component title = page.count() > 1
                ? GuiUtils.miniFromLang("gui.editor.title_paged", page.index() + 1, page.count())
                : GuiUtils.miniFromLang("gui.editor.title");
        this.inventory = Bukkit.createInventory(null, 54, title);
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.NEUTRAL);

        if (worldsOnPage.isEmpty()) {
            inventory.setItem(EMPTY_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.miniFromLang("gui.editor.empty")));
        } else {
            for (int i = 0; i < worldsOnPage.size(); i++) {
                String worldName = worldsOnPage.get(i);
                boolean loaded = Bukkit.getWorld(worldName) != null;
                String creator = WorldsConfig.getCreator(worldName);
                inventory.setItem(i, GuiUtils.item(
                        loaded ? Material.GRASS_BLOCK : Material.DIRT,
                        Component.text(worldName, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true),
                        GuiUtils.miniFromLang("gui.editor.created_by", creator),
                        loaded ? GuiUtils.miniFromLang("gui.editor.status_loaded") : GuiUtils.miniFromLang("gui.editor.status_unloaded")
                ));
            }
        }

        if (page.index() > 0) {
            inventory.setItem(PREV_SLOT, GuiUtils.item(Material.ARROW, GuiUtils.miniFromLang("gui.editor.previous")));
        }
        inventory.setItem(BACK_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.back")));
        if (page.index() < page.count() - 1) {
            inventory.setItem(NEXT_SLOT, GuiUtils.item(Material.ARROW, GuiUtils.miniFromLang("gui.editor.next")));
        }
    }

    public static void open(Player player, int page) {
        GuiManager.open(player, new EditorListGui(player, page));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == PREV_SLOT && page.index() > 0) {
            EditorListGui.open(player, page.index() - 1);
        } else if (slot == BACK_SLOT) {
            MainMenuGui.open(player);
        } else if (slot == NEXT_SLOT && page.index() < page.count() - 1) {
            EditorListGui.open(player, page.index() + 1);
        } else if (slot < worldsOnPage.size()) {
            WorldOptionsGui.open(player, worldsOnPage.get(slot));
        }
    }
}
