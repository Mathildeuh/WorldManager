package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.gui.GUIMain;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Gui {
    CommandSender sender;

    public Gui(CommandSender sender) {
        this.sender = sender;
    }

    public void execute() {
        if (!(sender instanceof Player player)) return;
        new GUIMain(player);
    }
}
