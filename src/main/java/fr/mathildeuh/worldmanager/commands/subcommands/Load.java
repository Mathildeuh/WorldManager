package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
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
        if (!WorldNameValidator.isValid(worldName)) {
            WorldManager.langConfig.sendError(sender, "general.invalid_world_name", worldName);
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            WorldManager.langConfig.sendError(sender, "load.world_already_loaded", worldName);
            return;
        }

        if (dimension == null || dimension.isEmpty()) {
            sendInvalidDimension();
            return;
        }

        Environment env = getEnvironment(dimension);
        if (env == null) {
            sendInvalidDimension();
            return;
        }

        WorldCreator worldCreator = new WorldCreator(worldName).environment(env);
        if (generator != null && !generator.isEmpty()) {
            worldCreator.generator(generator);
        }

        world = Bukkit.createWorld(worldCreator);

        if (world != null) {
            WorldManager.langConfig.sendSuccess(sender, "load.success", worldName);
            WorldManager.addWorld(sender, worldCreator.name(), worldCreator.type().name(), worldCreator.environment(), generator);
        } else {
            WorldManager.langConfig.sendError(sender, "load.failed", worldName);
        }
    }

    private void sendInvalidDimension() {
        WorldManager.langConfig.sendError(sender, "load.invalid_dimension");
        WorldManager.langConfig.sendWaiting(sender, "load.available_dimensions");
        for (Environment env : Environment.values()) {
            if (env != Environment.CUSTOM) {
                WorldManager.langConfig.sendWaiting(sender, "load.dimension_list", env.toString().toLowerCase());
            }
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
