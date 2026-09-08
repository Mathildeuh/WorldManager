package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists named cuboid portal regions (Multiverse-Portals parity): walking (or riding a vehicle)
 * into a portal's cuboid teleports to its configured destination and/or applies a launch velocity
 * (see {@code events/CustomPortalListener}), optionally gated by a permission node and a Vault
 * price. Stored in {@code portals.yml}, keyed by the portal's own user-chosen, unique name.
 */
public class CustomPortalsManager {

    private static final Map<String, CustomPortal> portals = new ConcurrentHashMap<>();

    // Two-point selection in progress per player (WorldEdit-style wand), never persisted.
    private static final Map<UUID, int[]> pos1Selections = new ConcurrentHashMap<>();
    private static final Map<UUID, int[]> pos2Selections = new ConcurrentHashMap<>();
    private static final Map<UUID, String> selectionWorld = new ConcurrentHashMap<>();

    private static File file;
    private static FileConfiguration config;

    private CustomPortalsManager() { }

    public static void load() {
        portals.clear();

        file = new File(WorldManager.getInstance().getDataFolder(), "portals.yml");
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("portals");
        if (section == null) {
            return;
        }

        int loaded = 0;
        for (String name : section.getKeys(false)) {
            String path = "portals." + name + ".";
            String world = config.getString(path + "world");
            if (world == null || !config.contains(path + "pos1.x") || !config.contains(path + "pos2.x")) {
                continue;
            }

            CustomPortal portal = new CustomPortal(name, world,
                    config.getInt(path + "pos1.x"), config.getInt(path + "pos1.y"), config.getInt(path + "pos1.z"),
                    config.getInt(path + "pos2.x"), config.getInt(path + "pos2.y"), config.getInt(path + "pos2.z"),
                    config.getString(path + "createdBy", "unknown"));

            if (config.contains(path + "destination.world")) {
                portal.destination = new SavedLocation(
                        config.getString(path + "destination.world"),
                        config.getDouble(path + "destination.x"),
                        config.getDouble(path + "destination.y"),
                        config.getDouble(path + "destination.z"),
                        (float) config.getDouble(path + "destination.yaw"),
                        (float) config.getDouble(path + "destination.pitch"));
            }
            portal.permission = config.getString(path + "permission");
            portal.price = config.getDouble(path + "price", 0.0);
            if (config.contains(path + "launch.x")) {
                portal.launch = new double[]{
                        config.getDouble(path + "launch.x"),
                        config.getDouble(path + "launch.y"),
                        config.getDouble(path + "launch.z")
                };
            }

            portals.put(name.toLowerCase(Locale.ROOT), portal);
            loaded++;
        }

        if (loaded > 0) {
            Bukkit.getLogger().info("[WorldManager] Loaded " + loaded + " custom portal(s)");
        }
    }

    public static void setPos1(Player player, Location loc) {
        pos1Selections.put(player.getUniqueId(), new int[]{loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()});
        selectionWorld.put(player.getUniqueId(), loc.getWorld().getName());
    }

    public static void setPos2(Player player, Location loc) {
        pos2Selections.put(player.getUniqueId(), new int[]{loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()});
        selectionWorld.put(player.getUniqueId(), loc.getWorld().getName());
    }

    public enum CreateResult { OK, MISSING_SELECTION, NAME_TAKEN, INVALID_NAME }

    public static CreateResult create(Player player, String name) {
        if (!WorldNameValidator.isValid(name)) {
            return CreateResult.INVALID_NAME;
        }
        if (portals.containsKey(name.toLowerCase(Locale.ROOT))) {
            return CreateResult.NAME_TAKEN;
        }
        UUID id = player.getUniqueId();
        int[] p1 = pos1Selections.get(id);
        int[] p2 = pos2Selections.get(id);
        String world = selectionWorld.get(id);
        if (p1 == null || p2 == null || world == null) {
            return CreateResult.MISSING_SELECTION;
        }

        CustomPortal portal = new CustomPortal(name, world, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], player.getName());
        portals.put(name.toLowerCase(Locale.ROOT), portal);
        save(portal);
        return CreateResult.OK;
    }

    public static boolean remove(String name) {
        CustomPortal removed = portals.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        config.set("portals." + removed.name, null);
        saveFile();
        return true;
    }

    public static CustomPortal get(String name) {
        return portals.get(name.toLowerCase(Locale.ROOT));
    }

    public static Collection<CustomPortal> all() {
        return portals.values();
    }

    /** First portal whose cuboid contains this block position, or {@code null}. */
    public static CustomPortal findContaining(String world, int bx, int by, int bz) {
        for (CustomPortal portal : portals.values()) {
            if (portal.contains(world, bx, by, bz)) {
                return portal;
            }
        }
        return null;
    }

    /** Persists one portal's current field values (call after mutating destination/permission/price/launch). */
    public static void save(CustomPortal portal) {
        String path = "portals." + portal.name + ".";
        config.set(path + "world", portal.world);
        config.set(path + "pos1.x", portal.minX);
        config.set(path + "pos1.y", portal.minY);
        config.set(path + "pos1.z", portal.minZ);
        config.set(path + "pos2.x", portal.maxX);
        config.set(path + "pos2.y", portal.maxY);
        config.set(path + "pos2.z", portal.maxZ);
        config.set(path + "createdBy", portal.createdBy);

        if (portal.destination != null) {
            config.set(path + "destination.world", portal.destination.world());
            config.set(path + "destination.x", portal.destination.x());
            config.set(path + "destination.y", portal.destination.y());
            config.set(path + "destination.z", portal.destination.z());
            config.set(path + "destination.yaw", (double) portal.destination.yaw());
            config.set(path + "destination.pitch", (double) portal.destination.pitch());
        } else {
            config.set(path + "destination", null);
        }

        config.set(path + "permission", portal.permission);
        config.set(path + "price", portal.price);

        if (portal.launch != null) {
            config.set(path + "launch.x", portal.launch[0]);
            config.set(path + "launch.y", portal.launch[1]);
            config.set(path + "launch.z", portal.launch[2]);
        } else {
            config.set(path + "launch", null);
        }

        saveFile();
    }

    private static void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save portals.yml: " + e.getMessage());
        }
    }
}
