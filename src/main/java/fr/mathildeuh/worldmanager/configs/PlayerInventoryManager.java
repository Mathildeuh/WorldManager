package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages player inventory saving and restoring for different world groups.
 * Uses both in-memory cache and database persistence.
 */
public class PlayerInventoryManager {

    // Structure: UUID -> GroupName -> InventoryData (in-memory cache)
    private static final Map<UUID, Map<String, InventoryData>> playerInventories = new HashMap<>();
    private static DatabaseManager databaseManager;

    /**
     * Initialize with database manager
     */
    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }

    /**
     * Save a player's inventory for a specific group
     * SYNCHRONOUS - ensures data is saved to database immediately
     */
    public static void saveInventory(Player player, String groupName) {
        if (player == null || groupName == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        InventoryData data = new InventoryData(player);

        // Save to memory cache immediately
        playerInventories.computeIfAbsent(playerId, k -> new HashMap<>()).put(groupName, data);

        // Save to database SYNCHRONOUSLY - do not use async
        if (databaseManager != null) {
            databaseManager.saveInventory(playerId, groupName,
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getInventory().getItemInOffHand(),
                    (float) player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation());
        }
    }

    /**
     * Restore a player's inventory for a specific group
     */
    public static boolean restoreInventory(Player player, String groupName) {
        if (player == null || groupName == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();

        // Try to get from memory cache first
        Map<String, InventoryData> playerData = playerInventories.get(playerId);
        if (playerData != null && playerData.containsKey(groupName)) {
            InventoryData data = playerData.get(groupName);
            data.apply(player);
            return true;
        }

        // Try to load from database
        if (databaseManager != null) {
            try {
                DatabaseManager.InventoryData dbData = databaseManager.loadInventory(playerId, groupName);
                if (dbData != null) {
                    // Apply and cache
                    InventoryData data = new InventoryData(dbData);
                    playerInventories.computeIfAbsent(playerId, k -> new HashMap<>()).put(groupName, data);
                    data.apply(player);
                    return true;
                }
            } catch (Exception e) {
                // Fall through to return false
            }
        }

        return false;
    }

    /**
     * Clear all saved inventories for a player (called on logout or plugin disable)
     */
    public static void clearPlayerData(UUID playerId) {
        playerInventories.remove(playerId);

        // Delete from database asynchronously
        if (databaseManager != null) {
            databaseManager.deleteAllPlayerInventories(playerId);
        }
    }

    /**
     * Clear all saved inventories (called on plugin disable)
     */
    public static void clearAllData() {
        playerInventories.clear();
    }

    /**
     * Get memory usage statistics
     */
    public static String getStatistics() {
        int totalPlayers = playerInventories.size();
        int totalGroups = playerInventories.values().stream()
                .mapToInt(Map::size)
                .sum();
        return "Cached inventories: " + totalPlayers + " players, " + totalGroups + " group inventories";
    }

    /**
     * Inner class to store inventory data
     */
    private static class InventoryData {
        private final ItemStack[] mainInventory;
        private final ItemStack[] armorContents;
        private final ItemStack offHandItem;
        private final int heldItemSlot;
        private final float health;
        private final int foodLevel;
        private final float saturation;

        /**
         * Create inventory data from a player
         */
        InventoryData(Player player) {
            this.mainInventory = player.getInventory().getContents().clone();
            this.armorContents = player.getInventory().getArmorContents().clone();
            this.offHandItem = player.getInventory().getItemInOffHand() != null ?
                    player.getInventory().getItemInOffHand().clone() : null;
            this.heldItemSlot = player.getInventory().getHeldItemSlot();
            this.health = (float) player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
        }

        /**
         * Create inventory data from database data
         */
        InventoryData(DatabaseManager.InventoryData dbData) {
            this.mainInventory = dbData.mainInventory;
            this.armorContents = dbData.armorContents;
            this.offHandItem = dbData.offHandItem;
            this.heldItemSlot = 0;
            this.health = dbData.health;
            this.foodLevel = dbData.foodLevel;
            this.saturation = dbData.saturation;
        }

        /**
         * Apply inventory data to a player
         */
        void apply(Player player) {
            player.getInventory().setContents(this.mainInventory.clone());
            player.getInventory().setArmorContents(this.armorContents.clone());
            if (this.offHandItem != null) {
                player.getInventory().setItemInOffHand(this.offHandItem.clone());
            } else {
                player.getInventory().setItemInOffHand(null);
            }
            player.getInventory().setHeldItemSlot(this.heldItemSlot);
            player.setHealth(Math.min(this.health, player.getMaxHealth()));
            player.setFoodLevel(Math.min(this.foodLevel, 20));
            player.setSaturation(Math.min(this.saturation, 20));
        }
    }
}

