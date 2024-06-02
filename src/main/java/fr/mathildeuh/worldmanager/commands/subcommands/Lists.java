package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Lists {
    private final MessageManager message;

    public Lists(CommandSender sender) {
        this.message = new MessageManager(sender);
    }

    public void execute() {

        message.parse(MessageManager.MessageType.SUCCESS, "Worlds list: \n<color:#19cdff>Click to teleport</color>");
        message.parse(MessageManager.MessageType.CUSTOM, " ");
        for (World world : Bukkit.getWorlds()) {

            message.parse(MessageManager.MessageType.CUSTOM, "<hover:show_text:'<blue>Teleport to</blue> <yellow>" + world.getName() + "</yellow>'><click:run_command:'/wm tp " + world.getName() + " '><color:#19cdff> <color:#7471b0>➥</color> " + world.getName() + "</color></click></hover>");
        }
    }

}
