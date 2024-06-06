package fr.mathildeuh.worldmanager.manager;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.pregen.Pregen;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Bukkit.getLogger().info("Backing up world " + name);

            File worldFolder = world.getWorldFolder();
            File backupDir = new File(worldFolder.getParentFile(), "backups/WorldManager");
            File backupFile = new File(backupDir, name.toLowerCase() + ".zip");

            // Create the backup directory if it doesn't exist
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            if (backupFile.exists()) {
                backupFile.delete();
            }

            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                try {
                    ZipUtil.pack(worldFolder, backupFile);
                    Bukkit.getLogger().info("World " + name + " has been backed up to " + backupFile.getAbsolutePath());

                    // Save the backup details to the config
                    config.set("backups." + name + ".env", world.getEnvironment().name());
                    config.set("backups." + name + ".type", world.getWorldType().getName().equalsIgnoreCase("DEFAULT") ? "NORMAL" : world.getWorldType().getName());
                    config.set("backups." + name + ".generator", world.getGenerator());
                    config.save(configFile);

                    new MessageManager(player).parse(MessageManager.MessageType.SUCCESS, "World " + name + " has been backed up");
                } catch (Exception e) {
                    e.printStackTrace();
                    new MessageManager(player).parse(MessageManager.MessageType.ERROR, "Failed to backup world " + name);
                }
            });

        } else {
            new MessageManager(player).parse(MessageManager.MessageType.ERROR, "World " + name + " does not exist.");
        }
    }

    public static void restoreWorld(Player player, String name) {
        File worldFolder = new File(Bukkit.getWorldContainer(), name);

        List<Player> worldPlayers = new ArrayList<>();

        File backupFile = new File(worldFolder.getParentFile(), "backups/WorldManager/" + name.toLowerCase() + ".zip");

        if (backupFile.exists()) {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                worldPlayers = new ArrayList<>(world.getPlayers());
                for (Player players : worldPlayers) {
                    players.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
                Bukkit.unloadWorld(world, false);

            }

            if (worldFolder.exists()) {
                deleteFolder(worldFolder);
            }
            if (Pregen.generators.containsKey(name)) {
                Pregen.generators.get(name).cancel();
                Pregen.generators.remove(name);
            }

            List<Player> finalWorldPlayers = worldPlayers;
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(WorldManager.class), () -> {
                ZipUtil.unpack(backupFile, worldFolder);

                World.Environment env = World.Environment.valueOf(config.getString("backups." + name + ".env"));
                WorldType type = WorldType.valueOf(config.getString("backups." + name + ".type"));
                String generator = config.getString("backups." + name + ".generator");

                WorldCreator worldCreator = new WorldCreator(name).environment(env).type(type);
                if (generator != null && !generator.isEmpty()) {
                    worldCreator.generator(generator);
                }

                Bukkit.createWorld(worldCreator);


                Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(WorldManager.class), () -> {
                    World restoredWorld = Bukkit.getWorld(name);
                    if (restoredWorld != null) {
                        for (Player players : finalWorldPlayers) {
                            players.teleport(restoredWorld.getSpawnLocation());
                            new MessageManager(players).parse(MessageManager.MessageType.CUSTOM, "<color:#aa3e00>☠</color> <color:#ff2e1f>The world you were in has been restored.</color>");
                        }
                    }
                }, 20L * 2L);
            }, 20L * 2L);

            new MessageManager(player).parse(MessageManager.MessageType.SUCCESS, "World " + name + " has been restored.");
        } else {
            new MessageManager(player).parse(MessageManager.MessageType.ERROR, "Backup for world " + name + " does not exist.");
        }
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
