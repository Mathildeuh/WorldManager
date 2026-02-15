package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.configs.LinkedWorldsManager;
import fr.mathildeuh.worldmanager.configs.PlayerInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles inventory management when players change worlds.
 * - If worlds are in the same group: inventory is preserved
 * - If worlds are in different groups: inventory is saved/restored per group
 */
public class WorldChangeListener implements Listener {

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        // Feature disabled check
        if (!isFeatureEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        // Check if the worlds are in the same linked group
        if (LinkedWorldsManager.areWorldsLinked(fromWorld, toWorld)) {
            // Worlds are linked, keep inventory as-is
            return;
        }

        // Worlds are in different groups, need to swap inventories
        String fromGroup = LinkedWorldsManager.getWorldGroup(fromWorld);
        String toGroup = LinkedWorldsManager.getWorldGroup(toWorld);

        // Save current inventory for the group they're leaving
        if (fromGroup != null) {
            PlayerInventoryManager.saveInventory(player, fromGroup);
        }

        // Try to restore inventory for the group they're entering
        if (toGroup != null) {
            boolean restored = PlayerInventoryManager.restoreInventory(player, toGroup);
            if (!restored) {
                // First time in this group, clear inventory
                clearPlayerInventory(player);
            }
        } else {
            // Entering a world not in any group, clear inventory
            clearPlayerInventory(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save current inventory before cleanup when player leaves
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        String currentGroup = LinkedWorldsManager.getWorldGroup(currentWorld);

        // Save the player's current inventory if in a configured group
        if (currentGroup != null && isFeatureEnabled()) {
            PlayerInventoryManager.saveInventory(player, currentGroup);
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
                    .getConfig().getBoolean("enable-linked-inventory", true);
        } catch (Exception e) {
            return true; // Default to enabled if error
        }
    }

    /**
     * Clear the player's entire inventory
     */
    private void clearPlayerInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        player.getInventory().setItemInOffHand(null);

        // Reset health and food
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
    }
}

