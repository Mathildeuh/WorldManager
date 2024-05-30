package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Lists {
    private final JavaPlugin plugin;
    private final MessageManager message;
    private final CommandSender sender;

    public Lists(CommandSender sender) {
        this.plugin = JavaPlugin.getPlugin(WorldManager.class);
        this.message = new MessageManager(sender);
        this.sender = sender;
    }

    public void execute() {

        message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow>Worlds list:</yellow>");
        for (World world : Bukkit.getWorlds()) {

            message.parse("<color:#19cdff> <color:#7471b0>➥</color> " + world.getName() + "</color>");
        }
    }

}
