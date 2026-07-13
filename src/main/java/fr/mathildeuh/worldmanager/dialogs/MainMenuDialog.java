package fr.mathildeuh.worldmanager.dialogs;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.entity.Player;

import java.util.List;

/** The `/wm gui` entry point: Create / Edit / Load. */
public final class MainMenuDialog {

    private MainMenuDialog() { }

    public static Dialog build(Player player) {
        ActionButton create = DialogUtils.button(
                DialogUtils.miniFromLang("dialog.main.create"),
                DialogUtils.miniFromLang("dialog.main.create_tooltip"),
                (response, audience) -> DialogUtils.show(player, CreatorDialog.build(player))
        );
        ActionButton edit = DialogUtils.button(
                DialogUtils.miniFromLang("dialog.main.edit"),
                DialogUtils.miniFromLang("dialog.main.edit_tooltip"),
                (response, audience) -> DialogUtils.show(player, EditorListDialog.build(player, 0))
        );
        ActionButton load = DialogUtils.button(
                DialogUtils.miniFromLang("dialog.main.load"),
                DialogUtils.miniFromLang("dialog.main.load_tooltip"),
                (response, audience) -> DialogUtils.show(player, LoaderDialog.build(player, 0))
        );

        DialogBase base = DialogBase.builder(DialogUtils.miniFromLang("dialog.main.title"))
                .canCloseWithEscape(true)
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.multiAction(List.of(create, edit, load), null, 1)));
    }

    public static void open(Player player) {
        DialogUtils.show(player, build(player));
    }
}
