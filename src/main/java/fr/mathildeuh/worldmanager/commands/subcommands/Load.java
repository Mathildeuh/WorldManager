package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
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
        if (worldName == null || worldName.isBlank()) {
            sendUsageWithUnloadedWorlds();
            return;
        }
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

        SchedulerUtil.runGlobal(() -> {
            World loadedWorld = Bukkit.createWorld(worldCreator);
            if (loadedWorld != null) {
                WorldsConfig.applyFlags(loadedWorld);
                WorldManager.langConfig.sendSuccess(sender, "load.success", worldName);
                WorldManager.addWorld(sender, worldCreator.name(), worldCreator.type().name(), worldCreator.environment(), generator);
            } else {
                WorldManager.langConfig.sendError(sender, "load.failed", worldName);
            }
        });
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

    /** Missing world name: point at the usage, and list what's actually loadable right now. */
    private void sendUsageWithUnloadedWorlds() {
        WorldManager.langConfig.sendError(sender, "load.usage");
        java.util.List<String> unloaded = fr.mathildeuh.worldmanager.util.WorldFolders.listUnloadedWorldFolders();
        if (unloaded.isEmpty()) {
            return;
        }
        WorldManager.langConfig.sendWaiting(sender, "load.unloaded_header");
        for (String name : unloaded) {
            WorldManager.langConfig.sendWaiting(sender, "load.unloaded_item", name);
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
