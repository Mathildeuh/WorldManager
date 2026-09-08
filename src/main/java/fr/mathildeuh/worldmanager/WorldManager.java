package fr.mathildeuh.worldmanager;

import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.configs.BackupConfig;
import fr.mathildeuh.worldmanager.configs.LangConfig;
import fr.mathildeuh.worldmanager.configs.LinkedWorldsManager;
import fr.mathildeuh.worldmanager.configs.PlayerInventoryManager;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.database.DatabaseConnection;
import fr.mathildeuh.worldmanager.database.DatabaseFactory;
import fr.mathildeuh.worldmanager.database.DatabaseManager;
import fr.mathildeuh.worldmanager.events.JoinListener;
import fr.mathildeuh.worldmanager.events.WorldChangeListener;
import fr.mathildeuh.worldmanager.guis.GuiListener;
import fr.mathildeuh.worldmanager.placeholder.Placeholders;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
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

    // Fichiers de configuration séparés
    public static File configFile;
    public static FileConfiguration worldsConfig;

    // Database
    private static DatabaseConnection databaseConnection;
    private static DatabaseManager databaseManager;

    public static BackupConfig backupConfig;
    public static LangConfig langConfig;
    private boolean updated = true;

    private static WorldManager instance;

    public static WorldManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Tried to access WorldManager instance before the plugin was enabled!");
        }
        return instance;
    }

    public static void addWorld(CommandSender player, String name, String type, World.Environment environement, String generator) {
        WorldsConfig.addWorld(player, name, type, environement, generator);
    }

    public static void removeWorld(String name) {
        WorldsConfig.removeWorld(name);
    }

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();
        if (setupPlaceholderAPI()) {
            getLogger().info("PlaceholderAPI is enabled.");
            new Placeholders().register();
        } else {
            getLogger().warning("PlaceholderAPI not found so PlaceholderAPI features will not work.");
        }
        loadLangFile();

        new Metrics(this, 22073);

        var worldManagerCommand = getCommand("worldmanager");
        if (worldManagerCommand != null) {
            worldManagerCommand.setExecutor(new WorldManagerCommand());
            worldManagerCommand.setTabCompleter(new WorldManagerCommand());
        } else {
            getLogger().severe("Could not register 'worldmanager' command! Check plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);
        getServer().getPluginManager().registerEvents(new fr.mathildeuh.worldmanager.events.PortalLinkListener(), this);
        getServer().getPluginManager().registerEvents(new fr.mathildeuh.worldmanager.events.WorldFlagsListener(), this);
        getServer().getPluginManager().registerEvents(new fr.mathildeuh.worldmanager.events.SignPortalListener(), this);
        getServer().getPluginManager().registerEvents(new fr.mathildeuh.worldmanager.events.CustomPortalListener(), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        fr.mathildeuh.worldmanager.guis.GuiAnimator.start();

        loadBackupFile();
        loadWorldsFile();

        // Initialize database
        initializeDatabase();

        LinkedWorldsManager.loadLinkedWorlds();
        fr.mathildeuh.worldmanager.configs.LinkedPortalsManager.load();
        fr.mathildeuh.worldmanager.configs.SignPortalsManager.load();
        fr.mathildeuh.worldmanager.configs.CustomPortalsManager.load();

        if (fr.mathildeuh.worldmanager.util.EconomyHook.setup()) {
            getLogger().info("Vault economy hook enabled - custom portals can charge a price.");
        }

        if (getConfig().getBoolean("update-checker"))
            update();
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

        List<String> defaultLangs = Arrays.asList("en", "es", "fr", "ru", "de", "pl");

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

        mergeMissingLangKeys(lang, langFile);

        // Charger le fichier de langue spécifié
        langConfig = new LangConfig(langFile);
    }

    /**
     * An on-disk lang file only gets created once, the very first time the plugin ever runs -
     * after that it's the server owner's file, and this plugin has no scripted launch task that
     * would ever recreate it. Without this, upgrading to a version that adds new lang keys (as
     * 4.0.0 did, replacing the whole {@code dialog.*} namespace with {@code gui.*}) leaves every
     * existing install logging "Missing GUI message key" forever, since the bundled resource is
     * never consulted again once the file exists. This adds only the keys the on-disk file is
     * missing, from the jar's bundled default for that locale - any key the server owner already
     * customized is left untouched.
     */
    private void mergeMissingLangKeys(String lang, File langFile) {
        try (var bundledStream = getResource("lang/" + lang + ".yml")) {
            if (bundledStream == null) {
                return;
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(bundledStream, java.nio.charset.StandardCharsets.UTF_8));
            YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(langFile);

            boolean changed = false;
            for (String key : bundled.getKeys(true)) {
                if (bundled.isConfigurationSection(key) || onDisk.contains(key)) {
                    continue;
                }
                onDisk.set(key, bundled.get(key));
                changed = true;
            }

            if (changed) {
                onDisk.save(langFile);
                getLogger().info("Added new translation keys to lang/" + lang + ".yml (introduced in this version).");
            }
        } catch (IOException e) {
            getLogger().warning("Failed to merge new keys into lang/" + lang + ".yml: " + e.getMessage());
        }
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

    private void initializeDatabase() {
        try {
            databaseConnection = DatabaseFactory.createConnection(this);
            databaseManager = new DatabaseManager(databaseConnection);
            databaseManager.initialize();
            PlayerInventoryManager.setDatabaseManager(databaseManager);
            getLogger().info("[WorldManager] Database initialized successfully");
        } catch (Exception e) {
            getLogger().severe("[WorldManager] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        fr.mathildeuh.worldmanager.guis.GuiAnimator.stop();

        // Clean up player inventory data
        PlayerInventoryManager.clearAllData();

        // Close database connection
        if (databaseConnection != null) {
            try {
                databaseConnection.closeConnection();
                getLogger().info("[WorldManager] Database connection closed");
            } catch (Exception e) {
                getLogger().warning("[WorldManager] Error closing database: " + e.getMessage());
            }
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
