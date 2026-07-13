package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.WorldManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Paginated list of every game rule supported by a world. */
public final class GameRuleListDialog {

    private static final int PAGE_SIZE = 20;

    private GameRuleListDialog() { }

    @SuppressWarnings("removal")
    private static List<GameRule<?>> availableRules(World world) {
        List<GameRule<?>> rules = new ArrayList<>();
        for (GameRule<?> rule : GameRule.values()) {
            try {
                world.getGameRuleValue(rule);
                rules.add(rule);
            } catch (IllegalArgumentException ignored) {
                // Not available for this world/version.
            }
        }
        return rules;
    }

    public static Dialog build(Player player, String worldName, int page) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return WorldOptionsDialog.build(player, worldName);
        }

        List<GameRule<?>> rules = availableRules(world);
        int pageCount = Math.max(1, (int) Math.ceil(rules.size() / (double) PAGE_SIZE));
        int clampedPage = Math.max(0, Math.min(page, pageCount - 1));

        List<ActionButton> buttons = new ArrayList<>();
        int from = clampedPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, rules.size());
        for (int i = from; i < to; i++) {
            GameRule<?> rule = rules.get(i);
            Object value = world.getGameRuleValue(rule);
            buttons.add(DialogUtils.button(
                    DialogUtils.mini("<yellow>" + rule.getName() + "</yellow>"),
                    DialogUtils.miniFromLang("dialog.gamerules.current_value", value),
                    (response, audience) -> DialogUtils.show(player, GameRuleEditDialog.build(player, worldName, rule))
            ));
        }
        if (clampedPage > 0) {
            int prevPage = clampedPage - 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.previous"),
                    () -> DialogUtils.show(player, build(player, worldName, prevPage))));
        }
        if (clampedPage < pageCount - 1) {
            int nextPage = clampedPage + 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.next"),
                    () -> DialogUtils.show(player, build(player, worldName, nextPage))));
        }

        ActionButton back = DialogUtils.backButton(() -> {
            saveGameRules(world);
            DialogUtils.show(player, WorldOptionsDialog.build(player, worldName));
        });

        Component title = pageCount > 1
                ? DialogUtils.miniFromLang("dialog.gamerules.title_paged", worldName, clampedPage + 1, pageCount)
                : DialogUtils.miniFromLang("dialog.gamerules.title", worldName);

        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(true)
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.multiAction(buttons, back, 3)));
    }

    @SuppressWarnings("removal")
    static void saveGameRules(World world) {
        for (GameRule<?> rule : GameRule.values()) {
            try {
                Object value = world.getGameRuleValue(rule);
                WorldManager.worldsConfig.set("worlds." + world.getName() + ".gameRules." + rule.getName(), value);
            } catch (IllegalArgumentException ignored) {
                // Not available for this world/version.
            }
        }
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save game rules for " + world.getName() + ": " + e.getMessage());
        }
    }
}
