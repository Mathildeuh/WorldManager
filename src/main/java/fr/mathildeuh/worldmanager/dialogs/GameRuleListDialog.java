package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.WorldManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Game rules browser: a category picker, then a paginated list of rules within it. */
public final class GameRuleListDialog {

    private static final int PAGE_SIZE = 24;

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

    private static List<GameRule<?>> rulesInCategory(World world, String category) {
        List<GameRule<?>> result = new ArrayList<>();
        for (GameRule<?> rule : availableRules(world)) {
            if (GameRuleCategories.categoryOf(rule).equals(category)) {
                result.add(rule);
            }
        }
        return result;
    }

    /** Top-level screen: pick a category. */
    public static Dialog build(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return WorldOptionsDialog.build(player, worldName);
        }

        List<ActionButton> buttons = new ArrayList<>();
        for (String category : GameRuleCategories.orderedKeys()) {
            int count = rulesInCategory(world, category).size();
            if (count == 0) {
                continue;
            }
            String key = category;
            buttons.add(DialogUtils.button(
                    DialogUtils.miniFromLang("dialog.gamerules.category." + category),
                    DialogUtils.mini("<gray>" + count + "</gray>"),
                    (response, audience) -> DialogUtils.show(player, buildCategory(player, worldName, key, 0))
            ));
        }

        ActionButton back = DialogUtils.backButton(() -> {
            saveGameRules(world);
            DialogUtils.show(player, WorldOptionsDialog.build(player, worldName));
        });

        DialogBase base = DialogBase.builder(DialogUtils.miniFromLang("dialog.gamerules.category_title", worldName))
                .canCloseWithEscape(true)
                .build();

        return DialogUtils.listOrNotice(base, buttons, back, 2);
    }

    /** Rules within one category, paginated. */
    public static Dialog buildCategory(Player player, String worldName, String category, int page) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return WorldOptionsDialog.build(player, worldName);
        }

        List<GameRule<?>> rules = rulesInCategory(world, category);
        int pageCount = Math.max(1, (int) Math.ceil(rules.size() / (double) PAGE_SIZE));
        int clampedPage = Math.max(0, Math.min(page, pageCount - 1));

        List<ActionButton> buttons = new ArrayList<>();
        int from = clampedPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, rules.size());
        for (int i = from; i < to; i++) {
            GameRule<?> rule = rules.get(i);
            buttons.add(ruleButton(player, worldName, category, world, rule));
        }
        if (clampedPage > 0) {
            int prevPage = clampedPage - 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.previous"),
                    () -> DialogUtils.show(player, buildCategory(player, worldName, category, prevPage))));
        }
        if (clampedPage < pageCount - 1) {
            int nextPage = clampedPage + 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.next"),
                    () -> DialogUtils.show(player, buildCategory(player, worldName, category, nextPage))));
        }

        ActionButton back = DialogUtils.backButton(() -> DialogUtils.show(player, build(player, worldName)));

        // Substitution into {0} happens at the raw MiniMessage-string level, before parsing -
        // so the category name must be looked up as a raw string here, not as an already-built
        // Component (which would otherwise show its Java toString() debug form instead).
        Component title = pageCount > 1
                ? DialogUtils.miniFromLang("dialog.gamerules.category_title_paged",
                        WorldManager.langConfig.getString("dialog.gamerules.category." + category), clampedPage + 1, pageCount)
                : DialogUtils.miniFromLang("dialog.gamerules.category." + category);

        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(true)
                .build();

        return DialogUtils.listOrNotice(base, buttons, back, 2);
    }

    @SuppressWarnings("unchecked")
    private static ActionButton ruleButton(Player player, String worldName, String category, World world, GameRule<?> rule) {
        String readable = GameRuleCategories.readableName(rule.getName());
        Component label;
        if (rule.getType() == Boolean.class) {
            boolean value = Boolean.TRUE.equals(world.getGameRuleValue((GameRule<Boolean>) rule));
            label = DialogUtils.mini((value ? "<green>✔</green> " : "<red>✘</red> ") + "<white>" + readable + "</white>");
        } else if (rule.getType() == Integer.class) {
            Integer value = world.getGameRuleValue((GameRule<Integer>) rule);
            label = DialogUtils.mini("<aqua>" + value + "</aqua> <white>" + readable + "</white>");
        } else {
            label = DialogUtils.mini("<white>" + readable + "</white>");
        }

        return DialogUtils.button(label,
                DialogUtils.mini("<gray>" + rule.getName() + "</gray>"),
                (response, audience) -> DialogUtils.show(player, GameRuleEditDialog.build(player, worldName, rule, category)));
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
