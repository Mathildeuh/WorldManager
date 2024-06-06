package fr.mathildeuh.worldmanager.manager;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Locale;

public class WorldsConfig {


    public static void loadWorlds() {
        if (!WorldManager.configFile.exists()) {
            WorldManager.worldsConfig.set("worlds", null);
            try {
                WorldManager.worldsConfig.save(WorldManager.configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        ConfigurationSection worlds = WorldManager.worldsConfig.getConfigurationSection("worlds");

        if (worlds == null) {
            return;
        }

        for (String worldName : worlds.getKeys(false)) {
            String type = WorldManager.worldsConfig.getString("worlds." + worldName + ".type");
            String generator = WorldManager.worldsConfig.getString("worlds." + worldName + ".generator");
            String environment = WorldManager.worldsConfig.getString("worlds." + worldName + ".environment");
            createWorld(worldName, type, environment, generator);
        }
    }

    private static void createWorld(String name, String type, String environment, String generator) {
        WorldCreator worldCreator = new WorldCreator(name);

        if (environment != null) {
            try {
                World.Environment env = World.Environment.valueOf(environment.toUpperCase(Locale.ROOT));
                worldCreator.environment(env);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid environment: " + environment);
                return;
            }
        }

        if (type != null) {
            try {
                WorldType worldType = WorldType.valueOf(type.toUpperCase(Locale.ROOT));
                worldCreator.type(worldType);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid world type: " + type);
                return;
            }
        }

        if (generator != null) {
            worldCreator.generator(generator);
        }
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), worldCreator::createWorld);
    }

    // Méthode pour ajouter un monde au fichier de configuration
    public static void addWorld(String name, String type, World.Environment environement, String generator) {
        WorldManager.worldsConfig.set("worlds." + name + ".type", type);
        WorldManager.worldsConfig.set("worlds." + name + ".environment", environement.name());
        WorldManager.worldsConfig.set("worlds." + name + ".generator", generator);
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeWorld(String name) {
        WorldManager.worldsConfig.set("worlds." + name, null);
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
