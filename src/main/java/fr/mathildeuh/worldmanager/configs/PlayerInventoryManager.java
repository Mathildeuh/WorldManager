package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.database.DatabaseManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-group player profiles (inventory and, per {@link ProfileType}, optionally
 * health/hunger/experience/bed-spawn/last-location) for the linked-worlds-inventory feature.
 * Uses both an in-memory cache and database persistence.
 */
public class PlayerInventoryManager {

    // Structure: UUID -> GroupName -> ProfileData (in-memory cache)
    private static final Map<UUID, Map<String, ProfileData>> profiles = new ConcurrentHashMap<>();
    private static DatabaseManager databaseManager;

    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }

    /**
     * Captures the player's current state for every {@code shares}-listed aspect and stores it
     * under {@code groupName} (memory immediately, database asynchronously). Aspects not in
     * {@code shares} are left completely untouched in storage.
     */
    public static void saveProfile(Player player, String groupName, Set<ProfileType> shares) {
        if (player == null || groupName == null || shares.isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        ProfileData data = profiles
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(groupName, k -> new ProfileData());
        data.captureFrom(player, shares);

        if (databaseManager != null) {
            ProfileData snapshot = data.copy();
            SchedulerUtil.runAsync(() -> databaseManager.saveProfile(playerId, groupName, snapshot));
        }
    }

    /**
     * Records where the player was standing, for {@code groupName}'s "last location" - called
     * when they leave that group (see {@code events/WorldChangeListener}), separately from
     * {@link #saveProfile} since by the time a world change is observed the player's prior exact
     * position is already gone.
     */
    public static void saveLastLocation(Player player, String groupName, org.bukkit.Location location) {
        if (player == null || groupName == null || location == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        ProfileData data = profiles
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(groupName, k -> new ProfileData());
        data.lastLocation = SavedLocation.of(location);
        data.captured.add(ProfileType.LOCATION);

        if (databaseManager != null) {
            ProfileData snapshot = data.copy();
            SchedulerUtil.runAsync(() -> databaseManager.saveProfile(playerId, groupName, snapshot));
        }
    }

    /**
     * Applies every {@code shares}-listed aspect that's actually available in storage for
     * {@code groupName} to the player. Returns whether any data existed at all for this group
     * (not just whether every requested aspect was found) - callers use this to decide whether
     * a "first time in this group" reset is needed.
     */
    public static boolean restoreProfile(Player player, String groupName, Set<ProfileType> shares) {
        if (player == null || groupName == null || shares.isEmpty()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Map<String, ProfileData> playerData = profiles.get(playerId);
        ProfileData data = playerData == null ? null : playerData.get(groupName);

        if (data == null && databaseManager != null) {
            try {
                ProfileData loaded = databaseManager.loadProfile(playerId, groupName);
                if (loaded != null) {
                    data = loaded;
                    profiles.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(groupName, data);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldManager] Failed to load profile for group '" + groupName + "': " + e.getMessage());
            }
        }

        if (data == null) {
            return false;
        }
        data.applyTo(player, shares);
        return true;
    }

    /** The stored "last location" for a group, or {@code null} if none is recorded (or the aspect isn't shared). */
    public static SavedLocation getLastLocation(Player player, String groupName, Set<ProfileType> shares) {
        if (!shares.contains(ProfileType.LOCATION)) {
            return null;
        }
        Map<String, ProfileData> playerData = profiles.get(player.getUniqueId());
        ProfileData data = playerData == null ? null : playerData.get(groupName);
        if (data == null && databaseManager != null) {
            try {
                data = databaseManager.loadProfile(player.getUniqueId(), groupName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldManager] Failed to load last location for group '" + groupName + "': " + e.getMessage());
            }
        }
        return (data != null && data.captured.contains(ProfileType.LOCATION)) ? data.lastLocation : null;
    }

    /**
     * Resets the player's shared aspects to sensible defaults for "first time in this group"
     * (only the aspects actually shared by the group are touched).
     */
    public static void resetProfile(Player player, Set<ProfileType> shares) {
        if (shares.contains(ProfileType.INVENTORY)) {
            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.getInventory().setItemInOffHand(null);
        }
        if (shares.contains(ProfileType.HEALTH)) {
            player.setHealth(player.getMaxHealth());
        }
        if (shares.contains(ProfileType.HUNGER)) {
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
        if (shares.contains(ProfileType.EXPERIENCE)) {
            player.setLevel(0);
            player.setExp(0f);
        }
        if (shares.contains(ProfileType.BED_SPAWN)) {
            player.setBedSpawnLocation(null, true);
        }
        // LOCATION: nothing to reset - no stored location just means "stay wherever the world change put you".
    }

    /**
     * Drops a player's in-memory profile cache (called on logout). Never touches the database -
     * the whole point of this feature is that per-group profiles persist across sessions.
     */
    public static void clearPlayerData(UUID playerId) {
        profiles.remove(playerId);
    }

    /**
     * Clear all cached profiles (called on plugin disable)
     */
    public static void clearAllData() {
        profiles.clear();
    }

    /**
     * Get memory usage statistics
     */
    public static String getStatistics() {
        int totalPlayers = profiles.size();
        int totalGroups = profiles.values().stream()
                .mapToInt(Map::size)
                .sum();
        return "Cached profiles: " + totalPlayers + " players, " + totalGroups + " group profiles";
    }

    /**
     * Mutable per-(player,group) snapshot; {@link #captured} tracks which aspects actually hold
     * real data. Public (with public fields) so {@code database.DatabaseManager} can serialize/
     * deserialize it directly as the DB round-trip DTO, matching how this class's predecessor
     * exposed its own DB transfer object.
     */
    public static final class ProfileData {
        public ItemStack[] mainInventory;
        public ItemStack[] armorContents;
        public ItemStack offHandItem;
        public int heldItemSlot;
        public float health;
        public int foodLevel;
        public float saturation;
        public int level;
        public float exp;
        public SavedLocation bedSpawn;
        public SavedLocation lastLocation;
        public final EnumSet<ProfileType> captured = EnumSet.noneOf(ProfileType.class);

        void captureFrom(Player player, Set<ProfileType> shares) {
            if (shares.contains(ProfileType.INVENTORY)) {
                mainInventory = player.getInventory().getContents().clone();
                armorContents = player.getInventory().getArmorContents().clone();
                ItemStack offHand = player.getInventory().getItemInOffHand();
                offHandItem = offHand.getType().isAir() ? null : offHand.clone();
                heldItemSlot = player.getInventory().getHeldItemSlot();
                captured.add(ProfileType.INVENTORY);
            }
            if (shares.contains(ProfileType.HEALTH)) {
                health = (float) player.getHealth();
                captured.add(ProfileType.HEALTH);
            }
            if (shares.contains(ProfileType.HUNGER)) {
                foodLevel = player.getFoodLevel();
                saturation = player.getSaturation();
                captured.add(ProfileType.HUNGER);
            }
            if (shares.contains(ProfileType.EXPERIENCE)) {
                level = player.getLevel();
                exp = player.getExp();
                captured.add(ProfileType.EXPERIENCE);
            }
            if (shares.contains(ProfileType.BED_SPAWN)) {
                org.bukkit.Location bed = player.getBedSpawnLocation();
                bedSpawn = bed == null ? null : SavedLocation.of(bed);
                captured.add(ProfileType.BED_SPAWN);
            }
        }

        void applyTo(Player player, Set<ProfileType> shares) {
            if (shares.contains(ProfileType.INVENTORY) && captured.contains(ProfileType.INVENTORY)) {
                player.getInventory().setContents(mainInventory.clone());
                player.getInventory().setArmorContents(armorContents.clone());
                player.getInventory().setItemInOffHand(offHandItem != null ? offHandItem.clone() : null);
                player.getInventory().setHeldItemSlot(heldItemSlot);
            }
            if (shares.contains(ProfileType.HEALTH) && captured.contains(ProfileType.HEALTH)) {
                player.setHealth(Math.min(health, (float) player.getMaxHealth()));
            }
            if (shares.contains(ProfileType.HUNGER) && captured.contains(ProfileType.HUNGER)) {
                player.setFoodLevel(Math.min(foodLevel, 20));
                player.setSaturation(Math.min(saturation, 20f));
            }
            if (shares.contains(ProfileType.EXPERIENCE) && captured.contains(ProfileType.EXPERIENCE)) {
                player.setLevel(level);
                player.setExp(exp);
            }
            if (shares.contains(ProfileType.BED_SPAWN) && captured.contains(ProfileType.BED_SPAWN)) {
                player.setBedSpawnLocation(bedSpawn == null ? null : bedSpawn.toLocation(), true);
            }
            // LOCATION is applied by the caller via getLastLocation() + a teleport, not here -
            // it needs to happen after the normal world-change teleport has fully settled.
        }

        ProfileData copy() {
            ProfileData copy = new ProfileData();
            copy.mainInventory = mainInventory == null ? null : mainInventory.clone();
            copy.armorContents = armorContents == null ? null : armorContents.clone();
            copy.offHandItem = offHandItem == null ? null : offHandItem.clone();
            copy.heldItemSlot = heldItemSlot;
            copy.health = health;
            copy.foodLevel = foodLevel;
            copy.saturation = saturation;
            copy.level = level;
            copy.exp = exp;
            copy.bedSpawn = bedSpawn;
            copy.lastLocation = lastLocation;
            copy.captured.addAll(captured);
            return copy;
        }
    }
}
