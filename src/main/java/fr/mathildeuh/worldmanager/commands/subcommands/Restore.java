package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.BackupConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Restore {
    private final CommandSender sender;

    public Restore(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String world) {
        if (!(sender instanceof Player player)) {
            WorldManager.langConfig.sendError(sender, "general.players_only");
            return;
        }
        BackupConfig.restoreWorld(player, world);
    }

}
