package fr.mathildeuh.worldmanager.util;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class SchedulerUtil {
    private SchedulerUtil() {
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(WorldManager.getInstance(), task -> runnable.run());
    }

    public static void runGlobal(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(WorldManager.getInstance(), runnable);
    }

    public static void runGlobalDelayed(Runnable runnable, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(WorldManager.getInstance(), task -> runnable.run(), delayTicks);
    }

    public static void runAtRegion(World world, int chunkX, int chunkZ, Runnable runnable) {
        Bukkit.getRegionScheduler().execute(WorldManager.getInstance(), world, chunkX, chunkZ, runnable);
    }
}
