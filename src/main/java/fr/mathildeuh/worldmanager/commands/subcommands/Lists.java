package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class Lists {
    CommandSender sender;

    public Lists(CommandSender sender) {
        this.sender = sender;
    }

    public void execute() {

        WorldManager.langConfig.sendWaiting(sender, "list.world_list");
        for (World world : Bukkit.getWorlds()) {
            WorldManager.langConfig.sendWaiting(sender, "list.item", world.getName());
        }
    }

}
