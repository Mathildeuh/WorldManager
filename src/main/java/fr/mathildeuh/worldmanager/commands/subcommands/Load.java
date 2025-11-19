package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;

public class Load {
    CommandSender sender;

    public Load(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String worldName, String dimension, String generator) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            WorldManager.langConfig.sendError(sender, "load.already_loaded", worldName);
            return;
        }

        if (dimension == null || dimension.isEmpty()) {
            new MessageManager(sender).sendHelp(); // TODO: Help message configurable
            return;
        }

        WorldCreator worldCreator = new WorldCreator(worldName);

        Environment env = getEnvironment(dimension);
        if (env == null) {
            WorldManager.langConfig.sendError(sender, "load.invalid_dimension");
            WorldManager.langConfig.sendWaiting(sender, "load.available_dimensions");
            for (Environment env2 : Environment.values()) {
                if (env2 != Environment.CUSTOM)
                    WorldManager.langConfig.sendWaiting(sender, "load.dimension_list", env2.toString().toLowerCase());
            }
            return;
        } else {
            worldCreator.environment(env);
        }

        world = Bukkit.createWorld(worldCreator);

        if (world != null) {

            WorldManager.langConfig.sendSuccess(sender, "load.success", worldName);
            WorldManager.addWorld(sender, worldCreator.name(), worldCreator.type().name(), worldCreator.environment(), generator);

        } else {
            WorldManager.langConfig.sendError(sender, "load.failed", worldName);
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
