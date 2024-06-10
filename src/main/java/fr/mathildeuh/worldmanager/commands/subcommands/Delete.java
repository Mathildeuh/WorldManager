package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.pregen.Pregen;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Delete {

    private final JavaPlugin plugin;
    CommandSender sender;

    public Delete(CommandSender sender) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.sender = sender;
    }

    public void execute(String name) {
        World targetWorld = Bukkit.getWorld(name);

        if (targetWorld == null) {
            WorldManager.langConfig.sendFormat(sender, "delete.worldNotFound");
            return;
        }

        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            WorldManager.langConfig.sendFormat(sender, "delete.defaultWorld");
            return;
        }

        WorldManager.langConfig.sendFormat(sender, "delete.warning", name);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                WorldManager.langConfig.sendFormat(player, "delete.kickedPlayers");
            }
        }

        boolean unloadSuccess = plugin.getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            WorldManager.langConfig.sendFormat(sender, "delete.failedToDelete");
            return;
        }
        if (Pregen.generators.containsKey(name)) {
            Pregen.generators.get(name).cancel();
            Pregen.generators.remove(name);
        }

        File worldFolder = targetWorld.getWorldFolder();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                FileUtils.deleteDirectory(worldFolder);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        WorldManager.langConfig.sendFormat(sender, "delete.success");
        WorldManager.removeWorld(targetWorld.getName());

    }
}
