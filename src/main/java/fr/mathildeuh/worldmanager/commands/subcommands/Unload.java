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
            WorldManager.langConfig.sendFormat(sender, "unload.worldNotLoaded", worldName);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                WorldManager.langConfig.sendFormat(sender, "unload.playersInWorld");
            }
        }

        boolean unloaded = Bukkit.unloadWorld(world, true);

        if (unloaded) {
            WorldManager.langConfig.sendFormat(sender, "unload.unloadSuccess", worldName);
            WorldManager.removeWorld(worldName);
        } else {
            WorldManager.langConfig.sendFormat(sender, "unload.failedUnloadingWorld", worldName);
        }
    }
}
