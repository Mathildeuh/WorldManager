package fr.mathildeuh.worldmanager.messages;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtils {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String SOURCE = "World Manager";

    private MessageUtils() { }

    public static Component parseMini(String input) {
        return MINI.deserialize(input);
    }

    public static Component wrapError(Component message) {
        return MINI.deserialize("<color:#aa3e00>☠</color> <color:#7d66ff>{" + SOURCE + "}</color> <color:#ff2e1f>").append(message);
    }

    public static Component wrapSuccess(Component message) {
        return MINI.deserialize("<dark_green>✔</dark_green> <color:#7d66ff>{" + SOURCE + "}</color> ").append(message);
    }

    public static Component wrapWaiting(Component message) {
        return MINI.deserialize("<gold>⌛</gold> <color:#7d66ff>{" + SOURCE + "}</color> ").append(message);
    }

    public static void send(CommandSender target, Component component) {
        if (target instanceof Player player)
            WorldManager.adventure().player(player).sendMessage(component);
        else
            WorldManager.adventure().console().sendMessage(component);
    }

    public static void sendMini(CommandSender target, String miniMessageString) {
        send(target, parseMini(miniMessageString));
    }
}

