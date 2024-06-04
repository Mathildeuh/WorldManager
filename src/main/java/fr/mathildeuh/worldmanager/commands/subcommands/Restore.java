package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.manager.BackupConfig;
import fr.mathildeuh.worldmanager.manager.WorldsConfig;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Restore {
    private final CommandSender sender;

    public Restore(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String world) {
        if (!(sender instanceof Player player)) return;
        BackupConfig.restoreWorld(player, world);
    }

}
