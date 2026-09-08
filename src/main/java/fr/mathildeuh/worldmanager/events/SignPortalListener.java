package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.SignPortal;
import fr.mathildeuh.worldmanager.configs.SignPortalsManager;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Sign portals (Multiverse-SignPortals parity): a sign whose first line reads exactly
 * {@link SignPortalsManager#MARKER} and whose second line names a loaded world becomes a portal -
 * right-clicking it teleports to that world's spawn (see {@code configs/WorldsConfig#getSpawn},
 * falling back to the world's own vanilla spawn), the same resolution
 * {@code commands/subcommands/Teleport} uses. Uses the deprecated {@code String}-based
 * {@code SignChangeEvent}/{@code Sign} line accessors deliberately - they're still fully functional
 * on the 26.2 ceiling (confirmed via Paper's javadoc), just superseded by a newer
 * per-side-{@code Component} API, and they're the only sign text API that exists on this plugin's
 * 1.20.1 floor at all.
 */
public class SignPortalListener implements Listener {

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String marker = event.getLine(0);
        if (marker == null || !marker.trim().equalsIgnoreCase(SignPortalsManager.MARKER)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("worldmanager.sign.create")) {
            event.setCancelled(true);
            WorldManager.langConfig.sendError(player, "signportal.no_create_permission");
            return;
        }

        String destinationInput = event.getLine(1);
        World destination = destinationInput == null || destinationInput.isBlank()
                ? null : Bukkit.getWorld(destinationInput.trim());
        if (destination == null) {
            event.setCancelled(true);
            WorldManager.langConfig.sendError(player, "signportal.invalid_destination",
                    destinationInput == null ? "" : destinationInput);
            return;
        }

        event.setLine(0, SignPortalsManager.MARKER);
        event.setLine(1, destination.getName());
        SignPortalsManager.create(event.getBlock(), destination.getName(), player.getName());
        WorldManager.langConfig.sendSuccess(player, "signportal.created", destination.getName());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        SignPortal portal = SignPortalsManager.get(block);
        if (portal == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("worldmanager.sign.use")) {
            WorldManager.langConfig.sendError(player, "signportal.no_use_permission");
            return;
        }

        World destination = Bukkit.getWorld(portal.destinationWorld());
        if (destination == null) {
            WorldManager.langConfig.sendError(player, "signportal.destination_not_loaded", portal.destinationWorld());
            return;
        }

        Location spawn = WorldsConfig.getSpawn(destination);
        player.teleportAsync(spawn != null ? spawn : destination.getSpawnLocation());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        SignPortalsManager.remove(event.getBlock());
    }
}
