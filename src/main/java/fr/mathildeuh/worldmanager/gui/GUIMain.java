package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.buttons.SGButtonListener;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class GUIMain {

    static SGMenu menu;
    private final WorldManager plugin;
    private final Player player;

    public GUIMain(Player player) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.player = player;

        openMainMenu();
    }

    public static void openWorldChoose(Player player) {
        List<String> worldNames = Bukkit.getWorlds().stream().map(org.bukkit.World::getName).collect(Collectors.toList());
        int menuSize = Math.min((worldNames.size() + 8) / 9 * 9, 54);
        if (menuSize <= 9) {
            menuSize = 27;
        }
        menu = WorldManager.getSpiGUI().create("&aChoose a world to manage", menuSize / 9);

        List<World> sortedWorlds = Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(Bukkit.getWorlds().get(0)))
                .sorted((world1, world2) -> world1.getName().compareToIgnoreCase(world2.getName()))
                .toList();
        int id = 0;
        for (World world : sortedWorlds) {
            menu.setButton(id, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + world.getName())
                    .lore("§7Click to manage this world")
                    .build()
            ).withListener(event -> {
                new GUIManage(player, world).open();
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

    private void openMainMenu() {
        menu = WorldManager.getSpiGUI().create("&9{------ World Manager -----}", 3);

        menu.setButton(11, createButton(Material.TARGET, "§aCreate a world", "§7World creator options", event -> {
            new GUICreate(plugin).open(player);
        }));

        menu.setButton(13, createButton(Material.GRASS_BLOCK, "§aManage your worlds", "§7World manager options", event -> {
            openWorldChoose(player);
        }));

        menu.setButton(15, createButton(Material.DEBUG_STICK, "§aLoad a world", "§7World loader option", event -> {
            new GUILoad(plugin).open(player);
        }));

        player.openInventory(menu.getInventory());
    }

    private SGButton createButton(Material material, String name, String lore, SGButtonListener listener) {
        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build()
        ).withListener(listener);
    }
}
