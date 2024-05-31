package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class Create {
    JavaPlugin plugin = JavaPlugin.getPlugin(WorldManager.class);
    private final MessageManager message;

    public Create(CommandSender sender) {
        this.message = new MessageManager(sender);
    }

    public void run(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        long seedValue = 0L;
        if (seed != null && seed.matches("\\d+")) {
            seedValue = Long.parseLong(seed);
        } else if (seed != null && !seed.isEmpty()) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Seed must be a number!</color>");
            return;
        }

        Object worldTypeOrEnvironment = getWorldType(type);

        if (worldTypeOrEnvironment instanceof WorldType) {
            sendStarting(type, name, seed, generator);

            createWorld(name, World.Environment.NORMAL, (WorldType) worldTypeOrEnvironment, seedValue, generator);
        } else if (worldTypeOrEnvironment instanceof World.Environment) {
            sendStarting(type, name, seed, generator);
            createWorld(name, (World.Environment) worldTypeOrEnvironment, WorldType.NORMAL, seedValue, generator);

        } else {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Invalid dimension types !</color>");
            message.parse("<color:#19cdff>Available dimension types:</color>");
            for (World.Environment env : World.Environment.values()) {
                if (env != World.Environment.CUSTOM && env != World.Environment.NORMAL)
                    message.parse("<color:#19cdff> <color:#7471b0>➥</color> " + env.toString().toLowerCase() + "</color>");
            }
            for (WorldType worldType : WorldType.values()) {
                message.parse("<color:#19cdff> <color:#7471b0>➥</color> " + worldType.name().toLowerCase() + "</color>");
            }
        }


    }

    public void sendStarting(String type, String name, String seed, String generator){
        message.parse("<gold>⌛</gold> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <gray>Starting world creation...</gray>");
        message.parse("<color:#19cdff> <color:#7471b0>➥</color> Name: <yellow>" + name + " </yellow></color>");

        String t = (type != null && !type.isEmpty()) ? type : "default";
        message.parse("<color:#19cdff> <color:#7471b0>➥</color> Type: <yellow>" + t + " </yellow></color>");

        String s = (seed != null && !seed.isEmpty()) ? seed : "random";
        message.parse("<color:#19cdff> <color:#7471b0>➥</color> Seed: <yellow>" + s + " </yellow></color>");

        String g = (generator != null && !generator.isEmpty()) ? generator : "default";
        message.parse("<color:#19cdff> <color:#7471b0>➥</color> Generator: <yellow>" + g + "</yellow></color>");
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

    public void createWorld(String name, @Nullable World.Environment environment, WorldType type, @Nullable Long seed, @Nullable String generator) {
        WorldCreator creator = new WorldCreator(name).environment(environment).type(type);
        if (seed != null) {
            creator.seed(seed);
        }
        if (generator != null) {
            try {
                creator.generator(generator);
            } catch (NoSuchElementException e) {
                message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>World generator \"" + generator + "\" not found.</color>");
                return;
            }
        }

        World world = creator.createWorld();
        if (world == null) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>World creation failed for \"" + name + "\".</color>");
        } else {
            message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow>World \"" + name + "\" created successfully!</yellow>");
            world.save();
            WorldManager.addWorld(creator.name(), creator.type().name(), creator.environment(), generator);
        }
    }

    public void execute(String name, @Nullable String type, @Nullable String seed, @Nullable String generator) {
        if (Bukkit.getWorld(name) != null) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>This world already exists.</color>");
            return;
        }

        if (name.equalsIgnoreCase("plugins") || name.equalsIgnoreCase("logs") || name.equalsIgnoreCase("libraries") || name.equalsIgnoreCase("versions") || name.equalsIgnoreCase("config") || name.equalsIgnoreCase("cache")) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>This world name can't be used.</color>");
            return;
        }

        if (getUnloadedWorlds().contains(name)) {
            message.parse("<click:suggest_command:'/wm load " + name + " (type)'><color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>This world already exist, click on this message to load it.</color></click>");
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
        File levelDat = new File(folder,  "level.dat");
        return levelDat.exists();
    }
}
