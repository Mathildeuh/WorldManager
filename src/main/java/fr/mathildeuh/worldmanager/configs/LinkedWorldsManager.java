package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages linked worlds groups where players keep a per-group profile (inventory and,
 * optionally, health/hunger/experience/bed-spawn/last-location - see {@link ProfileType}) as
 * they travel between worlds in the same group.
 *
 * <p>Each group in {@code linked-worlds-inventory} can be configured two ways:
 * <pre>
 * # Legacy flat list - still fully supported, defaults to sharing inventory+health+hunger
 * # (exactly what every version before the per-group "share" option did).
 * group2:
 *   - world_nether
 *   - world_the_end
 *
 * # New form - explicit world list plus which profile aspects this group shares.
 * group1:
 *   worlds: [world, test]
 *   share: [inventory, health, hunger, experience, bed-spawn, location]
 * </pre>
 */
public class LinkedWorldsManager {

    private static final Set<ProfileType> DEFAULT_SHARES =
            EnumSet.of(ProfileType.INVENTORY, ProfileType.HEALTH, ProfileType.HUNGER);

    private static final Map<String, Set<String>> worldGroups = new HashMap<>();
    private static final Map<String, String> worldToGroup = new HashMap<>();
    private static final Map<String, Set<ProfileType>> groupShares = new HashMap<>();

    /**
     * Load linked worlds configuration
     */
    public static void loadLinkedWorlds() {
        worldGroups.clear();
        worldToGroup.clear();
        groupShares.clear();

        WorldManager plugin = JavaPlugin.getPlugin(WorldManager.class);

        // Check if the feature is enabled
        if (!plugin.getConfig().getBoolean("enable-linked-inventory", false)) {
            Bukkit.getLogger().info("[WorldManager] Linked worlds inventory feature is disabled");
            return;
        }

        ConfigurationSection linkedWorldsSection = plugin.getConfig().getConfigurationSection("linked-worlds-inventory");

        if (linkedWorldsSection == null) {
            Bukkit.getLogger().info("[WorldManager] No linked worlds inventory configuration found");
            return;
        }

        for (String groupName : linkedWorldsSection.getKeys(false)) {
            List<String> worlds;
            Set<ProfileType> shares;

            if (plugin.getConfig().isList("linked-worlds-inventory." + groupName)) {
                // Legacy flat-list format.
                worlds = plugin.getConfig().getStringList("linked-worlds-inventory." + groupName);
                shares = DEFAULT_SHARES;
            } else {
                worlds = plugin.getConfig().getStringList("linked-worlds-inventory." + groupName + ".worlds");
                List<String> shareKeys = plugin.getConfig().getStringList("linked-worlds-inventory." + groupName + ".share");
                shares = shareKeys.isEmpty() ? DEFAULT_SHARES : parseShares(groupName, shareKeys);
            }

            if (worlds.isEmpty()) {
                Bukkit.getLogger().warning("[WorldManager] Group '" + groupName + "' has no worlds configured");
                continue;
            }

            Set<String> worldSet = new HashSet<>();
            for (String world : worlds) {
                worldSet.add(world.toLowerCase());
                worldToGroup.put(world.toLowerCase(), groupName);
            }

            worldGroups.put(groupName, worldSet);
            groupShares.put(groupName, shares);
            Bukkit.getLogger().info("[WorldManager] Loaded linked world group '" + groupName + "' with " + worlds.size()
                    + " world(s), sharing " + shares);
        }

        if (worldGroups.isEmpty()) {
            Bukkit.getLogger().info("[WorldManager] No linked world groups loaded - inventory will be cleared on world change");
        }
    }

    private static Set<ProfileType> parseShares(String groupName, List<String> shareKeys) {
        Set<ProfileType> shares = EnumSet.noneOf(ProfileType.class);
        for (String key : shareKeys) {
            ProfileType type = ProfileType.fromConfigKey(key);
            if (type == null) {
                Bukkit.getLogger().warning("[WorldManager] Group '" + groupName + "' has unknown share type '" + key + "', ignoring it");
                continue;
            }
            shares.add(type);
        }
        return shares;
    }

    /**
     * Check if two worlds are in the same linked group
     *
     * @param fromWorld the source world name
     * @param toWorld   the destination world name
     * @return true if both worlds are in the same linked group, false otherwise
     */
    public static boolean areWorldsLinked(String fromWorld, String toWorld) {
        if (fromWorld == null || toWorld == null) {
            return false;
        }

        String fromWorldLower = fromWorld.toLowerCase();
        String toWorldLower = toWorld.toLowerCase();

        // If they're the same world, they're "linked"
        if (fromWorldLower.equals(toWorldLower)) {
            return true;
        }

        String fromGroup = worldToGroup.get(fromWorldLower);
        String toGroup = worldToGroup.get(toWorldLower);

        // Both worlds must be in a group AND in the same group
        return fromGroup != null && fromGroup.equals(toGroup);
    }

    /**
     * Get the group name for a world
     *
     * @param worldName the world name
     * @return the group name, or null if the world is not in any group
     */
    public static String getWorldGroup(String worldName) {
        if (worldName == null) {
            return null;
        }
        return worldToGroup.get(worldName.toLowerCase());
    }

    /** The profile aspects a group shares - empty (never null) if the group is unknown. */
    public static Set<ProfileType> getShares(String groupName) {
        if (groupName == null) {
            return Set.of();
        }
        Set<ProfileType> shares = groupShares.get(groupName);
        return shares != null ? shares : Set.of();
    }

    /**
     * Get all worlds in a group
     *
     * @param groupName the group name
     * @return a set of world names in the group, or an empty set if group doesn't exist
     */
    public static Set<String> getWorldsInGroup(String groupName) {
        Set<String> worlds = worldGroups.get(groupName);
        return worlds != null ? new HashSet<>(worlds) : new HashSet<>();
    }

    /**
     * Get all configured groups
     *
     * @return a map of group names to their world sets
     */
    public static Map<String, Set<String>> getAllGroups() {
        return new HashMap<>(worldGroups);
    }

    /**
     * Check if any linked worlds are configured
     *
     * @return true if there are any linked world groups configured
     */
    public static boolean hasLinkedWorlds() {
        return !worldGroups.isEmpty();
    }
}
