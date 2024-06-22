package fr.mathildeuh.worldmanager.commands.subcommands.pregenerator;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static fr.mathildeuh.worldmanager.commands.WorldManagerCommand.generator;

public class Pregen {
    private final CommandSender sender;
    private MessageManager message;


    public Pregen(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String[] args) {
        this.message = new MessageManager(sender);

        if (!(sender instanceof Player player)) return;

        if (args.length < 3) {
            message.parse("&c/wm pregen start|stop|pause|resume <world> [center:x,z] [radius:n]");
            return;
        }

        String action = args[1].toLowerCase();
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            WorldManager.langConfig.sendFormat(sender, "pregen.worldNotFound", worldName);
            return;
        }

        switch (action) {
            case "start" -> handleStart(args, player, world);
            case "stop" -> handleStop(world);
            case "pause" -> handlePause(world);
            case "resume" -> handleResume(world);
            default -> message.parse("Unknown action: " + action);
        }
    }

    private void handleStart(String[] args, Player player, World world) {
        int centerX = 0;
        int centerZ = 0;
        int totalChunks = 200;


        if (generator != null) {
            WorldManager.langConfig.sendFormat(sender, "pregen.alreadyRunning", world.getName());
            return;
        }
        for (int i = 3; i < args.length; i++) {
            if (args[i].startsWith("center:")) {
                String[] centerCoords = args[i].substring("center:".length()).split(",");
                if (centerCoords.length != 2) {
                    WorldManager.langConfig.sendFormat(sender, "pregen.invalidCenter");
                    return;
                }
                try {
                    centerX = Integer.parseInt(centerCoords[0]);
                    centerZ = Integer.parseInt(centerCoords[1]);
                } catch (NumberFormatException e) {
                    WorldManager.langConfig.sendFormat(sender, "pregen.invalidCenter");
                    return;
                }
            } else if (args[i].startsWith("radius:")) {
                try {
                    totalChunks = Integer.parseInt(args[i].substring("radius:".length()));
                } catch (NumberFormatException e) {
                    WorldManager.langConfig.sendFormat(sender, "pregen.invalidRadius");
                    return;
                }
            }
        }

        generator = new ChunkGenerator(world, player, totalChunks, new Location(world, centerX, 0, centerZ));
        generator.start();
        WorldManager.langConfig.sendFormat(sender, "pregen.start", world.getName(), centerX, centerZ, totalChunks);
    }

    private void handleStop(World world) {

        if (generator == null) {
            WorldManager.langConfig.sendFormat(sender, "pregen.notRunning", world.getName());
            return;
        }
        generator.stop();
        generator = null;
    }

    private void handlePause(World world) {

        if (generator == null) {
            WorldManager.langConfig.sendFormat(sender, "pregen.notRunning", world.getName());
            return;
        }

        generator.pause();
        WorldManager.langConfig.sendFormat(sender, "pregen.pause", world.getName());
    }

    private void handleResume(World world) {

        if (generator == null) {
            WorldManager.langConfig.sendFormat(sender, "pregen.notRunning", world.getName());
            return;
        }
        generator.resume();
        WorldManager.langConfig.sendFormat(sender, "pregen.resume", world.getName());
    }
}
