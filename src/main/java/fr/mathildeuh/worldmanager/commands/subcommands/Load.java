package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;

public class Load {
    private final MessageManager message;

    public Load(CommandSender sender) {
        this.message = new MessageManager(sender);
    }

    public void execute(String worldName, String dimension, String generator) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            message.parse(MessageManager.MessageType.ERROR, "The world \"" + worldName + "\" is already loaded.");
            return;
        }

        if (dimension == null || dimension.isEmpty()) {
            message.help();
            return;
        }

        WorldCreator worldCreator = new WorldCreator(worldName);

        Environment env = getEnvironment(dimension);
        if (env == null) {
            message.parse(MessageManager.MessageType.ERROR, "Invalid dimension types !");
            message.parse(MessageManager.MessageType.CUSTOM, "<color:#19cdff>Available dimension types:</color>");
            for (Environment env2 : Environment.values()) {
                if (env2 != Environment.CUSTOM)
                    message.parse(MessageManager.MessageType.CUSTOM, "<color:#19cdff> <color:#7471b0>➥</color> " + env2.toString().toLowerCase() + "</color>");
            }
            return;
        } else {
            worldCreator.environment(env);
        }

        world = Bukkit.createWorld(worldCreator);

        if (world != null) {
            message.parse(MessageManager.MessageType.SUCCESS, "World \"" + worldName + "\" loaded successfully!");
            WorldManager.addWorld(worldCreator.name(), worldCreator.type().name(), worldCreator.environment(), generator);

        } else {
            message.parse(MessageManager.MessageType.ERROR, "Failed loading world \"" + worldName + "\".");
        }
    }


    private Environment getEnvironment(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "normal" -> Environment.NORMAL;
            case "nether" -> Environment.NETHER;
            case "the_end", "end" -> Environment.THE_END;
            default -> null;
        };
    }
}
