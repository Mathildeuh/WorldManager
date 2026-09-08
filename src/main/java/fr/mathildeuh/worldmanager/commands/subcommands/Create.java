package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
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


public class Create {
    private final CommandSender sender;

    public Create(CommandSender sender) {
        this.sender = sender;
    }

    public void run(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        long seedValue;

        if (seed != null && !seed.isEmpty()) {
            if (seed.matches("-?\\d+")) {
                seedValue = Long.parseLong(seed);
            } else {
                WorldManager.langConfig.sendError(sender, "create.invalid_seed");
                return;
            }
        } else {
            // Générer une seed aléatoire si aucune n'est spécifiée
            seedValue = new Random().nextLong();
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

        WorldCreator creator = new WorldCreator(name).environment(environment).type(type);

        // Si aucune seed n'est spécifiée, en générer une aléatoire pour éviter les mondes identiques
        long seedValue = Objects.requireNonNullElseGet(seed, () -> new Random().nextLong());
        creator.seed(seedValue);

        if (generator != null) {
            try {
                creator.generator(generator);
            } catch (NoSuchElementException e) {
                WorldManager.langConfig.sendError(sender, "create.generator_not_found", generator);
                return;
            }
        }

        SchedulerUtil.runGlobal(() -> {
            World world = creator.createWorld();
            if (world == null) {
                WorldManager.langConfig.sendError(sender, "create.error", name);
            } else {
                WorldManager.langConfig.sendSuccess(sender, "create.success", name);
                try {
                    world.save();
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[WorldManager] Initial save failed for newly created world " + name + ": " + e.getMessage());
                }
                WorldManager.addWorld(player, creator.name(), creator.type().name(), creator.environment(), generator);
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

         if (type == null) {
             run(name, null, null, null);
         } else if (seed == null) {
             run(name, type, null, null);
         } else {
             run(name, type, seed, generator);
         }
     }

    private List<String> getUnloadedWorlds() {
        return fr.mathildeuh.worldmanager.util.WorldFolders.listUnloadedWorldFolders();
    }
}
