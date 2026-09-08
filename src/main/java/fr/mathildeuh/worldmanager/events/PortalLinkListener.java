package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.configs.LinkedPortalsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Redirects Nether/End portal travel to the worlds configured under
 * {@code linked-portals} in config.yml, instead of Bukkit's auto-derived
 * "{@code <world>_nether}"/"{@code <world>_the_end}" pair.
 * <p>
 * For End travel, vanilla's own computed {@link PlayerPortalEvent#getTo()} already carries the
 * right destination (a fixed exit platform, or the player's last overworld position) - this
 * listener only swaps out which {@link World} those coordinates land in. Nether travel is
 * different: vanilla always assumes the standard 8:1 ratio when computing {@code getTo()}, so when
 * a link configures a non-default {@code nether-scale} the coordinates are recomputed from scratch
 * (see {@link #resolveNetherTarget}) instead of trusting vanilla's 8:1 math.
 */
public class PortalLinkListener implements Listener {

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Location vanillaTo = event.getTo();
        if (vanillaTo == null || vanillaTo.getWorld() == null) {
            return;
        }

        Location from = event.getFrom();
        String fromWorld = from.getWorld().getName();

        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            Location target = resolveNetherTarget(fromWorld, from, vanillaTo);
            if (target != null) {
                event.setTo(target);
            }
            return;
        }

        World redirectTo = switch (event.getCause()) {
            case END_PORTAL -> resolveEndRedirect(fromWorld);
            case END_GATEWAY -> resolveEndGatewayRedirect(fromWorld, vanillaTo.getWorld());
            default -> null;
        };

        if (redirectTo == null || redirectTo.equals(vanillaTo.getWorld())) {
            return;
        }

        event.setTo(new Location(redirectTo, vanillaTo.getX(), vanillaTo.getY(), vanillaTo.getZ(),
                vanillaTo.getYaw(), vanillaTo.getPitch()));
    }

    /**
     * Resolves the linked Nether world for either travel direction and, only when this link
     * configures a non-default {@code nether-scale}, recomputes X/Z directly from the origin
     * position (with a safe-Y lookup at the new coordinates) instead of reusing vanilla's own
     * always-8:1 {@code getTo()}. Returns {@code null} when there's no link to apply (falls back
     * to vanilla's own destination) or the link resolves to the same world vanilla already picked.
     */
    private Location resolveNetherTarget(String fromWorld, Location from, Location vanillaTo) {
        boolean fromIsOverworldSide = LinkedPortalsManager.getLinkedNether(fromWorld) != null;
        String overworldKey = fromIsOverworldSide ? fromWorld : LinkedPortalsManager.getOverworldForNether(fromWorld);
        if (overworldKey == null) {
            return null;
        }
        String targetName = fromIsOverworldSide ? LinkedPortalsManager.getLinkedNether(fromWorld) : overworldKey;
        if (targetName == null) {
            return null;
        }

        World redirectTo = Bukkit.getWorld(targetName);
        if (redirectTo == null || redirectTo.equals(vanillaTo.getWorld())) {
            return null;
        }

        double scale = LinkedPortalsManager.getNetherScale(overworldKey);
        if (scale == LinkedPortalsManager.VANILLA_NETHER_SCALE) {
            // Vanilla already computed the correct 8:1 coordinates - just swap the world.
            return new Location(redirectTo, vanillaTo.getX(), vanillaTo.getY(), vanillaTo.getZ(),
                    vanillaTo.getYaw(), vanillaTo.getPitch());
        }

        double x = fromIsOverworldSide ? from.getX() / scale : from.getX() * scale;
        double z = fromIsOverworldSide ? from.getZ() / scale : from.getZ() * scale;
        double y = safeY(redirectTo, x, z);
        return new Location(redirectTo, x, y, z, vanillaTo.getYaw(), vanillaTo.getPitch());
    }

    /** Highest solid block at (x, z) plus one, clamped to the world's height range. */
    private double safeY(World world, double x, double z) {
        int highest = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        int y = Math.max(highest + 1, world.getMinHeight() + 1);
        return Math.min(y, world.getMaxHeight() - 2);
    }

    private World resolveEndRedirect(String fromWorld) {
        String targetName = LinkedPortalsManager.getLinkedEnd(fromWorld);
        if (targetName == null) {
            targetName = LinkedPortalsManager.getOverworldForEnd(fromWorld);
        }
        return targetName == null ? null : Bukkit.getWorld(targetName);
    }

    private World resolveEndGatewayRedirect(String fromWorld, World vanillaTargetWorld) {
        // Intra-End gateways (the long-range ones between end cities) stay within the
        // same End world - only the "exit the End back to the overworld" gateway
        // actually crosses worlds, so only redirect when vanilla already agrees this
        // is a cross-world jump.
        if (vanillaTargetWorld.getName().equals(fromWorld)) {
            return null;
        }
        String targetName = LinkedPortalsManager.getOverworldForEnd(fromWorld);
        return targetName == null ? null : Bukkit.getWorld(targetName);
    }
}
