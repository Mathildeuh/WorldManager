package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.configs.LinkedWorldsManager;
import fr.mathildeuh.worldmanager.configs.PlayerInventoryManager;
import fr.mathildeuh.worldmanager.configs.ProfileType;
import fr.mathildeuh.worldmanager.configs.SavedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;

/**
 * Handles per-group player profiles (see {@link ProfileType}) as players travel between worlds:
 * - Same group (or same world): nothing is touched.
 * - Leaving a group: every aspect that group shares is saved; if it also shares "location", the
 *   player's exact pre-teleport position is captured here (in {@link #onPlayerTeleport}, since by
 *   the time {@link PlayerChangedWorldEvent} fires that position is already gone).
 * - Entering a group: every aspect it shares is restored (or reset to a default, first time in
 *   that group); if "location" is shared and a last position is on record, the player is
 *   teleported there right after.
 * - Entering an ungrouped world from a group: whatever that group shared is reset to default
 *   (this is load-bearing - a bug here in 2.2.0-2.2.1 wiped inventories on every world change
 *   between two ungrouped worlds, regardless of whether the feature was even enabled; see
 *   AUDIT.md). Two ungrouped worlds are never touched at all.
 */
public class WorldChangeListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!isFeatureEnabled()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || from.getWorld().equals(to.getWorld())) {
            return;
        }

        String fromWorld = from.getWorld().getName();
        String toWorld = to.getWorld().getName();
        if (LinkedWorldsManager.areWorldsLinked(fromWorld, toWorld)) {
            return;
        }

        String fromGroup = LinkedWorldsManager.getWorldGroup(fromWorld);
        if (fromGroup == null) {
            return;
        }

        Set<ProfileType> shares = LinkedWorldsManager.getShares(fromGroup);
        if (shares.contains(ProfileType.LOCATION)) {
            PlayerInventoryManager.saveLastLocation(event.getPlayer(), fromGroup, from);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        if (!isFeatureEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        if (LinkedWorldsManager.areWorldsLinked(fromWorld, toWorld)) {
            return;
        }

        String fromGroup = LinkedWorldsManager.getWorldGroup(fromWorld);
        String toGroup = LinkedWorldsManager.getWorldGroup(toWorld);

        if (fromGroup == null && toGroup == null) {
            return;
        }

        if (fromGroup != null) {
            Set<ProfileType> fromShares = LinkedWorldsManager.getShares(fromGroup);
            if (!fromShares.isEmpty()) {
                PlayerInventoryManager.saveProfile(player, fromGroup, fromShares);
            }
        }

        if (toGroup != null) {
            Set<ProfileType> toShares = LinkedWorldsManager.getShares(toGroup);
            if (toShares.isEmpty()) {
                return;
            }
            boolean restored = PlayerInventoryManager.restoreProfile(player, toGroup, toShares);
            if (!restored) {
                PlayerInventoryManager.resetProfile(player, toShares);
            }

            SavedLocation lastLocation = PlayerInventoryManager.getLastLocation(player, toGroup, toShares);
            if (lastLocation != null) {
                Location target = lastLocation.toLocation();
                if (target != null) {
                    player.teleportAsync(target);
                }
            }
        } else {
            // Leaving a group into a world that isn't part of any group: reset whatever that
            // group was managing, exactly as before (see class doc for why this must never
            // touch a player moving between two ungrouped worlds).
            PlayerInventoryManager.resetProfile(player, LinkedWorldsManager.getShares(fromGroup));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        String currentGroup = LinkedWorldsManager.getWorldGroup(currentWorld);

        if (currentGroup != null && isFeatureEnabled()) {
            Set<ProfileType> shares = LinkedWorldsManager.getShares(currentGroup);
            if (!shares.isEmpty()) {
                PlayerInventoryManager.saveProfile(player, currentGroup, shares);
            }
            if (shares.contains(ProfileType.LOCATION)) {
                PlayerInventoryManager.saveLastLocation(player, currentGroup, player.getLocation());
            }
        }

        // Clean up memory when player leaves
        PlayerInventoryManager.clearPlayerData(player.getUniqueId());
    }

    /**
     * Check if the linked inventory feature is enabled
     */
    private boolean isFeatureEnabled() {
        try {
            return Bukkit.getPluginManager().getPlugin("WorldManager")
                    .getConfig().getBoolean("enable-linked-inventory", false);
        } catch (Exception e) {
            // Fail closed: a config read error should never start touching player inventories.
            return false;
        }
    }
}
