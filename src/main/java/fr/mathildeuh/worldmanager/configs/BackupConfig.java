package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
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

public class BackupConfig {

    public static File configFile;
    public static FileConfiguration config;

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
        File backupDir = new File(worldFolder.getParentFile(), "backups/WorldManager");
        File backupFile = new File(backupDir, name.toLowerCase() + ".zip");
        World.Environment environment = world.getEnvironment();
        String worldType = world.getWorldType().getName().equalsIgnoreCase("DEFAULT") ? "NORMAL" : world.getWorldType().getName();
        org.bukkit.generator.ChunkGenerator generator = world.getGenerator();

        Bukkit.getScheduler().runTaskAsynchronously(WorldManager.getInstance(), () -> {
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

                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () ->
                        WorldManager.langConfig.sendSuccess(player, "backup.finished"));
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldManager] Backup failed for " + name + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () ->
                        WorldManager.langConfig.sendError(player, "backup.failed"));
            }
        });
    }

    public static void restoreWorld(Player player, String name) {
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(player, "general.invalid_world_name", name);
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        File backupFile = new File(worldFolder.getParentFile(), "backups/WorldManager/" + name.toLowerCase() + ".zip");

        if (!backupFile.exists()) {
            WorldManager.langConfig.sendError(player, "restore.world_not_found");
            return;
        }

        List<Player> worldPlayers = new ArrayList<>();
        World world = Bukkit.getWorld(name);
        if (world != null) {
            worldPlayers = new ArrayList<>(world.getPlayers());
            for (Player p : worldPlayers) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Bukkit.unloadWorld(world, false);
        }

        WorldManager.langConfig.sendWaiting(player, "restore.started");

        List<Player> finalWorldPlayers = worldPlayers;
        Bukkit.getScheduler().runTaskAsynchronously(WorldManager.getInstance(), () -> {
            try {
                if (worldFolder.exists()) {
                    deleteFolder(worldFolder);
                }
                ZipUtil.unpack(backupFile, worldFolder);

                World.Environment env = World.Environment.valueOf(config.getString("backups." + name + ".env"));
                WorldType type = WorldType.valueOf(config.getString("backups." + name + ".type"));
                String generator = config.getString("backups." + name + ".generator");

                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () -> {
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

                        Bukkit.getScheduler().runTaskLater(WorldManager.getInstance(), () -> {
                            for (Player p : finalWorldPlayers) {
                                if (p.isOnline()) {
                                    p.teleport(restoredWorld.getSpawnLocation());
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
                Bukkit.getScheduler().runTask(WorldManager.getInstance(), () ->
                        WorldManager.langConfig.sendError(player, "restore.failed", name));
            }
        });
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
