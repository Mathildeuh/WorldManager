package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;

public class GUIUnload implements Listener {
    public GUIUnload(WorldManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        SGMenu menu = WorldManager.getSpiGUI().create("&6Unload a world", 6);
        List<World> sortedWorlds = Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(Bukkit.getWorlds().get(0)))
                .sorted((world1, world2) -> world1.getName().compareToIgnoreCase(world2.getName()))
                .toList();
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

        menu.setButton((9 * 6) - 1, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to open main menu")
                .build()
        ).withListener(event -> {
            new GUIMain(player);
        }));

        player.openInventory(menu.getInventory());
    }
}
