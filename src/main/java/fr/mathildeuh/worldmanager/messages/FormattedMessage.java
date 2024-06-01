package fr.mathildeuh.worldmanager.messages;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record FormattedMessage(CommandSender sender, Component formattedMessage) {

    public void send() {
        if (sender instanceof Player player)
            WorldManager.adventure().player(player).sendMessage(formattedMessage);

    }
}