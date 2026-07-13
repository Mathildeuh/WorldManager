package fr.mathildeuh.worldmanager.database;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL/MariaDB database connection manager
 */
public class MySQLConnection extends DatabaseConnection {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver not found: " + e.getMessage());
        }
    }

    public MySQLConnection(String databaseName, String host, int port, String username, String password) {
        super(databaseName);
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                host, port, databaseName);

        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public void closeConnection() {
        // MySQL connection pool will close on plugin disable
    }

    @Override
    public void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create player_inventories table
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS player_inventories (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        group_name VARCHAR(50) NOT NULL,
                        inventory_data LONGBLOB NOT NULL,
                        health FLOAT NOT NULL,
                        food_level INT NOT NULL,
                        saturation FLOAT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_player_group (player_uuid, group_name),
                        INDEX idx_player_group (player_uuid, group_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;

            stmt.execute(createTableSQL);
            Bukkit.getLogger().info("[WorldManager] MySQL table 'player_inventories' created/verified");

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[WorldManager] Failed to create MySQL tables: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getDatabaseType() {
        return "MySQL/MariaDB";
    }

    /**
     * Test the connection
     */
    public void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            Bukkit.getLogger().info("[WorldManager] MySQL connection successful!");
        }
    }
}

