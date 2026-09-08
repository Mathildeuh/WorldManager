package fr.mathildeuh.worldmanager.configs;

/**
 * A named cuboid portal region (Multiverse-Portals parity) - see
 * {@code events/CustomPortalListener}. Mutable (unlike {@link SignPortal}): destination,
 * permission, price and launch vector are each set independently via their own
 * {@code /wm portal} subcommand after the region itself is created.
 */
public final class CustomPortal {

    public final String name;
    public final String world;
    public final int minX, minY, minZ;
    public final int maxX, maxY, maxZ;
    public final String createdBy;

    /** Where a player who enters is sent, or {@code null} if this portal doesn't teleport (launch-only). */
    public SavedLocation destination;
    /** Extra permission node required to use this specific portal, or {@code null} for none. */
    public String permission;
    /** Vault price to use this portal; {@code 0} (default) is free, and it's always free if Vault is absent. */
    public double price;
    /** Velocity {dx, dy, dz} applied on use (a "cannon" launch), or {@code null} for none. */
    public double[] launch;

    public CustomPortal(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, String createdBy) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.createdBy = createdBy;
    }

    public boolean contains(String worldName, int bx, int by, int bz) {
        return world.equalsIgnoreCase(worldName)
                && bx >= minX && bx <= maxX
                && by >= minY && by <= maxY
                && bz >= minZ && bz <= maxZ;
    }
}
