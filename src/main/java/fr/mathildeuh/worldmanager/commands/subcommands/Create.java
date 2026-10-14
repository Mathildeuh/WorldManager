package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import fr.mathildeuh.worldmanager.util.WorldLifecycle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;


public class Create {
    private final CommandSender sender;

    public Create(CommandSender sender) {
        this.sender = sender;
    }

    public void run(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        // Allow /wm create <name> <type> empty without requiring a seed placeholder.
        if ("empty".equalsIgnoreCase(seed) && (generator == null || generator.isEmpty())) {
            seed = null;
            generator = "Empty";
        }
        if ("empty".equalsIgnoreCase(generator)) generator = "Empty";

        long seedValue;
        try {
            seedValue = seed == null || seed.isEmpty() ? new Random().nextLong() : Long.parseLong(seed);
        } catch (NumberFormatException e) {
            WorldManager.langConfig.sendError(sender, "create.invalid_seed");
            return;
        }

        if ("empty".equalsIgnoreCase(type)) {
            sendStarting(sender, "empty", name, seed, "Empty");
            createWorld(sender, name, World.Environment.NORMAL, WorldType.FLAT, seedValue, "Empty");
            return;
        }

        Object worldTypeOrEnvironment = getWorldType(type);

        if (worldTypeOrEnvironment instanceof WorldType) {
            sendStarting(sender, type, name, seed, generator);
            createWorld(sender, name, World.Environment.NORMAL, (WorldType) worldTypeOrEnvironment, seedValue, generator);
        } else if (worldTypeOrEnvironment instanceof World.Environment) {
            sendStarting(sender, type, name, seed, generator);
            createWorld(sender, name, (World.Environment) worldTypeOrEnvironment, WorldType.NORMAL, seedValue, generator);
        } else {
            WorldManager.langConfig.sendError(sender, "create.invalid_dimension_type");

            WorldManager.langConfig.sendWaiting(sender, "create.available_dimensions");

            for (World.Environment env : World.Environment.values()) {
                if (env != World.Environment.CUSTOM && env != World.Environment.NORMAL) {
                    WorldManager.langConfig.sendWaiting(sender, "create.dimension_list", env.name().toLowerCase());
                }
            }

            WorldManager.langConfig.sendWaiting(sender, "create.dimension_list", "empty");
            for (WorldType worldType : WorldType.values()) {
                WorldManager.langConfig.sendWaiting(sender, "create.dimension_list", worldType.name().toLowerCase());
            }
        }

    }

    public static void sendStarting(CommandSender sender, String type, String name, String seed, String generator) {
        WorldManager.langConfig.sendWaiting(sender, "create.starting");
        WorldManager.langConfig.sendWaiting(sender, "create.recap.name", name);


        String t = (type != null && !type.isEmpty()) ? type : "default";
        WorldManager.langConfig.sendWaiting(sender, "create.recap.type", t);

        String s = (seed != null && !seed.isEmpty()) ? seed : "random";
        WorldManager.langConfig.sendWaiting(sender, "create.recap.seed", s);

        String g = (generator != null && !generator.isEmpty()) ? generator : "default";
        WorldManager.langConfig.sendWaiting(sender, "create.recap.generator", g);
    }

    private Object getWorldType(String type) {
        if (type == null) {
            return WorldType.NORMAL;
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "normal" -> {
                return WorldType.NORMAL;
            }
            case "flat", "amplified", "large_biomes" -> {
                return WorldType.getByName(type.toUpperCase(Locale.ROOT));
            }
            case "the_end", "end", "ender" -> {
                return World.Environment.THE_END;
            }
            case "the_nether", "nether" -> {
                return World.Environment.NETHER;
            }
            default -> {
                return null;
            }
        }
    }

    public void createWorld(CommandSender player, String name, @Nullable World.Environment environment, WorldType type, @Nullable Long seed, @Nullable String generator) {
        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        SchedulerUtil.runGlobal(() -> {
            try {
                // Recheck after the scheduler handoff to avoid adopting a concurrently created world.
                if (Bukkit.getWorld(name) != null) {
                    WorldManager.langConfig.sendError(sender, "create.already_exists");
                    return;
                }
                WorldCreator creator = new WorldCreator(name).environment(environment).type(type)
                        .seed(Objects.requireNonNullElseGet(seed, () -> new Random().nextLong()));
                WorldLifecycle.applyGenerator(creator, generator);
                World world = creator.createWorld();
                if (world == null) {
                    WorldManager.langConfig.sendError(sender, "create.error", name);
                } else {
                    WorldManager.addWorld(player, name, type.name(), environment, generator);
                    WorldManager.langConfig.sendSuccess(sender, "create.success", name);
                }
            } catch (NoSuchElementException e) {
                WorldManager.langConfig.sendError(sender, "create.generator_not_found", generator);
            } catch (Exception e) {
                WorldManager.getInstance().getLogger().log(Level.WARNING, "Could not create world " + name, e);
                WorldManager.langConfig.sendError(sender, "create.error", name);
            }
        });
    }


    public void execute(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        if (name == null || name.isBlank()) {
            WorldManager.langConfig.sendError(sender, "create.name_required");
            return;
        }
        if (!fr.mathildeuh.worldmanager.util.WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(sender, "general.invalid_world_name", name);
            return;
        }

        if (Bukkit.getWorld(name) != null) {
            WorldManager.langConfig.sendError(sender, "create.already_exists");
            return;
        }

        if (name.equalsIgnoreCase("plugins") || name.equalsIgnoreCase("logs") || name.equalsIgnoreCase("libraries") || name.equalsIgnoreCase("versions") || name.equalsIgnoreCase("config") || name.equalsIgnoreCase("cache")) {
            WorldManager.langConfig.sendError(sender, "create.name_unusable");
            return;
        }

        if (getUnloadedWorlds().contains(name)) {
            WorldManager.langConfig.sendError(sender, "create.exists_but_not_loaded", name);
            return;
        }

        run(name, type, seed, generator);
    }

    private List<String> getUnloadedWorlds() {
        return fr.mathildeuh.worldmanager.util.WorldFolders.listUnloadedWorldFolders();
    }
}
