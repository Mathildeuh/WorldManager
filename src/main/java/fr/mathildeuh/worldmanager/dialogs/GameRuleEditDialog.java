package fr.mathildeuh.worldmanager.dialogs;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/** Editor for a single game rule: boolean toggle or integer slider. */
public final class GameRuleEditDialog {

    private GameRuleEditDialog() { }

    @SuppressWarnings("unchecked")
    public static Dialog build(Player player, String worldName, GameRule<?> rule) {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return WorldOptionsDialog.build(player, worldName);
        }

        if (rule.getType() == Boolean.class) {
            return buildBooleanEdit(player, worldName, (GameRule<Boolean>) rule, world);
        } else if (rule.getType() == Integer.class) {
            return buildIntegerEdit(player, worldName, (GameRule<Integer>) rule, world);
        }

        DialogBase base = DialogBase.builder(DialogUtils.mini("<yellow>" + rule.getName() + "</yellow>"))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.gamerules.not_available"))))
                .build();
        ActionButton back = DialogUtils.backButton(() -> DialogUtils.show(player, GameRuleListDialog.build(player, worldName, 0)));
        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.notice(back)));
    }

    private static Dialog buildBooleanEdit(Player player, String worldName, GameRule<Boolean> rule, World world) {
        Boolean current = world.getGameRuleValue(rule);
        ActionButton trueButton = DialogUtils.button(DialogUtils.miniFromLang("dialog.gamerules.true"),
                (response, audience) -> applyBoolean(player, worldName, rule, world, true));
        ActionButton falseButton = DialogUtils.button(DialogUtils.miniFromLang("dialog.gamerules.false"),
                (response, audience) -> applyBoolean(player, worldName, rule, world, false));

        DialogBase base = DialogBase.builder(DialogUtils.mini("<yellow>" + rule.getName() + "</yellow>"))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.gamerules.current_value", current))))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(trueButton, falseButton)));
    }

    private static void applyBoolean(Player player, String worldName, GameRule<Boolean> rule, World world, boolean value) {
        world.setGameRule(rule, value);
        DialogUtils.show(player, GameRuleListDialog.build(player, worldName, 0));
    }

    private static Dialog buildIntegerEdit(Player player, String worldName, GameRule<Integer> rule, World world) {
        Integer current = world.getGameRuleValue(rule);
        List<DialogInput> inputs = List.of(
                DialogInput.numberRange("value", DialogUtils.mini("<yellow>" + rule.getName() + "</yellow>"), 0f, 100_000f)
                        .width(200)
                        .labelFormat("%s: %s")
                        .initial(current == null ? 0f : current.floatValue())
                        .step(1f)
                        .build()
        );

        DialogBase base = DialogBase.builder(DialogUtils.mini("<yellow>" + rule.getName() + "</yellow>"))
                .canCloseWithEscape(true)
                .inputs(inputs)
                .build();

        ActionButton confirm = DialogUtils.button(DialogUtils.miniFromLang("dialog.confirm"),
                (response, audience) -> applyInteger(player, worldName, rule, world, response));
        ActionButton cancel = DialogUtils.backButton(() -> DialogUtils.show(player, GameRuleListDialog.build(player, worldName, 0)));

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(confirm, cancel)));
    }

    private static void applyInteger(Player player, String worldName, GameRule<Integer> rule, World world, DialogResponseView response) {
        Float value = response.getFloat("value");
        if (value != null) {
            world.setGameRule(rule, Math.round(value));
        }
        DialogUtils.show(player, GameRuleListDialog.build(player, worldName, 0));
    }
}
