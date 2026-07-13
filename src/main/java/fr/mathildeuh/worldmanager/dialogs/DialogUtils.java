package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Shared helpers for building Paper Dialog screens, mirroring the role
 * {@link MessageUtils} plays for chat messages: consistent styling and
 * lang-file-backed text for every dialog in the plugin.
 */
public final class DialogUtils {

    private DialogUtils() { }

    public static Component miniFromLang(String path, Object... args) {
        String mini = WorldManager.langConfig.formatMessage(path, args);
        if (mini == null) {
            org.bukkit.Bukkit.getLogger().warning("[WorldManager] Missing dialog message key: " + path);
            return Component.text(path);
        }
        return MessageUtils.parseMini(mini);
    }

    public static Component mini(String rawMiniMessage) {
        return MessageUtils.parseMini(rawMiniMessage);
    }

    /**
     * Default single-use, short-lived callback options for a dialog button.
     * Every dialog we show is freshly built per-open, so callbacks never need
     * to survive longer than a player would plausibly stare at a menu.
     */
    public static ClickCallback.Options singleUse() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }

    public static ActionButton button(Component label, DialogActionCallback onClick) {
        return ActionButton.builder(label)
                .action(DialogAction.customClick(onClick, singleUse()))
                .build();
    }

    public static ActionButton button(Component label, Component tooltip, DialogActionCallback onClick) {
        return ActionButton.builder(label)
                .tooltip(tooltip)
                .action(DialogAction.customClick(onClick, singleUse()))
                .build();
    }

    public static ActionButton navButton(Component label, Runnable onClick) {
        return button(label, (response, audience) -> onClick.run());
    }

    public static ActionButton backButton(Runnable onClick) {
        return navButton(miniFromLang("dialog.back"), onClick);
    }

    public static void show(Player player, io.papermc.paper.dialog.Dialog dialog) {
        player.showDialog(dialog);
    }
}
