package fr.mathildeuh.worldmanager.commands.subcommands;

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
            message.help();
            return;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            message.parse(MessageManager.MessageType.ERROR, "The specified world does not exist.");
            return;
        }

        Player targetPlayer = null;
        if (args.length > 2) {
            String playerName = args[2];
            targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                message.parse(MessageManager.MessageType.ERROR, "The specified player is not online.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                message.parse(MessageManager.MessageType.ERROR, "You must specify a player when executing from the console.");
                return;
            }
            targetPlayer = (Player) sender;
        }

        targetPlayer.teleport(targetWorld.getSpawnLocation());
        message.parse(MessageManager.MessageType.SUCCESS, "Teleported " + targetPlayer.getName() + " to \"" + targetWorld.getName() + "\".");
    }
}
