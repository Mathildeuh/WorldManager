package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Delete;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import fr.mathildeuh.worldmanager.commands.subcommands.Teleport;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class EditorOptionsGUI {
    private final ChestGui gui;
    private final World currentWorld;

    public EditorOptionsGUI(World currentWorld) {
        this.currentWorld = currentWorld == null ? Bukkit.getWorlds().get(0) : currentWorld;
        this.gui = createGui();
    }

    private ChestGui createGui() {
        ChestGui gui = new ChestGui(3, "Options for " + currentWorld.getName());

        List<String> disabledLore = List.of("", "&4&nThis feature is disabled on default world");
        boolean isDefaultWorld = currentWorld == Bukkit.getWorlds().get(0);

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        // Game Rules button
        pane.addItem(new GuiItem(new ItemBuilder(Material.COMMAND_BLOCK).name("&c&nGame Rules").build(), event -> {
            if (event.getWhoClicked() instanceof Player player) {
                MessageUtils.sendMini(player, "<gray>></gray> <yellow>Opening game rules editor...</yellow>");
                GameRuleEditorGUI editor = new GameRuleEditorGUI(currentWorld);
                editor.getGui().show(player);
            }
        }), Slot.fromIndex(3));

        // Unload button
        pane.addItem(new GuiItem(
            new ItemBuilder(Material.STRUCTURE_BLOCK)
                .name("&e&nUnload")
                .lore(isDefaultWorld ? disabledLore : List.of(""))
                .build(),
            event -> {
                if (!isDefaultWorld) {
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.UNLOAD).show(event.getWhoClicked());
                }
            }
        ), Slot.fromIndex(5));

        // Teleport button
        pane.addItem(new GuiItem(new ItemBuilder(Material.ENDER_PEARL).name("&d&nTeleport").build(), event -> {
            Player player = (Player) event.getWhoClicked();
            new Teleport(player).execute(player.getName(), currentWorld.getName());
        }), Slot.fromIndex(11));

        // Delete button
        pane.addItem(new GuiItem(
            new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&4&nDelete")
                .lore(isDefaultWorld ? disabledLore : List.of(""))
                .build(),
            event -> {
                if (!isDefaultWorld) {
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.DELETE).show(event.getWhoClicked());
                }
            }
        ), Slot.fromIndex(15));

        // Backup button
        pane.addItem(new GuiItem(
            new ItemBuilder(Material.EMERALD)
                .name("&a&nBackup")
                .lore(isDefaultWorld ? disabledLore : List.of(""))
                .build(),
            event -> {
                if (!isDefaultWorld) {
                    new Backup(event.getWhoClicked()).execute(currentWorld.getName());
                }
            }
        ), Slot.fromIndex(21));

        // Restore button
        pane.addItem(new GuiItem(
            new ItemBuilder(Material.DIAMOND)
                .name("&b&nRestore")
                .lore(isDefaultWorld ? disabledLore : List.of(""))
                .build(),
            event -> {
                if (!isDefaultWorld) {
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.RESTORE).show(event.getWhoClicked());
                }
            }
        ), Slot.fromIndex(23));

        // Back button
        pane.addItem(new GuiItem(new ItemBuilder(Material.DARK_OAK_DOOR).name("&c&nBack").build(), event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.EDITOR.open(player);
            }
        }), Slot.fromIndex(26));

        gui.addPane(pane);

        return gui;
    }

    public ChestGui getGui() {
        return this.gui;
    }

    private ChestGui confirmationGui(Player player, ConfirmTypes types) {
        ChestGui gui = new ChestGui(1, "Confirmation for " + currentWorld.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 1);

        switch (types) {
            case DELETE:
                pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK).name("&4&nDelete").build(), event -> {
                    new Delete(player).execute(currentWorld.getName());
                    new EditorOptionsGUI(currentWorld).getGui().show(player);
                }), Slot.fromIndex(4));
                break;
            case UNLOAD:
                pane.addItem(new GuiItem(new ItemBuilder(Material.STRUCTURE_BLOCK).name("&e&nUnload").build(), event -> {
                    new Unload(player).execute(currentWorld.getName());
                    new EditorOptionsGUI(currentWorld).getGui().show(player);
                }), Slot.fromIndex(4));
                break;
            case RESTORE:
                pane.addItem(new GuiItem(new ItemBuilder(Material.DIAMOND).name("&b&nRestore").build(), event -> {
                    new Restore(player).execute(currentWorld.getName());
                    new EditorOptionsGUI(currentWorld).getGui().show(player);
                }), Slot.fromIndex(4));
                break;
        }

        pane.addItem(new GuiItem(new ItemBuilder(Material.DARK_OAK_DOOR).name("&c&nBack").build(), event ->
            new EditorOptionsGUI(currentWorld).getGui().show(player)
        ), Slot.fromIndex(8));

        gui.addPane(pane);
        return gui;
    }

    public enum ConfirmTypes {
        DELETE,
        UNLOAD,
        RESTORE
    }
}

