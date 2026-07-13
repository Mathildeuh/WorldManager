package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import fr.mathildeuh.worldmanager.util.WorldFolders;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Paginated list of unloaded world folders, each button loading the world in place. */
public final class LoaderDialog {

    private static final int PAGE_SIZE = 18;

    private LoaderDialog() { }

    public static Dialog build(Player player, int page) {
        List<String> worlds = WorldFolders.listUnloadedWorldFolders();
        int pageCount = Math.max(1, (int) Math.ceil(worlds.size() / (double) PAGE_SIZE));
        int clampedPage = Math.max(0, Math.min(page, pageCount - 1));

        List<ActionButton> buttons = new ArrayList<>();
        int from = clampedPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, worlds.size());
        for (int i = from; i < to; i++) {
            String worldName = worlds.get(i);
            buttons.add(DialogUtils.button(
                    DialogUtils.mini("<green><bold>" + worldName + "</bold></green>"),
                    (response, audience) -> {
                        new Load(player).execute(worldName, "normal", null);
                        DialogUtils.show(player, build(player, 0));
                    }
            ));
        }
        if (clampedPage > 0) {
            int prevPage = clampedPage - 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.previous"),
                    () -> DialogUtils.show(player, build(player, prevPage))));
        }
        if (clampedPage < pageCount - 1) {
            int nextPage = clampedPage + 1;
            buttons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.next"),
                    () -> DialogUtils.show(player, build(player, nextPage))));
        }

        ActionButton back = DialogUtils.backButton(() -> MainMenuDialog.open(player));

        Component title = pageCount > 1
                ? DialogUtils.miniFromLang("dialog.loader.title_paged", clampedPage + 1, pageCount)
                : DialogUtils.miniFromLang("dialog.loader.title");

        DialogBase.Builder baseBuilder = DialogBase.builder(title).canCloseWithEscape(true);
        if (worlds.isEmpty()) {
            baseBuilder.body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.loader.empty"))));
        }

        return Dialog.create(factory -> factory.empty()
                .base(baseBuilder.build())
                .type(DialogType.multiAction(buttons, back, 2)));
    }
}
