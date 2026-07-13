package fr.mathildeuh.worldmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared world-folder scanning helpers. Previously duplicated across
 * {@code WorldManagerCommand}, the creator flow, and every GUI list screen.
 */
public final class WorldFolders {

    private WorldFolders() { }

    private static boolean containsLevelDat(File folder) {
        return new File(folder, "level.dat").exists();
    }

    /** All world folders on disk (loaded or not) that contain a level.dat, sorted by name. */
    public static List<String> listAllWorldFolders() {
        List<String> names = new ArrayList<>();
        File[] worldFolders = Bukkit.getServer().getWorldContainer().listFiles();
        if (worldFolders != null) {
            for (File folder : worldFolders) {
                if (folder.isDirectory() && containsLevelDat(folder)) {
                    names.add(folder.getName());
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
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
