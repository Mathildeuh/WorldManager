package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Teleport {

    private final CommandSender sender;
    private final MessageManager message;

    public Teleport(CommandSender sender) {
        this.sender = sender;
        this.message = new MessageManager(sender);
    }

    public void execute(String... args) {
        if (args.length < 2) {
            message.sendHelp();
            return;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            WorldManager.langConfig.sendFormat(sender, "teleport.worldNotFound", worldName);
            message.parse("The specified world does not exist.");
            return;
        }

        Player targetPlayer = null;
        if (args.length > 2) {
            String playerName = args[2];
            targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                WorldManager.langConfig.sendFormat(sender, "teleport.playerNotFound");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                WorldManager.langConfig.sendFormat(sender, "teleport.errorFromConsole");
                return;
            }
            targetPlayer = (Player) sender;
        }

        targetPlayer.teleport(targetWorld.getSpawnLocation());
        WorldManager.langConfig.sendFormat(sender, "teleport.teleportSuccess", targetPlayer.getName(), targetWorld.getName());

    }
}
