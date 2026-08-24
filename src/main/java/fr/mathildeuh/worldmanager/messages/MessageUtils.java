package fr.mathildeuh.worldmanager.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class MessageUtils {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String SOURCE = "World Manager";
    private static final TextColor ERROR_COLOR = TextColor.fromHexString("#ff2e1f");

    private MessageUtils() { }

    public static Component parseMini(String input) {
        return MINI.deserialize(input);
    }

    public static Component wrapError(Component message) {
        // colorIfAbsent (not color()) so lang strings that already embed their own
        // color tags (e.g. "<red>Error:</red> ...") aren't overridden.
        return MINI.deserialize("<color:#aa3e00>☠</color> <color:#7d66ff>{" + SOURCE + "}</color> ")
                .append(message.colorIfAbsent(ERROR_COLOR));
    }

    public static Component wrapSuccess(Component message) {
        return MINI.deserialize("<dark_green>✔</dark_green> <color:#7d66ff>{" + SOURCE + "}</color> ").append(message);
    }

    public static Component wrapWaiting(Component message) {
        return MINI.deserialize("<gold>⌛</gold> <color:#7d66ff>{" + SOURCE + "}</color> ").append(message);
    }

    /**
     * Sends directly via {@link CommandSender#sendMessage(Component)} - on Paper,
     * CommandSender (Player, ConsoleCommandSender, ...) implements Adventure's
     * Audience natively. Do NOT route this through the adventure-platform-bukkit
     * BukkitAudiences bridge: that library exists for legacy Spigot/CraftBukkit
     * servers without native Adventure support, and going through it on a modern
     * Paper server behind a signed-chat-enforcing proxy (e.g. Velocity +
     * SignedVelocity) can produce messages that silently fail signature
     * verification and never reach the client, with no server-side exception.
     */
    public static void send(CommandSender target, Component component) {
        target.sendMessage(component);
    }

    public static void sendMini(CommandSender target, String miniMessageString) {
        send(target, parseMini(miniMessageString));
    }
}

