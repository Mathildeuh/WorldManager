package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists sign-based portals (Multiverse-SignPortals parity): a sign whose first line matches
 * {@link #MARKER} teleports whoever right-clicks it to the destination world named on its second
 * line (see {@code events/SignPortalListener}), stored in {@code signportals.yml} keyed by a random
 * id (never parsed back - the world/x/y/z fields are the source of truth, the id is just a stable
 * YAML section name so two portals can never collide on write).
 */
public class SignPortalsManager {

    public static final String MARKER = "[WorldManager]";

    private record BlockPos(String world, int x, int y, int z) {
        static BlockPos of(Block block) {
            return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private static final Map<BlockPos, SignPortal> portalsByLocation = new ConcurrentHashMap<>();

    private static File file;
    private static FileConfiguration config;

    private SignPortalsManager() { }

    public static void load() {
        portalsByLocation.clear();

        file = new File(WorldManager.getInstance().getDataFolder(), "signportals.yml");
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("sign-portals");
        if (section == null) {
            return;
        }

        int loaded = 0;
        for (String id : section.getKeys(false)) {
            String path = "sign-portals." + id + ".";
            String world = config.getString(path + "world");
            String destination = config.getString(path + "destination");
            if (world == null || destination == null || !config.contains(path + "x")) {
                continue;
            }
            int x = config.getInt(path + "x");
            int y = config.getInt(path + "y");
            int z = config.getInt(path + "z");
            String createdBy = config.getString(path + "createdBy", "unknown");

            portalsByLocation.put(new BlockPos(world, x, y, z),
                    new SignPortal(id, world, x, y, z, destination, createdBy));
            loaded++;
        }

        if (loaded > 0) {
            Bukkit.getLogger().info("[WorldManager] Loaded " + loaded + " sign portal(s)");
        }
    }

    /** Registers a new sign portal at {@code block} and persists it. */
    public static void create(Block block, String destinationWorld, String createdBy) {
        String id = UUID.randomUUID().toString();
        BlockPos pos = BlockPos.of(block);
        portalsByLocation.put(pos, new SignPortal(id, pos.world(), pos.x(), pos.y(), pos.z(), destinationWorld, createdBy));

        String path = "sign-portals." + id + ".";
        config.set(path + "world", pos.world());
        config.set(path + "x", pos.x());
        config.set(path + "y", pos.y());
        config.set(path + "z", pos.z());
        config.set(path + "destination", destinationWorld);
        config.set(path + "createdBy", createdBy);
        save();
    }

    /** Un-registers whatever sign portal is at {@code block}, if any. Returns whether one was removed. */
    public static boolean remove(Block block) {
        SignPortal portal = portalsByLocation.remove(BlockPos.of(block));
        if (portal == null) {
            return false;
        }
        config.set("sign-portals." + portal.id(), null);
        save();
        return true;
    }

    /** The sign portal registered at {@code block}, or {@code null} if it isn't one. */
    public static SignPortal get(Block block) {
        return portalsByLocation.get(BlockPos.of(block));
    }

    public static Collection<SignPortal> all() {
        return portalsByLocation.values();
    }

    private static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save signportals.yml: " + e.getMessage());
        }
    }
}
