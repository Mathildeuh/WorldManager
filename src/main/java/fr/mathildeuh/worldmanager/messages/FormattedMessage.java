package fr.mathildeuh.worldmanager.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public record FormattedMessage(CommandSender sender, Component formattedMessage) {

    public void send() {
        sender.sendMessage(formattedMessage);
    }

}
