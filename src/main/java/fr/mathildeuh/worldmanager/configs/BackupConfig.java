package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(player, "general.invalid_world_name", name);
            return;
        }

        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldManager.langConfig.sendError(player, "backup.world_not_found");
            return;
        }

        Bukkit.getLogger().info("Backing up world " + name);
        WorldManager.langConfig.sendWaiting(player, "backup.started");

        world.save();

        File worldFolder = world.getWorldFolder();
        File backupFile = backupFileFor(name);
        File backupDir = backupFile.getParentFile();
        Bukkit.getLogger().info("[WorldManager] Backing up " + name + " (folder=" + worldFolder.getAbsolutePath()
                + ") to " + backupFile.getAbsolutePath());
        World.Environment environment = world.getEnvironment();
        String worldType = world.getWorldType().getName().equalsIgnoreCase("DEFAULT") ? "NORMAL" : world.getWorldType().getName();
        org.bukkit.generator.ChunkGenerator generator = world.getGenerator();

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

                SchedulerUtil.runGlobal(() -> WorldManager.langConfig.sendSuccess(player, "backup.finished"));
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldManager] Backup failed for " + name + ": " + e.getMessage());
                SchedulerUtil.runGlobal(() -> WorldManager.langConfig.sendError(player, "backup.failed"));
            }
        });
    }

    public static void restoreWorld(Player player, String name) {
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

        List<Player> worldPlayers = new ArrayList<>();
        World world = Bukkit.getWorld(name);
        if (world != null) {
            worldPlayers = new ArrayList<>(world.getPlayers());
        }

        WorldManager.langConfig.sendWaiting(player, "restore.started");

        List<Player> finalWorldPlayers = worldPlayers;
        List<CompletableFuture<Boolean>> teleports = finalWorldPlayers.stream()
                .map(p -> p.teleportAsync(Bukkit.getWorlds().get(0).getSpawnLocation()))
                .toList();

        CompletableFuture.allOf(teleports.toArray(new CompletableFuture[0])).thenRun(() -> SchedulerUtil.runGlobal(() -> {
            World worldToUnload = Bukkit.getWorld(name);
            if (worldToUnload != null) {
                Bukkit.unloadWorld(worldToUnload, false);
            }

            SchedulerUtil.runAsync(() -> {
                try {
                    if (worldFolder.exists()) {
                        deleteFolder(worldFolder);
                    }
                    ZipUtil.unpack(backupFile, worldFolder);

                    World.Environment env = World.Environment.valueOf(config.getString("backups." + name + ".env"));
                    WorldType type = WorldType.valueOf(config.getString("backups." + name + ".type"));
                    String generator = config.getString("backups." + name + ".generator");

                    SchedulerUtil.runGlobal(() -> {
                        try {
                            WorldCreator worldCreator = new WorldCreator(name).environment(env).type(type);
                            if (generator != null && !generator.isEmpty()) {
                                worldCreator.generator(generator);
                            }

                            World restoredWorld = Bukkit.createWorld(worldCreator);
                            if (restoredWorld == null) {
                                WorldManager.langConfig.sendError(player, "restore.failed", name);
                                return;
                            }

                            WorldManager.langConfig.sendSuccess(player, "restore.finished");

                            SchedulerUtil.runGlobalDelayed(() -> {
                                for (Player p : finalWorldPlayers) {
                                    if (p.isOnline()) {
                                        p.teleportAsync(restoredWorld.getSpawnLocation());
                                    }
                                }
                            }, 20L * 2L);
                        } catch (Exception e) {
                            Bukkit.getLogger().warning("[WorldManager] Restore failed for " + name + ": " + e.getMessage());
                            WorldManager.langConfig.sendError(player, "restore.failed", name);
                        }
                    });
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[WorldManager] Restore failed for " + name + ": " + e.getMessage());
                    SchedulerUtil.runGlobal(() -> WorldManager.langConfig.sendError(player, "restore.failed", name));
                }
            });
        }));
    }


    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}
