package fr.mathildeuh.worldmanager.database;

import fr.mathildeuh.worldmanager.configs.ProfileType;
import fr.mathildeuh.worldmanager.configs.PlayerInventoryManager;
import fr.mathildeuh.worldmanager.configs.SavedLocation;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages database operations for per-group player profile storage and retrieval (see
 * {@link PlayerInventoryManager}). The {@code health}/{@code food_level}/{@code saturation}/
 * {@code held_item_slot} SQL columns predate the per-aspect "share" system and are kept as-is to
 * avoid another schema migration; every other aspect (inventory contents, level/exp, bed spawn,
 * last location, and the authoritative {@code captured} set that says which aspects a given row
 * actually holds real data for) lives in the existing {@code inventory_data} YAML blob, which was
 * already schema-free enough to grow without a migration.
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

    /** Called off the main thread (see {@code PlayerInventoryManager}); {@code data} is an already-cloned snapshot. */
    public void saveProfile(UUID playerUUID, String groupName, PlayerInventoryManager.ProfileData data) {
        try {
            byte[] blob = serialize(data);

            String sql;
            if (dbConnection.getDatabaseType().equals("SQLite")) {
                sql = """
                        INSERT OR REPLACE INTO player_inventories (player_uuid, group_name, inventory_data, held_item_slot, health, food_level, saturation)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;
            } else {
                // MySQL/MariaDB
                sql = """
                        INSERT INTO player_inventories (player_uuid, group_name, inventory_data, held_item_slot, health, food_level, saturation)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            inventory_data = VALUES(inventory_data),
                            held_item_slot = VALUES(held_item_slot),
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
                pstmt.setBytes(3, blob);
                pstmt.setInt(4, data.heldItemSlot);
                pstmt.setFloat(5, data.health);
                pstmt.setInt(6, data.foodLevel);
                pstmt.setFloat(7, data.saturation);

                pstmt.executeUpdate();
                Bukkit.getLogger().fine("[WorldManager] Profile saved for " + playerUUID + " in group " + groupName);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save profile for " + playerUUID + ": " + e.getMessage());
        }
    }

    /** Loads a group's stored profile, or {@code null} if nothing is stored for it yet. */
    public PlayerInventoryManager.ProfileData loadProfile(UUID playerUUID, String groupName) {
        String sql = "SELECT inventory_data, held_item_slot, health, food_level, saturation FROM player_inventories WHERE player_uuid = ? AND group_name = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, groupName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] blob = rs.getBytes("inventory_data");
                    int heldItemSlot = rs.getInt("held_item_slot");
                    float health = rs.getFloat("health");
                    int foodLevel = rs.getInt("food_level");
                    float saturation = rs.getFloat("saturation");
                    return deserialize(blob, heldItemSlot, health, foodLevel, saturation);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to load profile for " + playerUUID + ": " + e.getMessage());
        }

        return null;
    }

    /** Delete one group's stored profile for a player. */
    public void deleteProfile(UUID playerUUID, String groupName) {
        SchedulerUtil.runAsync(() -> {
                    String sql = "DELETE FROM player_inventories WHERE player_uuid = ? AND group_name = ?";

                    try (Connection conn = dbConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {

                        pstmt.setString(1, playerUUID.toString());
                        pstmt.setString(2, groupName);
                        pstmt.executeUpdate();

                    } catch (SQLException e) {
                        Bukkit.getLogger().warning("[WorldManager] Failed to delete profile for " + playerUUID + ": " + e.getMessage());
                    }
                });
    }

    private byte[] serialize(PlayerInventoryManager.ProfileData data) throws SQLException {
        try {
            YamlConfiguration yaml = new YamlConfiguration();

            List<String> capturedKeys = new ArrayList<>();
            for (ProfileType type : data.captured) {
                capturedKeys.add(type.configKey());
            }
            yaml.set("captured", capturedKeys);

            if (data.captured.contains(ProfileType.INVENTORY)) {
                for (int i = 0; i < data.mainInventory.length; i++) {
                    if (data.mainInventory[i] != null) {
                        yaml.set("main." + i, data.mainInventory[i]);
                    }
                }
                for (int i = 0; i < data.armorContents.length; i++) {
                    if (data.armorContents[i] != null) {
                        yaml.set("armor." + i, data.armorContents[i]);
                    }
                }
                if (data.offHandItem != null) {
                    yaml.set("offhand", data.offHandItem);
                }
            }

            if (data.captured.contains(ProfileType.EXPERIENCE)) {
                yaml.set("level", data.level);
                yaml.set("exp", (double) data.exp);
            }

            if (data.captured.contains(ProfileType.BED_SPAWN) && data.bedSpawn != null) {
                setLocation(yaml, "bedSpawn", data.bedSpawn);
            }

            if (data.captured.contains(ProfileType.LOCATION) && data.lastLocation != null) {
                setLocation(yaml, "lastLocation", data.lastLocation);
            }

            return yaml.saveToString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("Failed to serialize profile", e);
        }
    }

    private PlayerInventoryManager.ProfileData deserialize(byte[] blob, int heldItemSlot, float health, int foodLevel, float saturation) throws SQLException {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(new String(blob, StandardCharsets.UTF_8));

            PlayerInventoryManager.ProfileData data = new PlayerInventoryManager.ProfileData();
            for (String key : yaml.getStringList("captured")) {
                ProfileType type = ProfileType.fromConfigKey(key);
                if (type != null) {
                    data.captured.add(type);
                }
            }

            if (data.captured.contains(ProfileType.INVENTORY)) {
                ItemStack[] mainInventory = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    if (yaml.contains("main." + i)) {
                        mainInventory[i] = yaml.getItemStack("main." + i);
                    }
                }
                ItemStack[] armorContents = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    if (yaml.contains("armor." + i)) {
                        armorContents[i] = yaml.getItemStack("armor." + i);
                    }
                }
                data.mainInventory = mainInventory;
                data.armorContents = armorContents;
                data.offHandItem = yaml.contains("offhand") ? yaml.getItemStack("offhand") : null;
                data.heldItemSlot = heldItemSlot;
            }

            if (data.captured.contains(ProfileType.HEALTH)) {
                data.health = health;
            }
            if (data.captured.contains(ProfileType.HUNGER)) {
                data.foodLevel = foodLevel;
                data.saturation = saturation;
            }
            if (data.captured.contains(ProfileType.EXPERIENCE)) {
                data.level = yaml.getInt("level");
                data.exp = (float) yaml.getDouble("exp");
            }
            if (data.captured.contains(ProfileType.BED_SPAWN)) {
                data.bedSpawn = getLocation(yaml, "bedSpawn");
            }
            if (data.captured.contains(ProfileType.LOCATION)) {
                data.lastLocation = getLocation(yaml, "lastLocation");
            }

            return data;
        } catch (Exception e) {
            throw new SQLException("Failed to deserialize profile", e);
        }
    }

    private static void setLocation(YamlConfiguration yaml, String path, SavedLocation location) {
        yaml.set(path + ".world", location.world());
        yaml.set(path + ".x", location.x());
        yaml.set(path + ".y", location.y());
        yaml.set(path + ".z", location.z());
        yaml.set(path + ".yaw", (double) location.yaw());
        yaml.set(path + ".pitch", (double) location.pitch());
    }

    private static SavedLocation getLocation(YamlConfiguration yaml, String path) {
        if (!yaml.contains(path + ".world")) {
            return null;
        }
        return new SavedLocation(
                yaml.getString(path + ".world"),
                yaml.getDouble(path + ".x"),
                yaml.getDouble(path + ".y"),
                yaml.getDouble(path + ".z"),
                (float) yaml.getDouble(path + ".yaw"),
                (float) yaml.getDouble(path + ".pitch"));
    }
}
