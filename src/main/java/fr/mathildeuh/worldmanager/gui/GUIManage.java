package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;


public class GUIManage {

    Player player;
    World world;
    SGMenu menu;

    public GUIManage(Player player, World world) {
        this.player = player;
        this.world = world;
    }

    public void open() {
        menu = WorldManager.getSpiGUI().create("&cManage " + world.getName(), 3);

        menu.setButton(3, new SGButton(new ItemBuilder(Material.COMMAND_BLOCK)
                .name("§aGame Rules")
                .lore("§7Click to manage game rules")
                .build()
        ).withListener(event -> {
            new GUIGameRuleSettings(WorldManager.getPlugin(WorldManager.class), world).open(player);
        }));

        if (Bukkit.getWorlds().get(0) != world)
            menu.setButton(5, new SGButton(new ItemBuilder(Material.STRUCTURE_BLOCK)
                    .name("§cUnload")
                    .lore("§7Click to unload this world")
                    .build()
            ).withListener(event -> {
                new GUIMain(player);
                new Unload(player).execute(world.getName());
            }));

        // PREGEN
        menu.setButton(11, new SGButton(new ItemBuilder(Material.OAK_SAPLING)
                .name("§aPregen")
                .lore("§7Click to pregen this world")
                .build()
        ).withListener(event -> {
            new GUIPregen().open(player, world.getName());
        }));

        // TELEPORT
        menu.setButton(13, new SGButton(new ItemBuilder(Material.ENDER_PEARL)
                .name("§aTeleport")
                .lore("§7Click to teleport to this world")
                .build()
        ).withListener(event -> {
            player.teleport(world.getSpawnLocation());
        }));
        if (Bukkit.getWorlds().get(0) != world)
            menu.setButton(15, new SGButton(new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("§cDelete")
                    .lore("§7Click to delete this world")
                    .build()
            ).withListener(event -> {
                new GUIDelete().confirmDelete(player, world);
            }));

        if (Bukkit.getWorlds().get(0) != world)

        // BACKUP
            menu.setButton(21, new SGButton(new ItemBuilder(Material.EMERALD)
                    .name("§aBackup")
                    .lore("§7Click to backup this world")
                    .build()
            ).withListener(event -> {
                new Backup(player).execute(world.getName());
            }));

        if (Bukkit.getWorlds().get(0) != world)

        // RESTORE
            menu.setButton(23, new SGButton(new ItemBuilder(Material.DIAMOND)
                    .name("§bRestore")
                    .lore("§7Click to restore this world")
                    .build()
            ).withListener(event -> {
                new GUIMain(player);
                new Restore(player).execute(world.getName());
            }));


        menu.setButton(26, new SGButton(new ItemBuilder(Material.BARRIER)
                .name("§cBack")
                .lore("§7Click to go back")
                .build()
        ).withListener(event -> {
            GUIMain.openWorldChoose(player);
        }));

        player.openInventory(menu.getInventory());

    }

}
