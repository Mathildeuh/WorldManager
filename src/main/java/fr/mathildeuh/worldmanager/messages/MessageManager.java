package fr.mathildeuh.worldmanager.messages;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.Component;
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
        List<String> helpConfig = WorldManager.langConfig.getStringList("help");
        helpMessage.addAll(helpConfig);
    }

    public void sendHelp() {
        if (!(sender instanceof Player player)) return;
        for (String message : helpMessage) {
            MessageUtils.sendMini(player, message);
        }
    }

    public void parse(String message) {
        // Legacy prefix handling for backwards compatibility
        MessageType type;
        if (message.startsWith("(ERROR) ")) {
            message = message.replace("(ERROR) ", "");
            type = MessageType.ERROR;
        } else if (message.startsWith("(SUCCESS) ")) {
            message = message.replace("(SUCCESS) ", "");
            type = MessageType.SUCCESS;
        } else if (message.startsWith("(WAITING) ")) {
            message = message.replace("(WAITING) ", "");
            type = MessageType.WAITING;
        } else if (message.startsWith("(CUSTOM) ")) {
            message = message.replace("(CUSTOM) ", "");
            type = MessageType.CUSTOM;
        } else {
            type = MessageType.CUSTOM;
        }

        Component formatted = MessageUtils.parseMini(message);
        switch (type) {
            case ERROR -> MessageUtils.send(sender, MessageUtils.wrapError(formatted));
            case SUCCESS -> MessageUtils.send(sender, MessageUtils.wrapSuccess(formatted));
            case WAITING -> MessageUtils.send(sender, MessageUtils.wrapWaiting(formatted));
            case CUSTOM -> MessageUtils.send(sender, formatted);
        }
    }

    public enum MessageType {
        ERROR,
        SUCCESS,
        WAITING,
        CUSTOM
    }
}
