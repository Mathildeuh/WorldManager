package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;


public class Create {
    static CommandSender sender;

    public Create(CommandSender sender) {
        this.sender = sender;
    }

    public void run(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        long seedValue = 0L;
        if (seed != null && seed.matches("-?\\d+")) {
            seedValue = Long.parseLong(seed);
        } else if (seed != null && !seed.isEmpty()) {
            WorldManager.langConfig.sendFormat(sender, "create.invalidSeed");

            return;
        }

        Object worldTypeOrEnvironment = getWorldType(type);

        if (worldTypeOrEnvironment instanceof WorldType) {
            sendStarting(type, name, seed, generator);
            createWorld(sender, name, World.Environment.NORMAL, (WorldType) worldTypeOrEnvironment, seedValue, generator);
        } else if (worldTypeOrEnvironment instanceof World.Environment) {
            sendStarting(type, name, seed, generator);
            createWorld(sender, name, (World.Environment) worldTypeOrEnvironment, WorldType.NORMAL, seedValue, generator);
        } else {
            WorldManager.langConfig.sendFormat(sender, "create.invalidDimensionType");

            WorldManager.langConfig.sendFormat(sender, "create.availableDimensions");

            for (World.Environment env : World.Environment.values()) {
                if (env != World.Environment.CUSTOM && env != World.Environment.NORMAL) {
                    WorldManager.langConfig.sendFormat(sender, "create.dimensionList", env.name().toLowerCase());
                }
            }

            for (WorldType worldType : WorldType.values()) {
                WorldManager.langConfig.sendFormat(sender, "create.dimensionList", worldType.name().toLowerCase());
            }
        }

    }

    public static void sendStarting(String type, String name, String seed, String generator) {
        WorldManager.langConfig.sendFormat(sender, "create.startingWorldCreation");
        WorldManager.langConfig.sendFormat(sender, "create.worldRecap.name", name);


        String t = (type != null && !type.isEmpty()) ? type : "default";
        WorldManager.langConfig.sendFormat(sender, "create.worldRecap.type", t);

        String s = (seed != null && !seed.isEmpty()) ? seed : "random";
        WorldManager.langConfig.sendFormat(sender, "create.worldRecap.seed", s);

        String g = (generator != null && !generator.isEmpty()) ? generator : "default";
        WorldManager.langConfig.sendFormat(sender, "create.worldRecap.generator", g);
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
        assert environment != null;
        WorldCreator creator = new WorldCreator(name).environment(environment).type(type);
        if (seed != null) {
            creator.seed(seed);
        }
        if (generator != null) {
            try {
                creator.generator(generator);
            } catch (NoSuchElementException e) {
                WorldManager.langConfig.sendFormat(sender, "create.generatorNotFound", generator);
                return;
            }
        }

        World world = creator.createWorld();
        if (world == null) {
            WorldManager.langConfig.sendFormat(sender, "create.worldCreationError", name);
        } else {
            WorldManager.langConfig.sendFormat(sender, "create.worldCreationSuccess", name);
            world.save();
            WorldManager.addWorld(player, creator.name(), creator.type().name(), creator.environment(), generator);
        }
    }


    public void execute(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        if (Bukkit.getWorld(name) != null) {
            WorldManager.langConfig.sendFormat(sender, "create.worldAlreadyExists");
            return;
        }

        if (name.equalsIgnoreCase("plugins") || name.equalsIgnoreCase("logs") || name.equalsIgnoreCase("libraries") || name.equalsIgnoreCase("versions") || name.equalsIgnoreCase("config") || name.equalsIgnoreCase("cache")) {
            WorldManager.langConfig.sendFormat(sender, "create.worldNameCantBeUsed");
            return;
        }

        if (getUnloadedWorlds().contains(name)) {
            WorldManager.langConfig.sendFormat(sender, "create.worldExistButNotLoaded", name);
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
        List<String> unloadedWorlds = new ArrayList<>();
        List<String> loadedWorldNames = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            loadedWorldNames.add(world.getName());
        }

        File[] worldFolders = Bukkit.getServer().getWorldContainer().listFiles();
        if (worldFolders != null) {
            for (File worldFolder : worldFolders) {
                if (worldFolder.isDirectory() && containsLevelDat(worldFolder) && !loadedWorldNames.contains(worldFolder.getName())) {
                    unloadedWorlds.add(worldFolder.getName());
                }
            }
        }

        return unloadedWorlds;
    }

    private boolean containsLevelDat(File folder) {
        File levelDat = new File(folder, "level.dat");
        return levelDat.exists();
    }
}
