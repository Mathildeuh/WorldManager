package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class GUILoad implements Listener {
    public GUILoad(WorldManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        SGMenu menu = WorldManager.getSpiGUI().create("&aLoad a world", 6);

        List<File> worldFolders = Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                .filter(File::isDirectory)
                .filter(folder -> new File(folder, "level.dat").exists())
                .sorted(Comparator.comparing(File::getName))
                .toList();

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

        menu.setButton((9 * 6) - 2, new SGButton(new ItemBuilder(Material.PAPER)
                .name("§cINFORMATION")
                .lore("§aWorlds who are loaded from this gui are always as §eNORMAL §atype!")
                .build()
        ));

        menu.setButton((9 * 6) - 1, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to open main menu")
                .build()
        ).withListener(event -> {
            new GUIMain(player);
        }));

        if (menu.getButton(0) == null) {
            menu.setButton(0, new SGButton(new ItemBuilder(Material.BARRIER)
                    .name("§cNo world to load")
                    .lore("§7Putt worlds in the root folder to load them")
                    .build()
            ));
        }

        player.openInventory(menu.getInventory());
    }
}
