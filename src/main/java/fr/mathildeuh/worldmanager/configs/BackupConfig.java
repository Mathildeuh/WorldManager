package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import fr.mathildeuh.worldmanager.util.WorldLifecycle;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import fr.mathildeuh.worldmanager.util.WorldOperationLock;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;

public class BackupConfig {

    public static File configFile;
    public static FileConfiguration config;

    /** Single source of truth for where a world's backup zip lives - both backup and restore must agree exactly. */
    private static File backupFileFor(String name) {
        File backupDir = new File(Bukkit.getWorldContainer(), "backups/WorldManager");
        return new File(backupDir, name.toLowerCase() + ".zip");
    }

    public BackupConfig(File configFile, FileConfiguration config) {
        BackupConfig.configFile = configFile;
        BackupConfig.config = config;
    }

    public static void backupWorld(Player player, String name) {
        SchedulerUtil.runGlobal(() -> backupOnGlobal(player, name));
    }

    private static void backupOnGlobal(Player player, String name) {
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(player, "general.invalid_world_name", name);
            return;
        }

        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldManager.langConfig.sendError(player, "backup.world_not_found");
            return;
        }

        if (!WorldOperationLock.tryLock(name)) {
            WorldManager.langConfig.sendError(player, "general.operation_in_progress", name);
            return;
        }

        Bukkit.getLogger().info("Backing up world " + name);
        WorldManager.langConfig.sendWaiting(player, "backup.started");

        try {
            world.save();
        } catch (Exception e) {
            WorldOperationLock.unlock(name);
            WorldManager.getInstance().getLogger().log(Level.WARNING, "Could not save world before backup " + name, e);
            WorldManager.langConfig.sendError(player, "backup.failed");
            return;
        }

        File worldFolder = world.getWorldFolder();
        File backupFile = backupFileFor(name);
        File backupDir = backupFile.getParentFile();
        Bukkit.getLogger().info("[WorldManager] Backing up " + name + " (folder=" + worldFolder.getAbsolutePath()
                + ") to " + backupFile.getAbsolutePath());
        World.Environment environment = world.getEnvironment();
        String worldType = world.getWorldType().getName().equalsIgnoreCase("DEFAULT") ? "NORMAL" : world.getWorldType().getName();
        String generator = world.getGenerator() instanceof fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator
                ? "Empty" : WorldManager.worldsConfig.getString("worlds." + name + ".generator");

        SchedulerUtil.runAsync(() -> {
            try {
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                ZipUtil.pack(worldFolder, backupFile);
                Bukkit.getLogger().info("World " + name + " has been backed up to " + backupFile.getAbsolutePath());

                config.set("backups." + name + ".env", environment.name());
                config.set("backups." + name + ".type", worldType);
                config.set("backups." + name + ".generator", generator);
                config.save(configFile);

                WorldLifecycle.completeOnGlobal(name, () -> {
                    WorldOperationLock.unlock(name);
                    WorldManager.langConfig.sendSuccess(player, "backup.finished");
                });
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldManager] Backup failed for " + name + ": " + e.getMessage());
                WorldLifecycle.completeOnGlobal(name, () -> {
                    WorldOperationLock.unlock(name);
                    WorldManager.langConfig.sendError(player, "backup.failed");
                });
            }
        });
    }

    public static void restoreWorld(Player player, String name) {
        SchedulerUtil.runGlobal(() -> restoreOnGlobal(player, name));
    }

    private static void restoreOnGlobal(Player player, String name) {
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(player, "general.invalid_world_name", name);
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        File backupFile = backupFileFor(name);
        Bukkit.getLogger().info("[WorldManager] Looking for backup of " + name + " at " + backupFile.getAbsolutePath()
                + " (exists=" + backupFile.exists() + ")");

        if (!backupFile.exists()) {
            WorldManager.langConfig.sendError(player, "restore.world_not_found");
            return;
        }

        if (!WorldOperationLock.tryLock(name)) {
            WorldManager.langConfig.sendError(player, "general.operation_in_progress", name);
            return;
        }

        World world = Bukkit.getWorld(name);
        if (world != null && world.equals(Bukkit.getWorlds().get(0))) {
            WorldOperationLock.unlock(name);
            WorldManager.langConfig.sendError(player, "restore.failed", name);
            return;
        }
        WorldManager.langConfig.sendWaiting(player, "restore.started");
        Queue<Player> finalWorldPlayers = new ConcurrentLinkedQueue<>();
        CompletableFuture<Boolean> evacuated = world == null ? CompletableFuture.completedFuture(true)
                : WorldLifecycle.evacuate(world, finalWorldPlayers::add);
        evacuated.whenComplete((success, error) -> WorldLifecycle.completeOnGlobal(name, () -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                restoreFailed(player, name, error);
                return;
            }
            CompletableFuture<Boolean> unloaded = world == null ? CompletableFuture.completedFuture(true)
                    : WorldLifecycle.unload(world, false);
            unloaded.whenComplete((unloadSuccess, unloadError) -> WorldLifecycle.completeOnGlobal(name, () -> {
                if (unloadError != null || !Boolean.TRUE.equals(unloadSuccess)) {
                    restoreFailed(player, name, unloadError);
                    return;
                }
                restoreArchive(player, name, worldFolder, backupFile, finalWorldPlayers);
            }));
        }));
    }

    private static void restoreArchive(Player player, String name, File worldFolder, File backupFile, Queue<Player> worldPlayers) {
        SchedulerUtil.runAsync(() -> {
            try {
                String envName = config.getString("backups." + name + ".env");
                String typeName = config.getString("backups." + name + ".type");
                if (envName == null || typeName == null) {
                    WorldLifecycle.completeOnGlobal(name, () -> {
                        WorldOperationLock.unlock(name);
                        WorldManager.langConfig.sendError(player, "restore.missing_metadata", name);
                    });
                    return;
                }
                World.Environment env = World.Environment.valueOf(envName);
                WorldType type = WorldType.valueOf(typeName);
                String generator = config.getString("backups." + name + ".generator");
                if (worldFolder.exists()) org.apache.commons.io.FileUtils.deleteDirectory(worldFolder);
                ZipUtil.unpack(backupFile, worldFolder);
                WorldLifecycle.completeOnGlobal(name, () -> reloadRestoredWorld(player, name, env, type, generator, worldPlayers));
            } catch (Exception e) {
                WorldLifecycle.completeOnGlobal(name, () -> restoreFailed(player, name, e));
            }
        });
    }

    private static void reloadRestoredWorld(Player player, String name, World.Environment env, WorldType type,
                                            String generator, Queue<Player> worldPlayers) {
        try {
            WorldCreator creator = new WorldCreator(name).environment(env).type(type);
            WorldLifecycle.applyGenerator(creator, generator);
            World restoredWorld = creator.createWorld();
            if (restoredWorld == null) {
                WorldManager.langConfig.sendError(player, "restore.failed", name);
                return;
            }
            WorldManager.langConfig.sendSuccess(player, "restore.finished");
            org.bukkit.Location spawn = restoredWorld.getSpawnLocation();
            for (Player returningPlayer : worldPlayers) {
                SchedulerUtil.runEntity(returningPlayer, () -> {
                    returningPlayer.teleportAsync(spawn.clone()).whenComplete((returned, failure) -> {
                        if (failure != null || !Boolean.TRUE.equals(returned)) {
                            WorldManager.getInstance().getLogger().log(Level.WARNING,
                                    "Could not return player to restored world " + name, failure);
                        }
                    });
                });
            }
        } catch (Exception e) {
            restoreFailed(player, name, e);
        } finally {
            WorldOperationLock.unlock(name);
        }
    }

    private static void restoreFailed(Player player, String name, Throwable error) {
        WorldOperationLock.unlock(name);
        if (error != null) WorldManager.getInstance().getLogger().log(Level.WARNING, "Could not restore world " + name, error);
        WorldManager.langConfig.sendError(player, "restore.failed", name);
    }
}
