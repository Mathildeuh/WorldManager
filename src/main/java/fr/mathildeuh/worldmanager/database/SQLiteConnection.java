package fr.mathildeuh.worldmanager.database;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database connection manager
 */
public class SQLiteConnection extends DatabaseConnection {

    private final File databaseFile;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    public SQLiteConnection(File dataFolder, String databaseName) {
        super(databaseName);
        this.databaseFile = new File(dataFolder, databaseName + ".db");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    @Override
    public void closeConnection() {
        // SQLite doesn't need explicit connection pool closing
    }

    @Override
    public void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create player_inventories table
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS player_inventories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        group_name TEXT NOT NULL,
                        inventory_data BLOB NOT NULL,
                        held_item_slot INTEGER NOT NULL DEFAULT 0,
                        health REAL NOT NULL,
                        food_level INTEGER NOT NULL,
                        saturation REAL NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(player_uuid, group_name)
                    )
                    """;

            stmt.execute(createTableSQL);
            Bukkit.getLogger().info("[WorldManager] SQLite table 'player_inventories' created/verified");

            // Migration for databases created before held_item_slot existed (SQLite 3.35+ supports
            // IF NOT EXISTS on ADD COLUMN, matching the bundled driver's bundled SQLite version).
            try {
                stmt.execute("ALTER TABLE player_inventories ADD COLUMN IF NOT EXISTS held_item_slot INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already present on a pre-migration driver that doesn't support IF NOT EXISTS.
            }

            // Create index for faster lookups
            String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_player_group ON player_inventories(player_uuid, group_name)";
            stmt.execute(createIndexSQL);

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[WorldManager] Failed to create SQLite tables: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getDatabaseType() {
        return "SQLite";
    }

    /**
     * Get the database file path
     */
    public String getDatabasePath() {
        return databaseFile.getAbsolutePath();
    }
}

