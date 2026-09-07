package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Unload {

    private final CommandSender sender;

    public Unload(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldManager.langConfig.sendError(sender, "unload.world_not_loaded", worldName);
            return;
        }

        boolean hadPlayers = false;
        List<CompletableFuture<Boolean>> teleports = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            hadPlayers = true;
            teleports.add(player.teleportAsync(Bukkit.getWorlds().get(0).getSpawnLocation()));
        }
        if (hadPlayers) {
            WorldManager.langConfig.sendError(sender, "unload.players_in_world");
        }

        CompletableFuture.allOf(teleports.toArray(new CompletableFuture[0])).thenRun(() -> SchedulerUtil.runGlobal(() -> {
            try {
                boolean unloaded = Bukkit.unloadWorld(world, true);

                if (unloaded) {
                    WorldManager.langConfig.sendSuccess(sender, "unload.success", worldName);
                    WorldManager.removeWorld(worldName);
                } else {
                    WorldManager.langConfig.sendError(sender, "unload.rejected", worldName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                WorldManager.langConfig.sendError(sender, "unload.failed", worldName);
            }
        }));
    }
}
