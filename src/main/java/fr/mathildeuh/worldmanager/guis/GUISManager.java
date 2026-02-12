package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * GUISManager - Gestionnaire des interfaces graphiques principales
 *
 * Cette classe a été refactorisée pour ne contenir que la GUI principale.
 * Les sous-GUIs sont maintenant dans leurs propres classes :
 * - CreatorGUI.java : Interface de création de mondes
 * - EditorGUI.java : Interface d'édition de mondes
 * - LoaderGUI.java : Interface de chargement de mondes
 * - EditorOptionsGUI.java : Options d'édition
 * - GameRuleEditorGUI.java : Éditeur de game rules
 */
public class GUISManager {
    private final ChestGui gui;

    public GUISManager() {
        gui = new ChestGui(3, "World Manager - GUI");
        StaticPane pane = new StaticPane(0, 0, 9, 3);
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // Create world button
        ItemBuilder grass = new ItemBuilder(Material.GRASS_BLOCK)
                .name("&2&nCreate a world")
                .lore("", "&7&oCreate a new fully customizable world");
        pane.addItem(new GuiItem(grass.build(), event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.CREATOR.open(player);
            }
        }), Slot.fromIndex(11));

        // Edit world button
        ItemBuilder cmd = new ItemBuilder(Material.COMMAND_BLOCK)
                .name("&2&nEdit your worlds")
                .lore("", "&7&oManage your loaded worlds");
        pane.addItem(new GuiItem(cmd.build(), event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.EDITOR.open(player);
            }
        }), Slot.fromIndex(13));

        // Load world button
        ItemBuilder struct = new ItemBuilder(Material.STRUCTURE_VOID)
                .name("&2&nLoad a world")
                .lore("", "&7&oLoad a world that is", "&7&oin server folders");
        pane.addItem(new GuiItem(struct.build(), event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.LOADER.open(player);
            }
        }), Slot.fromIndex(15));

        gui.addPane(pane);
    }

    public ChestGui getGui() {
        return gui;
    }
}

