package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.Consumer;

/**
 * A single-use chat-based text prompt: closes whatever GUI is open, asks the player to type
 * their answer in chat (or {@code cancel} to abort), and hands the result to {@code onConfirm} -
 * {@code null} if cancelled or if the player disconnects before answering.
 *
 * <p>This replaces an earlier virtual-anvil-based implementation ({@code AnvilInput}). Three
 * different techniques for reading text back out of a virtual anvil's result slot all failed in
 * practice; server-side diagnostic logging confirmed {@code PrepareAnvilEvent} never fires at
 * all for a {@code Bukkit.createInventory(null, InventoryType.ANVIL, ...)} virtual inventory on
 * at least some servers, even though the client-side rename text field itself accepts input.
 * Chat capture has no dependency on anvil-menu internals and is the standard, version-agnostic
 * fallback for exactly this reason.
 */
public final class ChatInput implements Listener {

    private final Player player;
    private final Consumer<String> onConfirm;
    private boolean finished = false;

    private ChatInput(Player player, Consumer<String> onConfirm) {
        this.player = player;
        this.onConfirm = onConfirm;
    }

    /** {@code promptLines} are sent to the player in chat, in order, before capturing their next message. */
    public static void open(Player player, Consumer<String> onConfirm, Component... promptLines) {
        ChatInput handler = new ChatInput(player, onConfirm);
        Bukkit.getPluginManager().registerEvents(handler, WorldManager.getInstance());
        player.closeInventory();
        for (Component line : promptLines) {
            player.sendMessage(line);
        }
        player.sendMessage(GuiUtils.miniFromLang("gui.chat_input.cancel_hint"));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (finished || !event.getPlayer().equals(player)) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        SchedulerUtil.runGlobal(() -> finish("cancel".equalsIgnoreCase(text) ? null : text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!finished && event.getPlayer().equals(player)) {
            finish(null);
        }
    }

    private void finish(String text) {
        if (finished) {
            return;
        }
        finished = true;
        HandlerList.unregisterAll(this);
        onConfirm.accept(text);
    }
}
