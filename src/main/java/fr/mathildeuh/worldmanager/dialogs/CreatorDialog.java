package fr.mathildeuh.worldmanager.dialogs;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Create;
import fr.mathildeuh.worldmanager.util.WorldFolders;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** World-creation form: a single dialog replacing the old chat-capture flow. */
public final class CreatorDialog {

    private static final Set<String> RESERVED_NAMES = Set.of(
            "plugins", "logs", "libraries", "versions", "config", "cache");

    private CreatorDialog() { }

    public static Dialog build(Player player) {
        List<SingleOptionDialogInput.OptionEntry> typeEntries = List.of(
                SingleOptionDialogInput.OptionEntry.create("normal", DialogUtils.mini("<white>Normal</white>"), true),
                SingleOptionDialogInput.OptionEntry.create("flat", DialogUtils.mini("<white>Flat</white>"), false),
                SingleOptionDialogInput.OptionEntry.create("large_biomes", DialogUtils.mini("<white>Large Biomes</white>"), false),
                SingleOptionDialogInput.OptionEntry.create("amplified", DialogUtils.mini("<white>Amplified</white>"), false),
                SingleOptionDialogInput.OptionEntry.create("nether", DialogUtils.mini("<white>Nether</white>"), false),
                SingleOptionDialogInput.OptionEntry.create("the_end", DialogUtils.mini("<white>The End</white>"), false)
        );

        List<DialogInput> inputs = List.of(
                DialogInput.text("name", DialogUtils.miniFromLang("dialog.creator.name_label"))
                        .width(200).labelVisible(true).initial("").maxLength(32).build(),
                DialogInput.singleOption("type", DialogUtils.miniFromLang("dialog.creator.type_label"), typeEntries)
                        .width(200).labelVisible(true).build(),
                DialogInput.text("seed", DialogUtils.miniFromLang("dialog.creator.seed_label"))
                        .width(200).labelVisible(true).initial("").maxLength(32).build(),
                DialogInput.text("generator", DialogUtils.miniFromLang("dialog.creator.generator_label"))
                        .width(200).labelVisible(true).initial("").maxLength(64).build(),
                DialogInput.bool("empty", DialogUtils.miniFromLang("dialog.creator.empty_label"), false, "true", "false")
        );

        DialogBase base = DialogBase.builder(DialogUtils.miniFromLang("dialog.creator.title"))
                .canCloseWithEscape(true)
                .inputs(inputs)
                .build();

        ActionButton confirm = DialogUtils.button(
                DialogUtils.miniFromLang("dialog.creator.confirm"),
                (response, audience) -> handleSubmit(player, response)
        );
        ActionButton cancel = DialogUtils.button(
                DialogUtils.miniFromLang("dialog.cancel"),
                (response, audience) -> MainMenuDialog.open(player)
        );

        return Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(confirm, cancel)));
    }

    private static void handleSubmit(Player player, DialogResponseView response) {
        String name = trim(response.getText("name"));
        String type = trim(response.getText("type"));
        String seed = trim(response.getText("seed"));
        String generator = trim(response.getText("generator"));
        boolean empty = Boolean.TRUE.equals(response.getBoolean("empty"));

        if (name.isEmpty()) {
            WorldManager.langConfig.sendError(player, "dialog.creator.name_required");
            DialogUtils.show(player, build(player));
            return;
        }
        if (RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            WorldManager.langConfig.sendError(player, "create.name_unusable");
            return;
        }
        if (Bukkit.getWorld(name) != null) {
            WorldManager.langConfig.sendError(player, "create.already_exists");
            return;
        }
        if (WorldFolders.listUnloadedWorldFolders().contains(name)) {
            WorldManager.langConfig.sendError(player, "create.exists_but_not_loaded", name);
            return;
        }

        if (empty) {
            createEmptyWorld(player, name, seed);
        } else {
            new Create(player).execute(name, type.isEmpty() ? null : type, seed.isEmpty() ? null : seed,
                    generator.isEmpty() ? null : generator);
        }
    }

    private static void createEmptyWorld(Player player, String name, String seed) {
        long seedValue;
        if (!seed.isEmpty()) {
            if (!seed.matches("-?\\d+")) {
                WorldManager.langConfig.sendError(player, "create.invalid_seed");
                return;
            }
            seedValue = Long.parseLong(seed);
        } else {
            seedValue = new Random().nextLong();
        }

        Create.sendStarting(player, "Empty", name, seed, "Empty");

        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .seed(seedValue)
                .generator(new EmptyWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            WorldManager.langConfig.sendError(player, "create.error", name);
            return;
        }
        WorldManager.langConfig.sendSuccess(player, "create.success", name);
        try {
            world.save();
        } catch (Exception ignored) {
        }
        WorldManager.addWorld(player, name, WorldType.FLAT.name(), World.Environment.NORMAL, "Empty");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
