package fr.mathildeuh.worldmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared world-folder scanning helpers. Previously duplicated across
 * {@code WorldManagerCommand}, the creator flow, and every GUI list screen.
 */
public final class WorldFolders {

    private WorldFolders() { }

    /**
     * Whether a directory looks like a Minecraft world/dimension folder. Checks several
     * long-stable structural markers rather than only {@code level.dat} - some Paper/MC
     * versions lay out companion dimension folders (nether/end) differently, and relying
     * on a single exact file was found to miss valid world folders on 26.2/Leaf.
     */
    private static boolean looksLikeWorldFolder(File folder) {
        return new File(folder, "level.dat").exists()
                || new File(folder, "level.dat_old").exists()
                || new File(folder, "region").isDirectory()
                || new File(folder, "DIM-1").isDirectory()
                || new File(folder, "DIM1").isDirectory();
    }

    /**
     * All world folders on disk (loaded or not), sorted by name. Every currently-loaded
     * world is always included even if its folder doesn't have a level.dat directly in
     * it - some Minecraft/Paper versions store companion dimension folders (e.g. the
     * default world's nether/end) differently, and a loaded world unambiguously exists
     * regardless of what the on-disk heuristic finds.
     */
    public static List<String> listAllWorldFolders() {
        Set<String> names = new LinkedHashSet<>();

        File[] worldFolders = Bukkit.getServer().getWorldContainer().listFiles();
        if (worldFolders != null) {
            for (File folder : worldFolders) {
                if (folder.isDirectory() && looksLikeWorldFolder(folder)) {
                    names.add(folder.getName());
                }
            }
        }

        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }

        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    /** World folders on disk that are not currently loaded. */
    public static List<String> listUnloadedWorldFolders() {
        List<String> loaded = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            loaded.add(world.getName());
        }
        List<String> unloaded = new ArrayList<>();
        for (String name : listAllWorldFolders()) {
            if (!loaded.contains(name)) {
                unloaded.add(name);
            }
        }
        return unloaded;
    }
}
