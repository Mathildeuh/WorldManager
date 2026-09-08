package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.CustomPortal;
import fr.mathildeuh.worldmanager.configs.CustomPortalsManager;
import fr.mathildeuh.worldmanager.util.EconomyHook;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom portal regions (Multiverse-Portals parity): stepping into a {@link CustomPortal}'s cuboid
 * (on foot or riding a vehicle) teleports to its destination and/or applies its launch velocity,
 * gated by an optional permission node and Vault price. Containment is only checked when the
 * mover's block position actually changes (skips the many same-block {@link PlayerMoveEvent}s a
 * server fires per tick just from head rotation), and a short cooldown after any trigger stops the
 * destination side of a portal (or an overlapping one) from immediately re-triggering.
 */
public class CustomPortalListener implements Listener {

    private static final long COOLDOWN_MS = 1500;
    private static final Map<UUID, Long> lastTrigger = new ConcurrentHashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isInsideVehicle()) {
            // Handled by onVehicleMove instead - avoids double-triggering for a passenger.
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to) || isOnCooldown(player.getUniqueId())) {
            return;
        }

        CustomPortal portal = CustomPortalsManager.findContaining(to.getWorld().getName(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        if (portal == null || (portal.destination == null && portal.launch == null)) {
            return;
        }

        if (portal.permission != null && !portal.permission.isBlank() && !player.hasPermission(portal.permission)) {
            WorldManager.langConfig.sendError(player, "portal.no_permission");
            return;
        }
        if (portal.price > 0 && EconomyHook.isEnabled()) {
            if (!EconomyHook.has(player, portal.price)) {
                WorldManager.langConfig.sendError(player, "portal.cant_afford", EconomyHook.format(portal.price));
                return;
            }
            EconomyHook.withdraw(player, portal.price);
        }

        markCooldown(player.getUniqueId());

        if (portal.destination == null) {
            player.setVelocity(new Vector(portal.launch[0], portal.launch[1], portal.launch[2]));
            return;
        }

        Location target = portal.destination.toLocation();
        if (target == null) {
            WorldManager.langConfig.sendError(player, "portal.destination_not_loaded");
            return;
        }
        player.teleportAsync(target).thenAccept(success -> {
            if (success && portal.launch != null) {
                SchedulerUtil.runGlobal(() -> player.setVelocity(new Vector(portal.launch[0], portal.launch[1], portal.launch[2])));
            }
        });
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        Vehicle vehicle = event.getVehicle();
        if (sameBlock(from, to) || isOnCooldown(vehicle.getUniqueId())) {
            return;
        }

        CustomPortal portal = CustomPortalsManager.findContaining(to.getWorld().getName(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        if (portal == null || portal.destination == null) {
            return;
        }

        if (!vehicle.getPassengers().isEmpty() && vehicle.getPassengers().get(0) instanceof Player player) {
            if (portal.permission != null && !portal.permission.isBlank() && !player.hasPermission(portal.permission)) {
                return;
            }
            if (portal.price > 0 && EconomyHook.isEnabled()) {
                if (!EconomyHook.has(player, portal.price)) {
                    return;
                }
                EconomyHook.withdraw(player, portal.price);
            }
        }

        Location target = portal.destination.toLocation();
        if (target == null) {
            return;
        }
        markCooldown(vehicle.getUniqueId());
        vehicle.teleportAsync(target).thenAccept(success -> {
            if (success && portal.launch != null) {
                SchedulerUtil.runGlobal(() -> vehicle.setVelocity(new Vector(portal.launch[0], portal.launch[1], portal.launch[2])));
            }
        });
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()
                && from.getWorld().equals(to.getWorld());
    }

    private static boolean isOnCooldown(UUID id) {
        Long last = lastTrigger.get(id);
        return last != null && System.currentTimeMillis() - last < COOLDOWN_MS;
    }

    private static void markCooldown(UUID id) {
        lastTrigger.put(id, System.currentTimeMillis());
    }
}
