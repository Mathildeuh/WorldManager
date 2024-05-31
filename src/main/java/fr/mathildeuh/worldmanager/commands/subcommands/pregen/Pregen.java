package fr.mathildeuh.worldmanager.commands.subcommands.pregen;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.WorldPreGenerator;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Pregen {
    private final JavaPlugin plugin = JavaPlugin.getPlugin(WorldManager.class);
    private final Map<String, WorldPreGenerator> generators = new HashMap<>();

    private final MessageManager message;
    private final CommandSender sender;

    public Pregen(CommandSender sender) {
        this.message = new MessageManager(sender);
        this.sender = sender;
    }

    public void execute(String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Invalid number of arguments.");
            return;
        }

        String action = args[1].toLowerCase();
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage("World \"" + worldName + "\" does not exist.");
            return;
        }

        switch (action) {
            case "start":
                handleStart(args, world);
                break;
            case "pause":
                pausePregen(sender, world);
                break;
            case "cancel":
                cancelPregen(sender, world);
                break;
            case "resume":
                resumePregen(sender, world);
                break;
            default:
                sender.sendMessage("Unknown action: " + action);
                break;
        }
    }


    private void handleStart(String[] args, World world) {
        int centerX = 0;
        int centerZ = 0;
        int radius = 20;

        if (generators.containsKey(world.getName())) {
            sender.sendMessage("Pre-generation is already running for world \"" + world.getName() + "\".");
            return;
        }

        for (int i = 3; i < args.length; i++) {
            if (args[i].startsWith("center:")) {
                String[] centerCoords = args[i].substring("center:".length()).split(",");
                if (centerCoords.length != 2) {
                    sender.sendMessage("Invalid center coordinates format.");
                    return;
                }
                try {
                    centerX = Integer.parseInt(centerCoords[0]);
                    centerZ = Integer.parseInt(centerCoords[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid center coordinates values.");
                    return;
                }
            } else if (args[i].startsWith("radius:")) {
                try {
                    radius = Integer.parseInt(args[i].substring("radius:".length()));
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid radius value.");
                    return;
                }
            }
        }

        startPregen(sender, world, centerX, centerZ, radius);
    }

    private void startPregen(CommandSender sender, World world, int centerX, int centerZ, int radius) {
        if (generators.containsKey(world.getName())) {
            sender.sendMessage("Pre-generation is already running for world \"" + world.getName() + "\".");
            return;
        }
        WorldPreGenerator generator = new WorldPreGenerator(plugin, world, centerX, centerZ, radius);
        generators.put(world.getName(), generator);
        generator.start();
        sender.sendMessage("Started pre-generation for world \"" + world.getName() + "\" with center (" + centerX + "," + centerZ + ") and radius " + radius + ".");
    }

    private void cancelPregen(CommandSender sender, World world) {
        WorldPreGenerator generator = generators.remove(world.getName());
        if (generator == null) {
            sender.sendMessage("No pre-generation running for world \"" + world.getName() + "\".");
            return;
        }
        generator.cancelGeneration();
        sender.sendMessage("Canceled pre-generation for world \"" + world.getName() + "\".");
    }

    private void pausePregen(CommandSender sender, World world) {
        WorldPreGenerator generator = generators.get(world.getName());
        if (generator == null) {
            sender.sendMessage("No pre-generation running for world \"" + world.getName() + "\".");
            return;
        }
        generator.pause();
        sender.sendMessage("Paused pre-generation for world \"" + world.getName() + "\".");
    }

    private void resumePregen(CommandSender sender, World world) {
        WorldPreGenerator generator = generators.get(world.getName());
        if (generator == null) {
            sender.sendMessage("No pre-generation running for world \"" + world.getName() + "\".");
            return;
        }
        generator.resume();
        sender.sendMessage("Resumed pre-generation for world \"" + world.getName() + "\".");
    }



}
