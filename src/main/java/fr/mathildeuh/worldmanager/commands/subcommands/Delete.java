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
    private static final String WORLD_DOES_NOT_EXIST = "The specified world does not exist.";
    private static final String CANNOT_DELETE_DEFAULT_WORLD = "You can't delete the default world.";
    private static final String WORLD_DELETED_SUCCESS = "World %s deleted successfully.";
    private static final String WORLD_DELETION_FAILED = "Failed to delete world folder for %s.";

    private final JavaPlugin plugin;
    private final CommandSender sender;
    private final MessageManager message;

    public Delete(CommandSender sender) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.sender = sender;
        this.message = new MessageManager(sender);
    }

    public void execute(String name) {
        World targetWorld = Bukkit.getWorld(name);

        if (targetWorld == null) {
            sender.sendMessage(WORLD_DOES_NOT_EXIST);
            return;
        }

        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            sender.sendMessage(CANNOT_DELETE_DEFAULT_WORLD);
            return;
        }

        message.delete(name);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }

        boolean unloadSuccess = plugin.getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            sender.sendMessage("Failed to unload the world " + name + ".");
            return;
        }

        File worldFolder = targetWorld.getWorldFolder();
        try {
            FileUtils.deleteDirectory(worldFolder);
            sender.sendMessage(String.format(WORLD_DELETED_SUCCESS, name));
        } catch (IOException e) {
            sender.sendMessage(String.format(WORLD_DELETION_FAILED, name));
            e.printStackTrace();
        }
    }
}
