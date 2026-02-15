package fr.mathildeuh.worldmanager.database;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Factory to create database connections based on configuration
 */
public class DatabaseFactory {

    /**
     * Create a database connection from plugin configuration
     */
    public static DatabaseConnection createConnection(JavaPlugin plugin) {
        Configuration config = plugin.getConfig();
        String databaseType = config.getString("database.type", "sqlite").toLowerCase();

        if ("mysql".equals(databaseType)) {
            return createMySQLConnection(config);
        } else {
            return createSQLiteConnection(plugin);
        }
    }

    /**
     * Create SQLite connection
     */
    private static DatabaseConnection createSQLiteConnection(JavaPlugin plugin) {
        Configuration config = plugin.getConfig();
        String filename = config.getString("database.sqlite.filename", "inventories");
        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        Bukkit.getLogger().info("[WorldManager] Using SQLite database: " + filename + ".db");
        return new SQLiteConnection(dataFolder, filename);
    }

    /**
     * Create MySQL connection
     */
    private static DatabaseConnection createMySQLConnection(Configuration config) {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "worldmanager");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");

        Bukkit.getLogger().info("[WorldManager] Using MySQL/MariaDB database at " + host + ":" + port);
        return new MySQLConnection(database, host, port, username, password);
    }
}

