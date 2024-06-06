package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GUIBackups implements Listener {
    SGMenu menu;

    public GUIBackups(WorldManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private List<String> getBackupFiles() {
        File backupDirectory = new File(Bukkit.getServer().getWorldContainer(), "backups/WorldManager");

        if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
            return new ArrayList<>();
        }

        return Arrays.stream(Objects.requireNonNull(backupDirectory.listFiles()))
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".zip"))
                .sorted()
                .collect(Collectors.toList());
    }

    public void open(Player player) {
        List<World> sortedWorlds = Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(Bukkit.getWorlds().get(0)))
                .sorted((world1, world2) -> world1.getName().compareToIgnoreCase(world2.getName()))
                .toList();

        int menuSize = Math.min((sortedWorlds.size() + 8) / 9 * 9, 54);
        if (menuSize <= 9) {
            menuSize = 18;
        }
        menu = WorldManager.getSpiGUI().create("&6Select world", menuSize / 9);

        int id = 0;


        for (World world : sortedWorlds) {


            menu.setButton(id, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + world.getName())
                    .lore("§7Click to backup or restore this world")
                    .build()
            ).withListener(event -> {
                openChoose(player, world);
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
            new GUIMain(player);
        }));

        player.openInventory(menu.getInventory());
    }

    private void openChoose(Player player, World world) {
        menu = WorldManager.getSpiGUI().create("&6Choose an option for " + world.getName(), 1);

        menu.setButton(3, new SGButton(new ItemBuilder(Material.GREEN_WOOL)
                .name("§aBackup")
                .lore("§7Backup this world")
                .build()
        ).withListener(event -> {
            player.closeInventory();

            new Backup(player).execute(world.getName());
        }));

        boolean isBackup = getBackupFiles().contains(world.getName() + ".zip");
        String name = isBackup ? "§aRestore" : "§cNo backup found";
        String lore = isBackup ? "§7Backup found, you can restore this world" : "§cUse §e\"Backup\" §cbutton to create one";

        menu.setButton(5, new SGButton(new ItemBuilder(Material.RED_WOOL)
                .name(name)
                .lore(lore)
                .build()
        ).withListener(event -> {
            player.closeInventory();

            new Restore(player).execute(world.getName());
        }));

        menu.setButton(8, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to go back")
                .build()
        ).withListener(event -> {
            open(player);
        }));

        player.openInventory(menu.getInventory());

    }
}
