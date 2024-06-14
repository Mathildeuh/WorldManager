package fr.mathildeuh.worldmanager.messages;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {

    private final CommandSender sender;
    private final List<String> helpMessage = new ArrayList<>();
    private final String SOURCE = "World Manager";

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
            WorldManager.adventure().player(player).sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    public void parse(String message) {
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
            // TODO: MissConfigured message type
            type = MessageType.CUSTOM;
        }

        switch (type) {
            case ERROR -> formatError(message).send();
            case SUCCESS -> formatSuccess(message).send();
            case WAITING -> formatWaiting(message).send();
            case CUSTOM -> {
                Component formattedMessage = MiniMessage.miniMessage().deserialize(message);
                new FormattedMessage(sender, formattedMessage).send();
            }
        }
    }

    public FormattedMessage formatError(String message) {
        Component formattedMessage = MiniMessage.miniMessage().deserialize("<color:#aa3e00>☠</color> <color:#7d66ff>{" + SOURCE + "}</color> <color:#ff2e1f>" + message + "</color>");
        return new FormattedMessage(sender, formattedMessage);
    }

    public FormattedMessage formatSuccess(String message) {
        Component formattedMessage = MiniMessage.miniMessage().deserialize("<dark_green>✔</dark_green> <color:#7d66ff>{" + SOURCE + "}</color> <yellow>" + message + "</yellow>");
        return new FormattedMessage(sender, formattedMessage);
    }

    public FormattedMessage formatWaiting(String message) {
        Component formattedMessage = MiniMessage.miniMessage().deserialize("<gold>⌛</gold> <color:#7d66ff>{" + SOURCE + "}</color> <gray>" + message + "</gray>");
        return new FormattedMessage(sender, formattedMessage);
    }

    public enum MessageType {
        ERROR,
        SUCCESS,
        WAITING,
        CUSTOM
    }
}
