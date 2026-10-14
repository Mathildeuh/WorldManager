package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import fr.mathildeuh.worldmanager.util.WorldLifecycle;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import fr.mathildeuh.worldmanager.util.WorldOperationLock;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.logging.Level;

public class Delete {
    private final CommandSender sender;

    public Delete(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String name) {
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(sender, "general.invalid_world_name", name);
            return;
        }
        SchedulerUtil.runGlobal(() -> delete(name));
    }

    private void delete(String name) {
        World targetWorld = Bukkit.getWorld(name);
        if (targetWorld == null) {
            WorldManager.langConfig.sendError(sender, "delete.world_not_found");
            return;
        }
        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            WorldManager.langConfig.sendError(sender, "delete.default_world");
            return;
        }
        if (!WorldOperationLock.tryLock(name)) {
            WorldManager.langConfig.sendError(sender, "general.operation_in_progress", name);
            return;
        }
        WorldManager.langConfig.sendWaiting(sender, "delete.warning", name);
        File worldFolder = targetWorld.getWorldFolder();
        WorldLifecycle.evacuate(targetWorld,
                player -> WorldManager.langConfig.sendError(player, "delete.kicked_players"))
                .whenComplete((evacuated, error) -> WorldLifecycle.completeOnGlobal(name, () -> {
                    if (error != null || !Boolean.TRUE.equals(evacuated)) {
                        failed(name, error);
                        return;
                    }
                    WorldLifecycle.unload(targetWorld, false).whenComplete((unloaded, unloadError) ->
                            WorldLifecycle.completeOnGlobal(name, () -> {
                                if (unloadError != null || !Boolean.TRUE.equals(unloaded)) {
                                    failed(name, unloadError);
                                    return;
                                }
                                // No file is touched until the unload callback confirms success.
                                SchedulerUtil.runAsync(() -> {
                                    try {
                                        FileUtils.deleteDirectory(worldFolder);
                                        WorldLifecycle.completeOnGlobal(name, () -> {
                                            try {
                                                WorldManager.removeWorld(name);
                                                WorldManager.langConfig.sendSuccess(sender, "delete.success");
                                            } finally {
                                                WorldOperationLock.unlock(name);
                                            }
                                        });
                                    } catch (Exception e) {
                                        WorldLifecycle.completeOnGlobal(name, () -> failed(name, e));
                                    }
                                });
                            }));
                }));
    }

    private void failed(String name, Throwable error) {
        WorldOperationLock.unlock(name);
        if (error != null) WorldManager.getInstance().getLogger().log(Level.WARNING, "Could not delete world " + name, error);
        WorldManager.langConfig.sendError(sender, "delete.failed", name);
    }
}
