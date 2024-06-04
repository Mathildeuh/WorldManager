package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedMainHandEvent;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GUILoad implements Listener {
    public GUILoad(WorldManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    SGMenu menu;

    public void open(Player player) {

        menu = WorldManager.getSpiGUI().create("&6Choose an option", 1);

        menu.setButton(3, new SGButton(new ItemBuilder(Material.GREEN_WOOL)
                .name("§aLoad")
                .lore("§7Load a world")
                .build()
        ).withListener(event -> {
            openWorldLoad(player);
        }));

        menu.setButton(5, new SGButton(new ItemBuilder(Material.RED_WOOL)
                .name("§cUnload")
                .lore("§7Unload a world")
                .build()
        ).withListener(event -> {
            openWorldUnload(player);
        }));

        menu.setButton(8, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to go back")
                .build()
        ).withListener(event -> {
            new GUIMain(player);
        }));

        player.openInventory(menu.getInventory());
    }

    private void openWorldUnload(Player player){

        List<World> sortedWorlds = Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(Bukkit.getWorlds().get(0)))
                .sorted((world1, world2) -> world1.getName().compareToIgnoreCase(world2.getName()))
                .toList();

        int menuSize = Math.min((sortedWorlds.size() + 8) / 9 * 9, 54);
        if (menuSize <= 9) {
            menuSize = 18;
        }
        menu = WorldManager.getSpiGUI().create("&6Unload a world", menuSize/9);

        int id = 0;
        for (World world : sortedWorlds) {
            menu.setButton(id, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + world.getName())
                    .lore("§7Click to unload this world")
                    .build()
            ).withListener(event -> {
                new Unload(player).execute(world.getName());
                open(player);
            }));
            id++;
        }

        int backButtonSlot = menuSize - 1;
        if (menu.getButton(backButtonSlot) != null) {
            menuSize += 9;
            menu.setRowsPerPage(menuSize / 9);
        }

        menu.setButton(menuSize - 1, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to open main menu")
                .build()
        ).withListener(event -> {
            open(player);
        }));

        player.openInventory(menu.getInventory());
    }


    private void openWorldLoad(Player player) {



        List<File> worldFolders = Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                .filter(File::isDirectory)
                .filter(folder -> new File(folder, "level.dat").exists())
                .sorted(Comparator.comparing(File::getName))
                .toList();

        int menuSize = Math.min((worldFolders.size() + 8) / 9 * 9, 54);
        if (menuSize <= 9) {
            menuSize = 18;
        }

        menu = WorldManager.getSpiGUI().create("&aChoose a world", menuSize/9);

        int id = 0;
        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            // Check if a world with the same name is already loaded
            if (Bukkit.getWorld(worldName) != null) {
                continue; // Skip this world if it's already loaded
            }
            menu.setButton(id, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + worldName)
                    .lore("§7Click to load this world")
                    .build()
            ).withListener(event -> {
                new Load(player).execute(worldName, "normal", null);
                open(player);
            }));
            id++;
        }

        if (menu.getButton(0) == null) {
            menu.setRowsPerPage(2);
            menuSize = 18;
            menu.setButton(0, new SGButton(new ItemBuilder(Material.BARRIER)
                    .name("§cNo world to load")
                    .lore("§7Putt worlds in the root folder to load them")
                    .build()
            ));
        }

        menu.setButton(menuSize - 2, new SGButton(new ItemBuilder(Material.PAPER)
                .name("§cINFORMATION")
                .lore("§aWorlds who are loaded from this gui are always as §eNORMAL §atype!")
                .build()
        ));

        int backButtonSlot = menuSize - 1;
        if (menu.getButton(backButtonSlot) != null) {
            menuSize += 9;
            menu.setRowsPerPage(menuSize / 9);
        }

        menu.setButton(menuSize- 1, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to open main menu")
                .build()
        ).withListener(event -> {
            open(player);
        }));



        player.openInventory(menu.getInventory());
    }
}
