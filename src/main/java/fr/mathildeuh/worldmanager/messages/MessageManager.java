package fr.mathildeuh.worldmanager.messages;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private final CommandSender sender;
    private final List<String> helpMessage = new ArrayList<>();

    public MessageManager(CommandSender receivedSender) {
        this.sender = receivedSender;
        initializeHelpMessage();
    }

    private void initializeHelpMessage() {
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("");
        helpMessage.add("<color:#7d66ff>{</color><color:#6258a6>-----</color> <color:#02a876>World Manager - SubCommands</color> <color:#6258a6>-----</color><color:#7d66ff>}</color>");
        helpMessage.add("");
        helpMessage.add("<click:suggest_command:'/wm create '><color:#7471b0>➥</color> <color:#ff9900>create</color> <dark_green>[name]</dark_green> <dark_aqua><type> <seed> <generator></dark_aqua> <dark_gray>-</dark_gray> <color:#ffa1f9>Create a new world</color></click>");
        helpMessage.add("<click:suggest_command:'/wm delete '><color:#7471b0>➥</color> <color:#ff9900>delete</color> <dark_green>[name]</dark_green> <dark_gray>-</dark_gray> <color:#ffa1f9>Remove a world =(</color></click>");
        helpMessage.add("");
        helpMessage.add("<click:suggest_command:'/wm load '><color:#7471b0>➥</color> <color:#ff9900>load</color> <dark_green>[name] [type]</dark_green> <dark_gray>-</dark_gray> <color:#ffa1f9>Load a world</color></click>");
        helpMessage.add("<click:suggest_command:'/wm unload '><color:#7471b0>➥</color> <color:#ff9900>unload</color> <dark_green>[name]</dark_green> <dark_gray>-</dark_gray> <color:#ffa1f9>Unload a world</color></click>");
        helpMessage.add("");
        helpMessage.add("<click:suggest_command:'/wm teleport '><color:#7471b0>➥</color> <color:#ff9900>teleport</color> <dark_green>[world]</dark_green> <dark_aqua><player></dark_aqua> <dark_gray>-</dark_gray> <color:#ffa1f9>Teleport you/player to world</color></click>");
    }

    public void help() {
        if (!(sender instanceof Player player)) return;
        for (String message : helpMessage) {
            WorldManager.adventure().player(player).sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    public void create(String data) {
        sender.sendMessage("Starting world creation ...");
        sender.sendMessage(data);
    }

    public void delete(String worldName) {
        sender.sendMessage("Deleting world " + worldName + " ...");
    }

    public void load(String worldName) {
        sender.sendMessage("Loading world " + worldName + " ...");
    }

    public void unload(String worldName) {
        sender.sendMessage("Unloading world " + worldName + " ...");
    }
}
