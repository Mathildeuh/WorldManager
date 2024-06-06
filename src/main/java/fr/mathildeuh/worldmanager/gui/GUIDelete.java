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

public class GUIDelete implements Listener {
    public GUIDelete() {
        Bukkit.getPluginManager().registerEvents(this, WorldManager.getPlugin(WorldManager.class));

    }

    public void confirmDelete(Player player, World world) {
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
            new GUIMain(player);
        }));
        player.openInventory(menu.getInventory());
    }

}
