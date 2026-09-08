package fr.mathildeuh.worldmanager.configs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** A world-name-keyed location snapshot, safe to persist even if the world isn't loaded right now. */
public record SavedLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static SavedLocation of(Location location) {
        return new SavedLocation(location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    /** {@code null} if the world isn't currently loaded. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
