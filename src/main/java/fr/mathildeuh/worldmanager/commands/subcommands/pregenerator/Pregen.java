package fr.mathildeuh.worldmanager.commands.subcommands.pregenerator;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import fr.mathildeuh.worldmanager.util.WorldOperationLock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Pregen {
    private final CommandSender sender;


    public Pregen(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String[] args) {

        if (!(sender instanceof Player player)) {
            WorldManager.langConfig.sendError(sender, "general.players_only");
            return;
        }

        if (args.length < 3) {
            sendGuide();
            return;
        }

        String action = args[1].toLowerCase();
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            WorldManager.langConfig.sendError(sender, "pregen.world_not_found", worldName);
            return;
        }

        switch (action) {
            case "start" -> handleStart(args, player, world);
            case "stop" -> handleStop(world);
            case "pause" -> handlePause(world);
            case "resume" -> handleResume(world);
            default -> {
                WorldManager.langConfig.sendError(sender, "pregen.unknown_action", action);
                sendGuide();
            }
        }
    }

    /** Guided explanation, shown for bare {@code /wm pregen} or an unrecognized action. */
    private void sendGuide() {
        for (String line : WorldManager.langConfig.getStringList("pregen.guide")) {
            MessageUtils.sendMini(sender, line);
        }
    }

    private void handleStart(String[] args, Player player, World world) {
        int centerX = 0;
        int centerZ = 0;
        int radius = 7; // (2*7+1)^2 = 225 chunks, close to the old flat default of 200

        if (WorldManagerCommand.activeGenerators.containsKey(world.getName())) {
            WorldManager.langConfig.sendError(sender, "pregen.already_running", world.getName());
            return;
        }
        for (int i = 3; i < args.length; i++) {
            if (args[i].startsWith("center:")) {
                String[] centerCoords = args[i].substring("center:".length()).split(",");
                if (centerCoords.length != 2) {
                    WorldManager.langConfig.sendError(sender, "pregen.invalid_center");
                    return;
                }
                try {
                    centerX = Integer.parseInt(centerCoords[0]);
                    centerZ = Integer.parseInt(centerCoords[1]);
                } catch (NumberFormatException e) {
                    WorldManager.langConfig.sendError(sender, "pregen.invalid_center");
                    return;
                }
            } else if (args[i].startsWith("radius:")) {
                try {
                    radius = Integer.parseInt(args[i].substring("radius:".length()));
                    if (radius <= 0) {
                        WorldManager.langConfig.sendError(sender, "pregen.invalid_radius");
                        return;
                    }
                } catch (NumberFormatException e) {
                    WorldManager.langConfig.sendError(sender, "pregen.invalid_radius");
                    return;
                }
            }
        }

        // Pregen holds this world exclusively for its whole run - a concurrent backup/restore/
        // unload/delete on the same world would otherwise race against chunks being written.
        if (!WorldOperationLock.tryLock(world.getName())) {
            WorldManager.langConfig.sendError(sender, "general.operation_in_progress", world.getName());
            return;
        }

        ChunkGenerator generator = new ChunkGenerator(world, player, radius, new Location(world, centerX, 0, centerZ));
        generator.start();
        WorldManager.langConfig.sendWaiting(sender, "pregen.start", world.getName(), centerX, centerZ, radius);
    }

    private void handleStop(World world) {
        ChunkGenerator generator = WorldManagerCommand.activeGenerators.get(world.getName());
        if (generator == null) {
            WorldManager.langConfig.sendError(sender, "pregen.not_running", world.getName());
            return;
        }
        generator.stop();
    }

    private void handlePause(World world) {
        ChunkGenerator generator = WorldManagerCommand.activeGenerators.get(world.getName());
        if (generator == null) {
            WorldManager.langConfig.sendError(sender, "pregen.not_running", world.getName());
            return;
        }

        generator.pause();
        WorldManager.langConfig.sendSuccess(sender, "pregen.pause", world.getName());
    }

    private void handleResume(World world) {
        ChunkGenerator generator = WorldManagerCommand.activeGenerators.get(world.getName());
        if (generator == null) {
            WorldManager.langConfig.sendError(sender, "pregen.not_running", world.getName());
            return;
        }
        generator.resume();
        WorldManager.langConfig.sendSuccess(sender, "pregen.resume", world.getName());
    }
}
