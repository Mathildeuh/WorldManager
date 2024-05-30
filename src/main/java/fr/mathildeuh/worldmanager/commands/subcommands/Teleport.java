package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Teleport {
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
            message.help();
            return;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>The specified world does not exist.</color>");
            return;
        }

        Player targetPlayer = null;
        if (args.length > 2) {
            String playerName = args[2];
            targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>The specified player is not online.</color>");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>You must specify a player when executing from the console.</color>");
                return;
            }
            targetPlayer = (Player) sender;
        }

        targetPlayer.teleport(targetWorld.getSpawnLocation());
        message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow> Teleported " + targetPlayer.getName() + " to \"" + targetWorld.getName() + "\". </yellow>");
    }
}
