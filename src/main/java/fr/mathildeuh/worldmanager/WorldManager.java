package fr.mathildeuh.worldmanager;

import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class WorldManager extends JavaPlugin {

    private File configFile;
    private FileConfiguration config;

    public static BukkitAudiences adventure;

    public static BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    @Override
    public void onEnable() {


        new Metrics(this, 22073);
        adventure = BukkitAudiences.create(this);

        getCommand("worldmanager").setExecutor(new WorldManagerCommand(this));
        getCommand("worldmanager").setTabCompleter(new WorldManagerCommand(this));

        configFile = new File(getDataFolder(), "worlds.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadWorlds();

    }

    private void loadWorlds() {
        if (!configFile.exists()) {
            getLogger().warning("Creating worlds config.");
            config.set("worlds", null);
            try {
                config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if (config.getConfigurationSection("worlds") == null) {
            getLogger().info("No worlds in configuration.");
            return;
        }

        for (String worldName : config.getConfigurationSection("worlds").getKeys(false)) {
            String type = config.getString("worlds." + worldName + ".type");
            String generator = config.getString("worlds." + worldName + ".generator");
            String environment = config.getString("worlds." + worldName + ".environment");
            createWorld(worldName, type, environment, generator);
        }
    }

    private void createWorld(String name, String type, String environement, String generator) {
        WorldCreator worldCreator = new WorldCreator(name);
        if (type != null) {
            worldCreator.environment(World.Environment.valueOf(type.toUpperCase()));
        }
        if (generator != null) {
            worldCreator.generator(generator);
        }
        World world = worldCreator.createWorld();
        if (world != null) {
            getLogger().info("Created world: " + name);
        } else {
            getLogger().warning("Can't load world: " + name);
        }
    }

    public static void addWorld(String name, String type, World.Environment environement, String generator) {
        WorldManager plugin = WorldManager.getPlugin(WorldManager.class);
        plugin.addWorld1(name, type, environement, generator);
    }

    // Méthode pour ajouter un monde au fichier de configuration
    public void addWorld1(String name, String type, World.Environment environement, String generator) {
        config.set("worlds." + name + ".type", type);
        config.set("worlds." + name + ".environment", environement.name());
        config.set("worlds." + name + ".generator", generator);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeWorld(String name) {
        WorldManager plugin = WorldManager.getPlugin(WorldManager.class);
        plugin.removeWorld1(name);
    }

    public void removeWorld1(String name) {
        config.set("worlds." + name, null);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getLogger().info("Deleted world : " + name);

    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
