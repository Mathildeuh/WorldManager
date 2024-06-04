package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Delete;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.stream.Collectors;

public class GUIDelete implements Listener {
    public GUIDelete(WorldManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

    }

    public void open(Player player) {
        List<String> worldNames = Bukkit.getWorlds().stream().map(org.bukkit.World::getName).collect(Collectors.toList());
        int menuSize = Math.min((worldNames.size() + 8) / 9 * 9, 54); // Taille dynamique en fonction du nombre de mondes, avec une limite de 54
        if (menuSize <= 9) {
            menuSize = 18;
        }
        SGMenu menu = WorldManager.getSpiGUI().create("&cDelete a world", menuSize/9);
        List<World> sortedWorlds = Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(Bukkit.getWorlds().get(0)))
                .sorted((world1, world2) -> world1.getName().compareToIgnoreCase(world2.getName()))
                .toList();
        int id = 0;
        for (World world : sortedWorlds) {
            menu.setButton(id, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + world.getName())
                    .lore("§7Click to delete this world")
                    .build()
            ).withListener(event -> {
                confirmDelete(player, world);
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

    private void confirmDelete(Player player, World world) {
        SGMenu menu = WorldManager.getSpiGUI().create("&cDelete " + world.getName() + " ?", 1);
        menu.setButton(3, new SGButton(new ItemBuilder(Material.RED_WOOL)
                .name("§c§lConfirm")
                .lore("§c§l⚠ THIS ACTION CAN NOT BE UNDONE ⚠")
                .build()
        ).withListener(event -> {
            new Delete(player).execute(world.getName());
            new GUIMain(player);
        }));
        menu.setButton(5, new SGButton(new ItemBuilder(Material.GREEN_WOOL)
                .name("§aCancel")
                .lore("§7Click to cancel deletion")
                .build()
        ).withListener(event -> {
            open(player);
        }));
        player.openInventory(menu.getInventory());
    }

}
