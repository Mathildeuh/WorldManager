package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Teleport {
    private static final String WORLD_DOES_NOT_EXIST = "The specified world does not exist.";
    private static final String PLAYER_NOT_ONLINE = "The specified player is not online.";
    private static final String MUST_SPECIFY_PLAYER = "You must specify a player when executing from the console.";
    private static final String TELEPORT_SUCCESS = "Teleported %s to world %s.";

    private final CommandSender sender;
    private final MessageManager message;

    public Teleport(CommandSender sender) {
        this.sender = sender;
        this.message = new MessageManager(sender);
    }

    public void execute(String... args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /teleport <world> [player]");
            return;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            sender.sendMessage(WORLD_DOES_NOT_EXIST);
            return;
        }

        Player targetPlayer = null;
        if (args.length > 2) {
            String playerName = args[2];
            targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(PLAYER_NOT_ONLINE);
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MUST_SPECIFY_PLAYER);
                return;
            }
            targetPlayer = (Player) sender;
        }

        targetPlayer.teleport(targetWorld.getSpawnLocation());
        sender.sendMessage(String.format(TELEPORT_SUCCESS, targetPlayer.getName(), targetWorld.getName()));
    }
}
