package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Delete {

    private final JavaPlugin plugin;
    private final MessageManager message;

    public Delete(CommandSender sender) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.message = new MessageManager(sender);
    }

    public void execute(String name) {
        World targetWorld = Bukkit.getWorld(name);

        if (targetWorld == null) {
            message.parse(MessageManager.MessageType.ERROR, "The specified world does not exist.");
            return;
        }

        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            message.parse(MessageManager.MessageType.ERROR, "You can't delete the default world.");
            return;
        }

        message.parse(MessageManager.MessageType.WAITING, "World \"" + name + "\" and it's folder will be erased.");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                new MessageManager(player).parse(MessageManager.MessageType.CUSTOM, "<color:#aa3e00>☠</color> <color:#ff2e1f>The world you were in has been deleted.</color>");
            }
        }

        boolean unloadSuccess = plugin.getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            message.parse(MessageManager.MessageType.ERROR, "Failed to delete the world \"" + name + "\".");
            return;
        }

        File worldFolder = targetWorld.getWorldFolder();
        try {
            FileUtils.deleteDirectory(worldFolder);
            message.parse(MessageManager.MessageType.SUCCESS, "Success !");
            WorldManager.removeWorld(targetWorld.getName());

        } catch (IOException e) {
            message.parse(MessageManager.MessageType.ERROR, "Failed to delete world folder for \"" + name + "\".");
            e.printStackTrace();
        }
    }
}
