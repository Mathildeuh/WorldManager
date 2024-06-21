package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import org.bukkit.entity.Player;

public enum GUIList {
    MAIN,
    CREATOR,
    EDITOR,
    LOADER;
    private ChestGui gui;

    public void open(Player player) {
        this.update();
        if (gui != null) {
            gui.show(player);
        } else {
            player.sendMessage("GUI is not initialized.");
        }
    }

    public ChestGui getGui() {
        return gui;
    }

    public void update() {
        switch (this) {
            case MAIN:
                this.gui = new GUISManager().getGui();
                break;
            case CREATOR:
                this.gui = new GUISManager.Creator().getGui();
                break;
            case EDITOR:
                this.gui = new GUISManager.Editor().getGui();
                break;
            case LOADER:
                this.gui = new GUISManager.Loader().getGui();
                break;
        }
    }

}
