package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            WorldManager.langConfig.sendError(sender, "teleport.usage");
            for (World world : Bukkit.getWorlds()) {
                MessageUtils.sendMini(sender, WorldManager.langConfig.formatMessage("list.world_item", world.getName()));
            }
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

        Location spawn = WorldsConfig.getSpawn(targetWorld);
        targetPlayer.teleportAsync(spawn != null ? spawn : targetWorld.getSpawnLocation());
        WorldManager.langConfig.sendSuccess(sender, "teleport.success", targetPlayer.getName(), targetWorld.getName());

    }
}
