package fr.mathildeuh.worldmanager.database;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages database operations for player inventory storage and retrieval
 */
public class DatabaseManager {

    private final DatabaseConnection dbConnection;

    public DatabaseManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Initialize database tables
     */
    public void initialize() {
        try {
            dbConnection.createTables();
            Bukkit.getLogger().info("[WorldManager] Database initialized successfully (" + dbConnection.getDatabaseType() + ")");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[WorldManager] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save player inventory to database (SYNCHRONOUS - ensures data is saved immediately)
     */
    public void saveInventory(UUID playerUUID, String groupName, ItemStack[] mainInventory,
                             ItemStack[] armorContents, ItemStack offHandItem,
                             float health, int foodLevel, float saturation) {
        try {
            byte[] inventoryData = serializeInventory(mainInventory, armorContents, offHandItem);

            // Use appropriate SQL syntax based on database type
            String sql;
            if (dbConnection.getDatabaseType().equals("SQLite")) {
                sql = """
                        INSERT OR REPLACE INTO player_inventories (player_uuid, group_name, inventory_data, health, food_level, saturation)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
            } else {
                // MySQL/MariaDB
                sql = """
                        INSERT INTO player_inventories (player_uuid, group_name, inventory_data, health, food_level, saturation)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE 
                            inventory_data = VALUES(inventory_data),
                            health = VALUES(health),
                            food_level = VALUES(food_level),
                            saturation = VALUES(saturation),
                            updated_at = CURRENT_TIMESTAMP
                        """;
            }

            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, groupName);
                pstmt.setBytes(3, inventoryData);
                pstmt.setFloat(4, health);
                pstmt.setInt(5, foodLevel);
                pstmt.setFloat(6, saturation);

                pstmt.executeUpdate();
                Bukkit.getLogger().fine("[WorldManager] Inventory saved for " + playerUUID + " in group " + groupName);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save inventory for " + playerUUID + ": " + e.getMessage());
        }
    }

    /**
     * Load player inventory from database
     */
    public InventoryData loadInventory(UUID playerUUID, String groupName) {
        String sql = "SELECT inventory_data, health, food_level, saturation FROM player_inventories WHERE player_uuid = ? AND group_name = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, groupName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] inventoryData = rs.getBytes("inventory_data");
                    float health = rs.getFloat("health");
                    int foodLevel = rs.getInt("food_level");
                    float saturation = rs.getFloat("saturation");

                    return deserializeInventory(inventoryData, health, foodLevel, saturation);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to load inventory for " + playerUUID + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete player inventory from database
     */
    public void deleteInventory(UUID playerUUID, String groupName) {
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("WorldManager"),
                () -> {
                    String sql = "DELETE FROM player_inventories WHERE player_uuid = ? AND group_name = ?";

                    try (Connection conn = dbConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {

                        pstmt.setString(1, playerUUID.toString());
                        pstmt.setString(2, groupName);
                        pstmt.executeUpdate();

                    } catch (SQLException e) {
                        Bukkit.getLogger().warning("[WorldManager] Failed to delete inventory for " + playerUUID + ": " + e.getMessage());
                    }
                });
    }

    /**
     * Delete all player inventories
     */
    public void deleteAllPlayerInventories(UUID playerUUID) {
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("WorldManager"),
                () -> {
                    String sql = "DELETE FROM player_inventories WHERE player_uuid = ?";

                    try (Connection conn = dbConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {

                        pstmt.setString(1, playerUUID.toString());
                        pstmt.executeUpdate();

                    } catch (SQLException e) {
                        Bukkit.getLogger().warning("[WorldManager] Failed to delete inventories for " + playerUUID + ": " + e.getMessage());
                    }
                });
    }

    /**
     * Serialize inventory to bytes using YAML (Bukkit native format)
     */
    private byte[] serializeInventory(ItemStack[] mainInventory, ItemStack[] armorContents, ItemStack offHandItem) throws SQLException {
        try {
            YamlConfiguration yaml = new YamlConfiguration();

            // Serialize main inventory
            for (int i = 0; i < mainInventory.length; i++) {
                if (mainInventory[i] != null) {
                    yaml.set("main." + i, mainInventory[i]);
                }
            }

            // Serialize armor
            for (int i = 0; i < armorContents.length; i++) {
                if (armorContents[i] != null) {
                    yaml.set("armor." + i, armorContents[i]);
                }
            }

            // Serialize offhand
            if (offHandItem != null) {
                yaml.set("offhand", offHandItem);
            }

            // Convert to bytes
            String yamlString = yaml.saveToString();
            return yamlString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("Failed to serialize inventory", e);
        }
    }

    /**
     * Deserialize inventory from bytes using YAML
     */
    private InventoryData deserializeInventory(byte[] data, float health, int foodLevel, float saturation) throws SQLException {
        try {
            String yamlString = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(yamlString);

            // Deserialize main inventory
            ItemStack[] mainInventory = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                if (yaml.contains("main." + i)) {
                    mainInventory[i] = yaml.getItemStack("main." + i);
                }
            }

            // Deserialize armor
            ItemStack[] armorContents = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                if (yaml.contains("armor." + i)) {
                    armorContents[i] = yaml.getItemStack("armor." + i);
                }
            }

            // Deserialize offhand
            ItemStack offHandItem = null;
            if (yaml.contains("offhand")) {
                offHandItem = yaml.getItemStack("offhand");
            }

            return new InventoryData(mainInventory, armorContents, offHandItem, health, foodLevel, saturation);
        } catch (Exception e) {
            throw new SQLException("Failed to deserialize inventory", e);
        }
    }

    /**
     * Inner class to store inventory data
     */
    public static class InventoryData {
        public final ItemStack[] mainInventory;
        public final ItemStack[] armorContents;
        public final ItemStack offHandItem;
        public final float health;
        public final int foodLevel;
        public final float saturation;

        public InventoryData(ItemStack[] mainInventory, ItemStack[] armorContents, ItemStack offHandItem,
                           float health, int foodLevel, float saturation) {
            this.mainInventory = mainInventory;
            this.armorContents = armorContents;
            this.offHandItem = offHandItem;
            this.health = health;
            this.foodLevel = foodLevel;
            this.saturation = saturation;
        }
    }
}

