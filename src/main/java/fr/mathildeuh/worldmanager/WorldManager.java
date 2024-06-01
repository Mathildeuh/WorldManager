package fr.mathildeuh.worldmanager;

import com.samjakob.spigui.SpiGUI;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class WorldManager extends JavaPlugin {

    public static BukkitAudiences adventure;
    private static SpiGUI spiGUI;
    private File configFile;
    private FileConfiguration config;
    private boolean updated = true;
    private static String newVersion;

    public static SpiGUI getSpiGUI() {
        return spiGUI;
    }

    public static BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    public static void addWorld(String name, String type, World.Environment environement, String generator) {
        WorldManager plugin = WorldManager.getPlugin(WorldManager.class);
        plugin.addWorld1(name, type, environement, generator);
    }

    public static void removeWorld(String name) {
        WorldManager plugin = WorldManager.getPlugin(WorldManager.class);
        plugin.removeWorld1(name);
    }

    @Override
    public void onEnable() {



        spiGUI = new SpiGUI(this);

        new Metrics(this, 22073);
        adventure = BukkitAudiences.create(this);

        getCommand("worldmanager").setExecutor(new WorldManagerCommand(this));
        getCommand("worldmanager").setTabCompleter(new WorldManagerCommand(this));

        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        configFile = new File(getDataFolder(), "worlds.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        loadWorlds();

        update();


    }

    private void update() {
        new UpdateChecker(this, 117043).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version.replace("V", ""))) {
                updated = true;
                getLogger().info("No update available.");
            } else {
                updated = false;
                System.out.println("An update is available for WorldManager.");
                System.out.println("New version: " + version);
                System.out.println("Download it here: " + UpdateChecker.RESOURCE_URL);
            }
        });
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

    private void createWorld(String name, String type, String environment, String generator) {
        WorldCreator worldCreator = new WorldCreator(name);

        if (environment != null) {
            try {
                World.Environment env = World.Environment.valueOf(environment.toUpperCase(Locale.ROOT));
                worldCreator.environment(env);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid environment: " + environment);
                return;
            }
        }

        if (type != null) {
            try {
                WorldType worldType = WorldType.valueOf(type.toUpperCase(Locale.ROOT));
                worldCreator.type(worldType);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid world type: " + type);
                return;
            }
        }

        if (generator != null) {
            worldCreator.generator(generator);
        }

        World world = worldCreator.createWorld();
        if (world != null) {
            getLogger().info("Loaded world: " + name);
        } else {
            getLogger().warning("Can't load world: " + name);
        }
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

    public boolean isUpdated() {
        return this.updated;
    }

}
