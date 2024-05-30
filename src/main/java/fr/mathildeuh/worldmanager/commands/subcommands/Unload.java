package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Unload {

    // TODO: KICK PLAYERS
    private final CommandSender sender;
    private final MessageManager message;

    public Unload(CommandSender sender) {
        this.sender = sender;
        this.message = new MessageManager(sender);
    }

    public void execute(String worldName) {
        // Vérifier si le monde est déjà chargé
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("The world " + worldName + " is not loaded.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }
        // Décharger le monde
        boolean unloaded = Bukkit.unloadWorld(world, false);

        // Vérifier si le monde a été déchargé correctement
        if (unloaded) {
            message.unload(worldName);
            sender.sendMessage("World " + worldName + " unloaded successfully!");
        } else {
            sender.sendMessage("Failed to unload world " + worldName + ".");
        }
    }
}
