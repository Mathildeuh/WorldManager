package fr.mathildeuh.worldmanager;

import com.samjakob.spigui.SpiGUI;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.manager.WorldsConfig;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class WorldManager extends JavaPlugin {

    public static BukkitAudiences adventure;
    public static File configFile;
    public static FileConfiguration config;
    private static SpiGUI spiGUI;
    private boolean updated = true;

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
        WorldsConfig.addWorld(name, type, environement, generator);
    }

    public static void removeWorld(String name) {
        WorldsConfig.removeWorld(name);
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

        WorldsConfig.loadWorlds();

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
