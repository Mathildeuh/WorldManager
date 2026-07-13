package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.dialogs.MainMenuDialog;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Gui {
    CommandSender sender;

    public Gui(CommandSender sender) {
        this.sender = sender;
    }

    public void execute() {
        if (!(sender instanceof Player player)) {
            WorldManager.langConfig.sendError(sender, "general.players_only");
            return;
        }
        MainMenuDialog.open(player);
    }
}
