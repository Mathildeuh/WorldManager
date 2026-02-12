package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LoaderGUI {
    private final ChestGui gui;

    public LoaderGUI() {
        List<File> worldFolders = getWorldFolders();
        this.gui = createGui(worldFolders);
    }

    private List<File> getWorldFolders() {
        return Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                .filter(File::isDirectory)
                .filter(folder -> new File(folder, "level.dat").exists())
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }

    private ChestGui createGui(List<File> worldFolders) {
        List<File> unloadedWorldFolders = new ArrayList<>(worldFolders.stream()
                .filter(worldFolder -> Bukkit.getWorld(worldFolder.getName()) == null)
                .toList());

        ChestGui gui = new ChestGui(3, "World Loader");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 3);

        int worldsPerPage = 18;
        int pageCount = (int) Math.ceil((double) unloadedWorldFolders.size() / worldsPerPage);

        for (int page = 0; page < pageCount; page++) {
            StaticPane pane = new StaticPane(0, 0, 9, 4);
            int start = page * worldsPerPage;
            int end = Math.min(start + worldsPerPage, unloadedWorldFolders.size());

            for (int index = start; index < end; index++) {
                File worldFolder = unloadedWorldFolders.get(index);
                String worldName = worldFolder.getName();

                ItemStack worldItem = new ItemBuilder(Material.GRASS_BLOCK)
                        .name("&2&n" + worldName)
                        .lore("", "&7&oLoad the world " + worldName)
                        .build();

                int slotIndex = index % worldsPerPage;
                pane.addItem(new GuiItem(worldItem, event -> handleItemClick(event, worldName)), Slot.fromIndex(slotIndex));
            }

            paginatedPane.addPane(page, pane);
        }

        String guiTitle = buildGuiTitle(paginatedPane);
        gui.setTitle(guiTitle);

        if (pageCount > 0) {
            paginatedPane.setPage(0);
        }

        gui.addPane(paginatedPane);

        StaticPane navigationPane = new StaticPane(0, 2, 9, 1);

        ItemStack prevPageItem = new ItemBuilder(Material.MAGENTA_DYE)
                .name("&aPrevious Page")
                .build();

        navigationPane.addItem(new GuiItem(prevPageItem, event -> {
            if (paginatedPane.getPage() > 0) {
                paginatedPane.setPage(paginatedPane.getPage() - 1);
                gui.setTitle(buildGuiTitle(paginatedPane));
                gui.update();
            }
        }), Slot.fromIndex(3));

        ItemStack nextPageItem = new ItemBuilder(Material.LIME_DYE)
                .name("&aNext Page")
                .build();
        navigationPane.addItem(new GuiItem(nextPageItem, event -> {
            if (paginatedPane.getPage() < pageCount - 1) {
                paginatedPane.setPage(paginatedPane.getPage() + 1);
                gui.setTitle(buildGuiTitle(paginatedPane));
                gui.update();
            }
        }), Slot.fromIndex(5));

        gui.addPane(navigationPane);

        StaticPane backButton = new StaticPane(0, 2, 9, 1);
        ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&cBack")
                .build();
        backButton.addItem(new GuiItem(backItem, event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.MAIN.open(player);
            }
        }), Slot.fromIndex(8));

        gui.addPane(backButton);

        return gui;
    }

    private String buildGuiTitle(PaginatedPane paginatedPane) {
        int currentPage = paginatedPane.getPage() + 1;
        int totalPageCount = paginatedPane.getPages();
        return "World Loader - Page " + currentPage + "/" + totalPageCount;
    }

    private void handleItemClick(InventoryClickEvent event, String worldName) {
        if (event.getWhoClicked() instanceof Player player) {
            new Load(player).execute(worldName, "normal", null);
            GUIList.LOADER.open(player);
        }
    }

    public ChestGui getGui() {
        return this.gui;
    }
}

