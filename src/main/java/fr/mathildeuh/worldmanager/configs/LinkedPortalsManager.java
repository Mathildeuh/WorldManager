package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Links a world's Nether/End companions to specific worlds instead of Bukkit's
 * auto-derived "{@code <world>_nether}"/"{@code <world>_the_end}" pair, e.g.:
 * <pre>
 * linked-portals:
 *   TEST:
 *     nether: Nether_TEST
 *     end: End_TEST
 *     nether-scale: 8.0   # optional, default 8.0 (vanilla overworld:nether ratio)
 * </pre>
 * Both directions are resolved: entering a portal in {@code TEST} goes to
 * {@code Nether_TEST}/{@code End_TEST}, and entering a portal back out of either
 * of those returns to {@code TEST}. {@code nether-scale} lets a linked nether companion use a
 * coordinate ratio other than vanilla's 8:1 - e.g. 1.0 for a 1:1 custom "twin world" instead of a
 * squeezed nether. There is no equivalent for the End: vanilla never derives End coordinates from
 * the player's position (entering always lands on the End's fixed exit platform, returning always
 * goes back to the player's last overworld position or its spawn) so there is nothing to scale -
 * only the destination world itself is swapped there.
 */
public class LinkedPortalsManager {

    public static final double VANILLA_NETHER_SCALE = 8.0;

    private static final Map<String, String> netherOf = new HashMap<>();
    private static final Map<String, String> endOf = new HashMap<>();
    private static final Map<String, String> overworldOfNether = new HashMap<>();
    private static final Map<String, String> overworldOfEnd = new HashMap<>();
    private static final Map<String, Double> netherScaleOf = new HashMap<>();

    private LinkedPortalsManager() { }

    public static void load() {
        netherOf.clear();
        endOf.clear();
        overworldOfNether.clear();
        overworldOfEnd.clear();
        netherScaleOf.clear();

        WorldManager plugin = JavaPlugin.getPlugin(WorldManager.class);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("linked-portals");
        if (section == null) {
            return;
        }

        for (String overworld : section.getKeys(false)) {
            String key = overworld.toLowerCase(Locale.ROOT);
            String nether = section.getString(overworld + ".nether");
            String end = section.getString(overworld + ".end");

            if (nether != null && !nether.isBlank()) {
                netherOf.put(key, nether);
                overworldOfNether.put(nether.toLowerCase(Locale.ROOT), overworld);
                netherScaleOf.put(key, section.getDouble(overworld + ".nether-scale", VANILLA_NETHER_SCALE));
            }
            if (end != null && !end.isBlank()) {
                endOf.put(key, end);
                overworldOfEnd.put(end.toLowerCase(Locale.ROOT), overworld);
            }
        }

        if (!netherOf.isEmpty() || !endOf.isEmpty()) {
            Bukkit.getLogger().info("[WorldManager] Loaded " + section.getKeys(false).size() + " linked-portal world group(s)");
        }
    }

    public static String getLinkedNether(String overworldName) {
        return netherOf.get(overworldName.toLowerCase(Locale.ROOT));
    }

    public static String getLinkedEnd(String overworldName) {
        return endOf.get(overworldName.toLowerCase(Locale.ROOT));
    }

    public static String getOverworldForNether(String netherName) {
        return overworldOfNether.get(netherName.toLowerCase(Locale.ROOT));
    }

    public static String getOverworldForEnd(String endName) {
        return overworldOfEnd.get(endName.toLowerCase(Locale.ROOT));
    }

    /** Configured overworld:nether coordinate ratio for this link, or the vanilla 8:1 default. */
    public static double getNetherScale(String overworldName) {
        return netherScaleOf.getOrDefault(overworldName.toLowerCase(Locale.ROOT), VANILLA_NETHER_SCALE);
    }
}
