package fr.mathildeuh.worldmanager;

import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.configs.BackupConfig;
import fr.mathildeuh.worldmanager.configs.LangConfig;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.guis.GUIList;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

// TODO: Permission per world

public final class WorldManager extends JavaPlugin {

    public static BukkitAudiences adventure;
    public static File configFile;
    public static FileConfiguration worldsConfig;
    public static BackupConfig backupConfig;
    public static LangConfig langConfig;
    private boolean updated = true;


    public static BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    public static void addWorld(CommandSender player, String name, String type, World.Environment environement, String generator) {
        WorldsConfig.addWorld(player, name, type, environement, generator);
    }

    public static void removeWorld(String name) {
        WorldsConfig.removeWorld(name);
    }

    @Override
    public void onEnable() {


        saveDefaultConfig();
        loadLangFile();

        new Metrics(this, 22073);
        adventure = BukkitAudiences.create(this);

        getCommand("worldmanager").setExecutor(new WorldManagerCommand(this));
        getCommand("worldmanager").setTabCompleter(new WorldManagerCommand(this));

        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        loadBackupFile();
        loadWorldsFile();

        if (getConfig().getBoolean("update-checker"))
            update();

        for (GUIList gui : GUIList.values()) {
            gui.update();
        }
    }

    private void loadLangFile() {
        String lang = getConfig().getString("lang");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");

        List<String> defaultLangs = Arrays.asList("en", "es", "fr", "ru", "de");

        langFile.getParentFile().mkdirs();

        if (!langFile.exists()) {
            if (this.getResource("lang/" + lang + ".yml") != null) {
                saveResource("lang/" + lang + ".yml", false);
            } else {
                String defaultLang = "en";
                for (String defLang : defaultLangs) {
                    assert lang != null;
                    if (lang.equals(defLang)) {
                        defaultLang = defLang;
                        break;
                    }
                }
                File defaultLangFile = new File(getDataFolder(), "lang/" + defaultLang + ".yml");
                if (!defaultLangFile.exists()) {
                    saveResource("lang/" + defaultLang + ".yml", false);
                }
                try {
                    Files.copy(defaultLangFile.toPath(), langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Charger le fichier de langue spécifié
        langConfig = new LangConfig(langFile);
    }

    private void loadWorldsFile() {
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
    }

    private void loadBackupFile() {
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
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }

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

    public boolean isUpdated() {
        return this.updated;
    }

}
