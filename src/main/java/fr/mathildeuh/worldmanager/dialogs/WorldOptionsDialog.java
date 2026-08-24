package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Delete;
import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import fr.mathildeuh.worldmanager.commands.subcommands.Teleport;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Per-world action menu: game rules / unload / teleport / delete / backup / restore. */
public final class WorldOptionsDialog {

    private WorldOptionsDialog() { }

    public static Dialog build(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return buildUnloadedNotice(player, worldName);
        }

        boolean isDefaultWorld = world.equals(Bukkit.getWorlds().get(0));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.game_rules"),
                (response, audience) -> DialogUtils.show(player, GameRuleListDialog.build(player, worldName))));
        buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.teleport"),
                (response, audience) -> new Teleport(player).execute(player.getName(), worldName)));

        if (!isDefaultWorld) {
            buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.unload"),
                    (response, audience) -> DialogUtils.show(player, confirmUnload(player, worldName))));
            buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.backup"),
                    (response, audience) -> new Backup(player).execute(worldName)));
            buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.restore"),
                    (response, audience) -> DialogUtils.show(player, confirmRestore(player, worldName))));
            buttons.add(DialogUtils.button(DialogUtils.miniFromLang("dialog.options.delete"),
                    (response, audience) -> DialogUtils.show(player, confirmDelete(player, worldName))));
        }

        ActionButton back = DialogUtils.backButton(() -> DialogUtils.show(player, EditorListDialog.build(player, 0)));

        DialogBase.Builder baseBuilder = DialogBase.builder(DialogUtils.miniFromLang("dialog.options.title", worldName))
                .canCloseWithEscape(true);
        if (isDefaultWorld) {
            baseBuilder.body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.options.default_world_notice"))));
        }

        return DialogUtils.listOrNotice(baseBuilder.build(), buttons, back, 2);
    }

    private static Dialog buildUnloadedNotice(Player player, String worldName) {
        ActionButton load = DialogUtils.button(DialogUtils.miniFromLang("dialog.main.load"),
                (response, audience) -> {
                    new Load(player).execute(worldName, "normal", null);
                    DialogUtils.show(player, build(player, worldName));
                });
        ActionButton back = DialogUtils.backButton(() -> DialogUtils.show(player, EditorListDialog.build(player, 0)));

        DialogBase base = DialogBase.builder(DialogUtils.miniFromLang("dialog.options.title", worldName))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.options.not_loaded"))))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(load, back)));
    }

    private static Dialog confirmUnload(Player player, String worldName) {
        return confirmation(player, worldName, "dialog.options.confirm_unload",
                () -> {
                    new Unload(player).execute(worldName);
                    DialogUtils.show(player, EditorListDialog.build(player, 0));
                });
    }

    private static Dialog confirmDelete(Player player, String worldName) {
        return confirmation(player, worldName, "dialog.options.confirm_delete",
                () -> {
                    new Delete(player).execute(worldName);
                    DialogUtils.show(player, EditorListDialog.build(player, 0));
                });
    }

    private static Dialog confirmRestore(Player player, String worldName) {
        return confirmation(player, worldName, "dialog.options.confirm_restore",
                () -> {
                    new Restore(player).execute(worldName);
                    DialogUtils.show(player, build(player, worldName));
                });
    }

    private static Dialog confirmation(Player player, String worldName, String messageKey, Runnable onConfirm) {
        ActionButton yes = DialogUtils.button(DialogUtils.miniFromLang("dialog.confirm"),
                (response, audience) -> onConfirm.run());
        ActionButton no = DialogUtils.button(DialogUtils.miniFromLang("dialog.cancel"),
                (response, audience) -> DialogUtils.show(player, build(player, worldName)));

        DialogBase base = DialogBase.builder(DialogUtils.miniFromLang("dialog.options.title", worldName))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang(messageKey, worldName))))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(yes, no)));
    }
}
