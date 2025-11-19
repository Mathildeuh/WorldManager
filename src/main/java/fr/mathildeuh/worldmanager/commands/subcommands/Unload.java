package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Unload {

    private final CommandSender sender;

    public Unload(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldManager.langConfig.sendError(sender, "unload.world_not_loaded", worldName);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                WorldManager.langConfig.sendError(sender, "unload.players_in_world");
                for (Player player1 : world.getPlayers()) {
                    player1.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }

        try {
            boolean unloaded = Bukkit.unloadWorld(world, true);

            if (unloaded) {
                WorldManager.langConfig.sendSuccess(sender, "unload.success", worldName);
                WorldManager.removeWorld(worldName);
                return;
            } } catch (Exception e) {
            e.printStackTrace();
            WorldManager.langConfig.sendError(sender, "unload.failed", worldName);
        }
    }
}
