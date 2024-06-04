package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.buttons.SGButtonListener;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Material;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIMain {

    private final WorldManager plugin;
    private final Player player;

    public GUIMain(Player player) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.player = player;

        openMainMenu();
    }

    private void openMainMenu() {
        SGMenu mainMenu = WorldManager.getSpiGUI().create("&9{------ World Manager -----}", 2);

        mainMenu.setButton(0, createButton(Material.GREEN_WOOL, "§aCreate a world", "§7Create a new world", event -> {
            new GUICreate(plugin).open(player);
        }));

        mainMenu.setButton(1, createButton(Material.RED_WOOL, "§cDelete a world", "§7Delete an existing world", event -> {
            new GUIDelete(plugin).open(player);
        }));

        mainMenu.setButton(3, createButton(Material.OAK_SAPLING, "§aPregen", "§7Pregen a world", event -> {
            new GUIPregen(plugin).open(player);
        }));

        mainMenu.setButton(5, createButton(Material.COMMAND_BLOCK, "§aManage game rules", "§7Manage world game rules", event -> {
            new GUIGameRules(plugin).open(player);
        }));

        mainMenu.setButton(7, createButton(Material.EMERALD_BLOCK, "§bLoad a world", "§7Import a new world", event -> {
            new GUILoad(plugin).open(player);
        }));

        mainMenu.setButton(8, createButton(Material.REDSTONE_BLOCK, "§6Unload a world", "§7Unload an existing world", event -> {
            new GUIUnload(plugin).open(player);
        }));

        mainMenu.setButton(16, createButton(Material.LIGHT, "§6Backup a world", "§7Backup an existing world", event -> {
            new GUIUnload(plugin).open(player);
        }));

        mainMenu.setButton(17, createButton(Material.JIGSAW, "§6Restore a world", "§7Restore an existing world", event -> {
            new GUIUnload(plugin).open(player);
        }));

        player.openInventory(mainMenu.getInventory());
    }

    private SGButton createButton(Material material, String name, String lore, SGButtonListener listener) {
        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build()
        ).withListener(listener);
    }
}
