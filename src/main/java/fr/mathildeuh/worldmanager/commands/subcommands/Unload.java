package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Unload {

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
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>The world \"" + worldName + "\" is not loaded.</color>");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                new MessageManager(player).parse("<color:#aa3e00>☠</color> <color:#ff2e1f>The world you were in has been unloaded.</color>");
            }
        }
        // Décharger le monde
        boolean unloaded = Bukkit.unloadWorld(world, false);

        // Vérifier si le monde a été déchargé correctement
        if (unloaded) {
            message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow>Success, world \"" + worldName + "\" successfully unloaded !</yellow>");
            WorldManager.removeWorld(worldName);
        } else {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Failed to unload work \"" + worldName + "\".</color>");

        }
    }
}
