package fr.mathildeuh.worldmanager;

import com.samjakob.spigui.SpiGUI;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.manager.BackupConfig;
import fr.mathildeuh.worldmanager.manager.WorldsConfig;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class WorldManager extends JavaPlugin {

    public static BukkitAudiences adventure;
    public static File configFile;
    public static FileConfiguration worldsConfig;
    public static BackupConfig backupConfig;
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

        saveDefaultConfig();

        new Metrics(this, 22073);
        adventure = BukkitAudiences.create(this);

        getCommand("worldmanager").setExecutor(new WorldManagerCommand(this));
        getCommand("worldmanager").setTabCompleter(new WorldManagerCommand(this));

        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        configFile = new File("backups/WorldManager/backups.yml");
        worldsConfig = YamlConfiguration.loadConfiguration(configFile);
        try {
            if (worldsConfig.getString("backups") == null)
                worldsConfig.set("backups", "");
            worldsConfig.save(configFile);
            backupConfig = new BackupConfig(configFile, worldsConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        configFile = new File(getDataFolder(), "worlds.yml");
        worldsConfig = YamlConfiguration.loadConfiguration(configFile);
        try {
            if (worldsConfig.getString("worlds") == null)
                worldsConfig.set("worlds", "");
            worldsConfig.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorldsConfig.loadWorlds();

        if (getConfig().getBoolean("update-checker"))
            update();


    }

    private void update() {
        new UpdateChecker(this, 117043).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version.replace("V", ""))) {
                updated = true;
            } else {
                updated = false;
                Bukkit.getLogger().log(Level.INFO, "An update is available for WorldManager.");
                Bukkit.getLogger().log(Level.INFO, "New version: " + version.replace("V", "") + " | Current version: " + this.getDescription().getVersion());
                Bukkit.getLogger().log(Level.INFO, "Download it here: " + UpdateChecker.RESOURCE_URL);
            }
        });
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }

    }

    public boolean isUpdated() {
        return this.updated;
    }

}
