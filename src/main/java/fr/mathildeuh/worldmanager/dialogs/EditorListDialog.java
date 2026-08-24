package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.util.WorldFolders;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Paginated list of every world folder on disk, leading into {@link WorldOptionsDialog}. */
public final class EditorListDialog {

    private static final int PAGE_SIZE = 18;

    private EditorListDialog() { }

    public static Dialog build(Player player, int page) {
        List<String> worlds = WorldFolders.listAllWorldFolders();
        int pageCount = Math.max(1, (int) Math.ceil(worlds.size() / (double) PAGE_SIZE));
        int clampedPage = Math.max(0, Math.min(page, pageCount - 1));

        List<ActionButton> buttons = new ArrayList<>();
        int from = clampedPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, worlds.size());
        for (int i = from; i < to; i++) {
            String worldName = worlds.get(i);
            String creator = WorldsConfig.getCreator(worldName);
            buttons.add(DialogUtils.button(
                    DialogUtils.mini("<green><bold>" + worldName + "</bold></green>"),
                    DialogUtils.miniFromLang("dialog.editor.created_by", creator),
                    (response, audience) -> DialogUtils.show(player, WorldOptionsDialog.build(player, worldName))
            ));
        }

        List<ActionButton> navButtons = new ArrayList<>();
        if (clampedPage > 0) {
            int prevPage = clampedPage - 1;
            navButtons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.previous"),
                    () -> DialogUtils.show(player, build(player, prevPage))));
        }
        if (clampedPage < pageCount - 1) {
            int nextPage = clampedPage + 1;
            navButtons.add(DialogUtils.navButton(DialogUtils.miniFromLang("dialog.editor.next"),
                    () -> DialogUtils.show(player, build(player, nextPage))));
        }
        buttons.addAll(navButtons);

        ActionButton back = DialogUtils.backButton(() -> MainMenuDialog.open(player));

        var titleBuilder = DialogBase.builder(pageCount > 1
                ? DialogUtils.miniFromLang("dialog.editor.title_paged", clampedPage + 1, pageCount)
                : DialogUtils.miniFromLang("dialog.editor.title"))
                .canCloseWithEscape(true);

        if (worlds.isEmpty()) {
            titleBuilder.body(List.of(DialogBody.plainMessage(DialogUtils.miniFromLang("dialog.editor.empty"))));
        }

        return DialogUtils.listOrNotice(titleBuilder.build(), buttons, back, 2);
    }
}
