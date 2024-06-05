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


        List<File> worldFolders = Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                .filter(File::isDirectory)
                .filter(folder -> new File(folder, "level.dat").exists())
                .sorted(Comparator.comparing(File::getName))
                .toList();

        int menuSize = Math.min((worldFolders.size() + 8) / 9 * 9, 54);
        if (menuSize <= 9) {
            menuSize = 27;
        }

        menu = WorldManager.getSpiGUI().create("&aChoose a world", menuSize/9);

        int id = 0;
        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            if (Bukkit.getWorld(worldName) != null) {
                continue;
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
            menu.setRowsPerPage(3);
            menuSize = 27;
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
            new GUIMain(player);
        }));



        player.openInventory(menu.getInventory());
    }
}
