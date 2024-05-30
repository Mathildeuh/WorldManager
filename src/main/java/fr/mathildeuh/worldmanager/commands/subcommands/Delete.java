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
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>The specified world does not exist.</color>");
            return;
        }

        if (targetWorld.equals(Bukkit.getWorlds().get(0))) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>You can't delete the default world.</color>");
            return;
        }

        message.parse("<gold>⌛</gold> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>World \"" + name + "\" and it's folder will be erased.</color>");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(targetWorld)) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }

        boolean unloadSuccess = plugin.getServer().unloadWorld(targetWorld, false);
        if (!unloadSuccess) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Failed to delete the world \"" + name + "\".</color>");
            return;
        }

        File worldFolder = targetWorld.getWorldFolder();
        try {
            FileUtils.deleteDirectory(worldFolder);
            message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow>Success !</yellow>");
        } catch (IOException e) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Failed to delete world folder for \"" + name + "\".</color>");
            e.printStackTrace();
        }
    }
}
