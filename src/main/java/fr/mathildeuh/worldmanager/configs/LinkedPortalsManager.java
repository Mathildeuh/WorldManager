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
 * </pre>
 * Both directions are resolved: entering a portal in {@code TEST} goes to
 * {@code Nether_TEST}/{@code End_TEST}, and entering a portal back out of either
 * of those returns to {@code TEST}.
 */
public class LinkedPortalsManager {

    private static final Map<String, String> netherOf = new HashMap<>();
    private static final Map<String, String> endOf = new HashMap<>();
    private static final Map<String, String> overworldOfNether = new HashMap<>();
    private static final Map<String, String> overworldOfEnd = new HashMap<>();

    private LinkedPortalsManager() { }

    public static void load() {
        netherOf.clear();
        endOf.clear();
        overworldOfNether.clear();
        overworldOfEnd.clear();

        WorldManager plugin = JavaPlugin.getPlugin(WorldManager.class);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("linked-portals");
        if (section == null) {
            return;
        }

        for (String overworld : section.getKeys(false)) {
            String nether = section.getString(overworld + ".nether");
            String end = section.getString(overworld + ".end");

            if (nether != null && !nether.isBlank()) {
                netherOf.put(overworld.toLowerCase(Locale.ROOT), nether);
                overworldOfNether.put(nether.toLowerCase(Locale.ROOT), overworld);
            }
            if (end != null && !end.isBlank()) {
                endOf.put(overworld.toLowerCase(Locale.ROOT), end);
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
}
