package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.BackupConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Backup {
    private final CommandSender sender;

    public Backup(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String world) {
        if (!(sender instanceof Player player)) {
            WorldManager.langConfig.sendError(sender, "general.players_only");
            return;
        }
        BackupConfig.backupWorld(player, world);
    }

}
