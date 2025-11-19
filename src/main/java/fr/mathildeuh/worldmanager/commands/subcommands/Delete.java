package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
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
            WorldManager.langConfig.sendError(sender, "delete.world_not_found");
            return;
        }

        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            WorldManager.langConfig.sendError(sender, "delete.default_world");
            return;
        }

        WorldManager.langConfig.sendWaiting(sender, "delete.warning", name);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                WorldManager.langConfig.sendError(player, "delete.kicked_players");
            }
        }

        boolean unloadSuccess = plugin.getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            WorldManager.langConfig.sendError(sender, "delete.failed_to_delete");
            return;
        }

        File worldFolder = targetWorld.getWorldFolder();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                FileUtils.deleteDirectory(worldFolder);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        WorldManager.langConfig.sendSuccess(sender, "delete.success");
        WorldManager.removeWorld(targetWorld.getName());

    }
}
