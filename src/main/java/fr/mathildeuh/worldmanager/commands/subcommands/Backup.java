package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.manager.BackupConfig;
import fr.mathildeuh.worldmanager.manager.WorldsConfig;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Backup {
    private final CommandSender sender;

    public Backup(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String world) {
        if (!(sender instanceof Player player)) return;
        BackupConfig.backupWorld(player, world);
    }

}
