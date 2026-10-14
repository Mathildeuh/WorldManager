package fr.mathildeuh.worldmanager.util;

import fr.mathildeuh.worldmanager.WorldManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public final class SchedulerUtil {
    private SchedulerUtil() {
    }

    /** Entity tasks follow the entity across regions; retirement must never access game state. */
    public static boolean runEntity(Entity entity, Runnable runnable, Runnable retired) {
        WorldManager plugin = WorldManager.getInstance();
        if (!plugin.acceptsTasks()) {
            if (retired != null) retired.run();
            return false;
        }
        try {
            boolean accepted = entity.getScheduler().execute(plugin, () -> {
                if (plugin.acceptsTasks()) runnable.run();
                else if (retired != null) retired.run();
            }, retired, 1L);
            if (!accepted && retired != null) retired.run();
            return accepted;
        } catch (org.bukkit.plugin.IllegalPluginAccessException e) {
            if (retired != null) retired.run();
            return false;
        }
    }

    public static boolean runEntity(Entity entity, Runnable runnable) {
        return runEntity(entity, runnable, null);
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

    /** Repeating global task (e.g. an animation heartbeat). Cancel via the returned handle. */
    public static ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(WorldManager.getInstance(), task -> runnable.run(), delayTicks, periodTicks);
    }

    public static void runAtRegion(World world, int chunkX, int chunkZ, Runnable runnable) {
        Bukkit.getRegionScheduler().execute(WorldManager.getInstance(), world, chunkX, chunkZ, runnable);
    }
}
