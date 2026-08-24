package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class Delete {

    CommandSender sender;

    public Delete(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String name) {
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(sender, "general.invalid_world_name", name);
            return;
        }

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

        boolean unloadSuccess = WorldManager.getInstance().getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            WorldManager.langConfig.sendError(sender, "delete.failed", name);
            return;
        }

        File worldFolder = targetWorld.getWorldFolder();
        String worldName = name;
        Bukkit.getScheduler().runTaskAsynchronously(WorldManager.getInstance(), () -> {
            try {
                FileUtils.deleteDirectory(worldFolder);
                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () -> {
                    WorldManager.langConfig.sendSuccess(sender, "delete.success");
                    WorldManager.removeWorld(worldName);
                });
            } catch (IOException e) {
                Bukkit.getLogger().warning("[WorldManager] Failed to delete world folder for " + worldName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () ->
                        WorldManager.langConfig.sendError(sender, "delete.async_failed", worldName));
            }
        });
    }
}
