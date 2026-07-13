package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.configs.LinkedPortalsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * Redirects Nether/End portal travel to the worlds configured under
 * {@code linked-portals} in config.yml, instead of Bukkit's auto-derived
 * "{@code <world>_nether}"/"{@code <world>_the_end}" pair.
 * <p>
 * Vanilla's own computed {@link PlayerPortalEvent#getTo()} already carries the
 * correctly scaled/clamped coordinates for the destination dimension - this
 * listener only swaps out which {@link World} those coordinates land in.
 */
public class PortalLinkListener implements Listener {

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Location vanillaTo = event.getTo();
        if (vanillaTo == null || vanillaTo.getWorld() == null) {
            return;
        }

        String fromWorld = event.getFrom().getWorld().getName();
        World redirectTo = switch (event.getCause()) {
            case NETHER_PORTAL -> resolveNetherRedirect(fromWorld);
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

    private World resolveNetherRedirect(String fromWorld) {
        String targetName = LinkedPortalsManager.getLinkedNether(fromWorld);
        if (targetName == null) {
            targetName = LinkedPortalsManager.getOverworldForNether(fromWorld);
        }
        return targetName == null ? null : Bukkit.getWorld(targetName);
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
