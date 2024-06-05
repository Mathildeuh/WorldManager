package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.pregen.Pregen;
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
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            message.parse(MessageManager.MessageType.ERROR, "The world \"" + worldName + "\" is not loaded.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                new MessageManager(player).parse(MessageManager.MessageType.CUSTOM, "<color:#aa3e00>☠</color> <color:#ff2e1f>The world you were in has been unloaded.</color>");
            }
        }

        if (Pregen.generators.containsKey(worldName)) {
            Pregen.generators.get(worldName).cancel();
            Pregen.generators.remove(worldName);
        }

        boolean unloaded = Bukkit.unloadWorld(world, true);

        if (unloaded) {
            message.parse(MessageManager.MessageType.SUCCESS, "Success, world \"" + worldName + "\" successfully unloaded !");
            WorldManager.removeWorld(worldName);
        } else {
            message.parse(MessageManager.MessageType.ERROR, "Failed to unload world \"" + worldName + "\".");

        }
    }
}
