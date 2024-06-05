package fr.mathildeuh.worldmanager.commands.subcommands.pregen;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Pregen {
    private final JavaPlugin plugin = JavaPlugin.getPlugin(WorldManager.class);
    public static final Map<String, WorldPreGenerator> generators = new HashMap<>();
    private final MessageManager message;
    private final CommandSender sender;
    Player player;

    public Pregen(CommandSender sender) {
        this.message = new MessageManager(sender);
        this.sender = sender;
        if (sender instanceof Player) {
            this.player = (Player) sender;
        }
    }

    public void execute(String[] args) {
        if (args.length < 3) {
            message.parse(MessageManager.MessageType.ERROR, "Invalid number of arguments.");
            return;
        }

        String action = args[1].toLowerCase();
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            message.parse(MessageManager.MessageType.ERROR, "World \"" + worldName + "\" does not exist.");
            return;
        }

        if (action.equals("start")) {
            handleStart(args, player, world);
        } else {
            message.parse(MessageManager.MessageType.ERROR, "Unknown action: " + action);
        }
    }

    private void handleStart(String[] args, Player player, World world) {
        int centerX = 0;
        int centerZ = 0;
        int radius = 20;

        if (generators.containsKey(world.getName())) {
            message.parse(MessageManager.MessageType.ERROR, "Pre-generation is already running for world \"" + world.getName() + "\".");
            return;
        }

        for (int i = 3; i < args.length; i++) {
            if (args[i].startsWith("center:")) {
                String[] centerCoords = args[i].substring("center:".length()).split(",");
                if (centerCoords.length != 2) {
                    message.parse(MessageManager.MessageType.ERROR, "Invalid center coordinates format.");
                    return;
                }
                try {
                    centerX = Integer.parseInt(centerCoords[0]);
                    centerZ = Integer.parseInt(centerCoords[1]);
                } catch (NumberFormatException e) {
                    message.parse(MessageManager.MessageType.ERROR, "Invalid center coordinates values.");
                    return;
                }
            } else if (args[i].startsWith("radius:")) {
                try {
                    radius = Integer.parseInt(args[i].substring("radius:".length()));
                } catch (NumberFormatException e) {
                    message.parse(MessageManager.MessageType.ERROR, "Invalid radius value.");
                    return;
                }
            }
        }

        startPregen(world, player, centerX, centerZ, radius);
    }

    private void startPregen(World world, Player player, int centerX, int centerZ, int radius) {
        if (generators.containsKey(world.getName())) {
            message.parse(MessageManager.MessageType.ERROR, "Pre-generation is already running for world \"" + world.getName() + "\".");
            return;
        }

        if (!(sender instanceof Player)) {
            message.parse(MessageManager.MessageType.ERROR, "Only players can start pre-generation.");
            return;
        }

        WorldPreGenerator generator = new WorldPreGenerator(plugin, player, world, centerX, centerZ, radius);
        generators.put(world.getName(), generator);
        generator.start();
        message.parse(MessageManager.MessageType.SUCCESS, "Started pre-generation for world \"" + world.getName() + "\" with center (" + centerX + "," + centerZ + ") and radius " + radius + ".");
    }

}
