package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import fr.mathildeuh.worldmanager.util.WorldLifecycle;
import fr.mathildeuh.worldmanager.util.WorldOperationLock;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;

public class Unload {
    private final CommandSender sender;

    public Unload(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String worldName) {
        SchedulerUtil.runGlobal(() -> unload(worldName));
    }

    private void unload(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldManager.langConfig.sendError(sender, "unload.world_not_loaded", worldName);
            return;
        }
        if (world.equals(Bukkit.getWorlds().get(0))) {
            WorldManager.langConfig.sendError(sender, "unload.rejected", worldName);
            return;
        }
        if (!WorldOperationLock.tryLock(worldName)) {
            WorldManager.langConfig.sendError(sender, "general.operation_in_progress", worldName);
            return;
        }
        AtomicBoolean warned = new AtomicBoolean();
        WorldLifecycle.evacuate(world, player -> {
            if (warned.compareAndSet(false, true)) WorldManager.langConfig.sendError(sender, "unload.players_in_world");
        }).whenComplete((evacuated, error) ->
                WorldLifecycle.completeOnGlobal(worldName, () -> {
                    if (error != null || !Boolean.TRUE.equals(evacuated)) {
                        finish(worldName, false, error);
                        return;
                    }
                    WorldLifecycle.unload(world, true).whenComplete((unloaded, unloadError) ->
                            WorldLifecycle.completeOnGlobal(worldName, () ->
                                    finish(worldName, Boolean.TRUE.equals(unloaded), unloadError)));
                }));
    }

    private void finish(String worldName, boolean unloaded, Throwable error) {
        try {
            if (error != null) {
                WorldManager.getInstance().getLogger().log(Level.WARNING, "Could not unload world " + worldName, error);
                WorldManager.langConfig.sendError(sender, "unload.failed", worldName);
            } else if (unloaded) {
                WorldManager.removeWorld(worldName);
                WorldManager.langConfig.sendSuccess(sender, "unload.success", worldName);
            } else {
                WorldManager.langConfig.sendError(sender, "unload.rejected", worldName);
            }
        } finally {
            WorldOperationLock.unlock(worldName);
        }
    }
}
