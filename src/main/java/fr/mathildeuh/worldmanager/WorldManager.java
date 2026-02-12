package fr.mathildeuh.worldmanager;

import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.configs.BackupConfig;
import fr.mathildeuh.worldmanager.configs.LangConfig;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.guis.GUIList;
import fr.mathildeuh.worldmanager.placeholder.Placeholders;
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

    // Fichiers de configuration séparés
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
        if (setupPlaceholderAPI()) {
            getLogger().info("PlaceholderAPI is enabled.");
            new Placeholders().register();
        } else {
            getLogger().warning("PlaceholderAPI not found so PlaceholderAPI features will not work.");
        }
        loadLangFile();

        new Metrics(this, 22073);
        adventure = BukkitAudiences.create(this);

        var worldManagerCommand = getCommand("worldmanager");
        if (worldManagerCommand != null) {
            worldManagerCommand.setExecutor(new WorldManagerCommand());
            worldManagerCommand.setTabCompleter(new WorldManagerCommand());
        } else {
            getLogger().severe("Could not register 'worldmanager' command! Check plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        loadBackupFile();
        loadWorldsFile();

        if (getConfig().getBoolean("update-checker"))
            update();

        for (GUIList gui : GUIList.values()) {
            gui.update();
        }
    }

    private boolean setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void loadLangFile() {
        String lang = getConfig().getString("lang");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");

        List<String> defaultLangs = Arrays.asList("en", "es", "fr", "ru", "de");

        if (!langFile.getParentFile().mkdirs() && !langFile.getParentFile().exists()) {
            getLogger().warning("Could not create lang directory!");
        }

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
                    getLogger().severe("Failed to copy language file: " + e.getMessage());
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
        File backupConfigFile = new File("backups/WorldManager/backups.yml");
        FileConfiguration backupConfigYaml = YamlConfiguration.loadConfiguration(backupConfigFile);
        try {
            if (backupConfigYaml.getString("backups") == null)
                backupConfigYaml.set("backups", "");
            backupConfigYaml.save(backupConfigFile);
            @SuppressWarnings("InstantiationOfUtilityClass")
            BackupConfig tempConfig = new BackupConfig(backupConfigFile, backupConfigYaml);
            backupConfig = tempConfig;
        } catch (IOException e) {
            getLogger().severe("Failed to load backup configuration: " + e.getMessage());
            throw new RuntimeException("Failed to initialize backup config", e);
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
