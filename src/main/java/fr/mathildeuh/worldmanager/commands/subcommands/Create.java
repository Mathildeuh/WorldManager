package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;
import java.util.Locale;

public class Create {
    private final CommandSender sender;
    private final MessageManager message;

    public Create(CommandSender sender) {
        this.message = new MessageManager(sender);
        this.sender = sender;
    }

    public void run(String name) {
        message.create(name);
        createWorld(name, World.Environment.NORMAL, WorldType.NORMAL, null, null);
    }

    public void run(String name, String type) {
        run(name, type, null, null);
    }

    public void run(String name, String type, @Nullable String seed) {
        run(name, type, seed, null);
    }

    public void run(String name, String type, @Nullable String seed, @Nullable String generator) {
        message.create(name + ", " + type + ", " + seed + ", " + generator);
        Object worldTypeOrEnvironment = getWorldType(type);
        long seedValue = seed != null ? Long.parseLong(seed) : 0L;

        if (worldTypeOrEnvironment instanceof WorldType) {
            createWorld(name, World.Environment.NORMAL, (WorldType) worldTypeOrEnvironment, seedValue, generator);
        } else if (worldTypeOrEnvironment instanceof World.Environment) {
            createWorld(name, (World.Environment) worldTypeOrEnvironment, WorldType.NORMAL, seedValue, generator);
        } else {
            sender.sendMessage("Invalid world type.");
            sender.sendMessage("Available types: ");
            for (World.Environment env : World.Environment.values()) {
                sender.sendMessage(env.toString());
            }
            for (WorldType worldType : WorldType.values()) {
                sender.sendMessage(worldType.toString());
            }
        }
    }

    private Object getWorldType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "normal" -> WorldType.NORMAL;
            case "flat", "amplified", "large_biomes" -> WorldType.getByName(type.toUpperCase(Locale.ROOT));
            case "the_end", "end", "ender" -> World.Environment.THE_END;
            case "the_nether", "nether" -> World.Environment.NETHER;
            default -> World.Environment.NORMAL;
        };
    }


    public void createWorld(String name, World.Environment environment, WorldType type, @Nullable Long seed, @Nullable String generator) {
        WorldCreator creator = new WorldCreator(name).environment(environment).type(type);
        if (seed != null) {
            creator.seed(seed);
        }
        if (generator != null) {
            creator.generator(generator);
        }

        World world = creator.createWorld();
        if (world == null) {
            sender.sendMessage("World creation failed for " + name + ".");
        } else {
            sender.sendMessage("World " + name + " created successfully!");
            world.save();
        }
    }

    public void execute(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        if (Bukkit.getWorld(name) != null) {
            sender.sendMessage("This world already exists.");
            return;
        }

        if (name.equalsIgnoreCase("plugins") || name.equalsIgnoreCase("logs") || name.equalsIgnoreCase("libraries") || name.equalsIgnoreCase("versions") || name.equalsIgnoreCase("config") || name.equalsIgnoreCase("cache")) {
            sender.sendMessage("This world name can't be used.");
            return;
        }

        if (type == null) {
            run(name);
        } else if (seed == null) {
            run(name, type);
        } else {
            run(name, type, seed, generator);
        }
    }
}
