package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Teleport {

    private final CommandSender sender;

    public Teleport(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String... args) {
        if (args.length < 2) {
            new MessageManager(sender).sendHelp();
            return;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            WorldManager.langConfig.sendError(sender, "teleport.world_not_found", worldName);
            return;
        }

        Player targetPlayer = null;
        if (args.length > 2) {
            String playerName = args[2];
            targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                WorldManager.langConfig.sendError(sender, "teleport.player_not_found");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                WorldManager.langConfig.sendError(sender, "teleport.console_need_player");
                return;
            }
            targetPlayer = (Player) sender;
        }

        targetPlayer.teleport(targetWorld.getSpawnLocation());
        WorldManager.langConfig.sendSuccess(sender, "teleport.success", targetPlayer.getName(), targetWorld.getName());

    }
}
