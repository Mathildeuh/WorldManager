package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;

public class Load {
    private final CommandSender sender;
    private final MessageManager message;

    public Load(CommandSender sender) {
        this.sender = sender;
        this.message = new MessageManager(sender);
    }

    public void execute(String worldName, String dimension, String generator) {
        // Vérifier si le monde est déjà chargé
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>The world \"" + worldName + "\" is already loaded.</color>");
            return;
        }

        // Si aucun type de dimension n'est fourni, afficher l'aide
        if (dimension == null || dimension.isEmpty()) {
            message.help();
            return;
        }

        // Créer un WorldCreator pour le monde
        WorldCreator worldCreator = new WorldCreator(worldName);

        // Spécifier le type de dimension
        Environment env = getEnvironment(dimension);
        System.out.println(dimension);
        if (env == null) {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Invalid dimension types !</color>");
            message.parse("<color:#19cdff>Available dimension types:</color>");
            for (Environment env2 : Environment.values()) {
                if (env2 != Environment.CUSTOM)
                    message.parse("<color:#19cdff> <color:#7471b0>➥</color> " + env2.toString().toLowerCase() + "</color>");
            }
            return;
        } else {
            worldCreator.environment(env);
        }

        // Charger le monde
        world = Bukkit.createWorld(worldCreator);

        // Vérifier si le monde a été chargé correctement
        if (world != null) {
            message.parse(worldName);
            message.parse("<dark_green>✔</dark_green> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <yellow>World \"" + worldName + "\" loaded successfully!</yellow>");
            WorldManager.addWorld(worldCreator.name(), worldCreator.type().name(), worldCreator.environment(), generator);

        } else {
            message.parse("<color:#aa3e00>☠</color> <color:#7d66ff>{</color><color:#02a876>World Manager</color><color:#7d66ff>}</color> <color:#ff2e1f>Failed loading world \"" + worldName + "\".</color>");
        }
    }


    private Environment getEnvironment(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "normal" -> Environment.NORMAL;
            case "nether" -> Environment.NETHER;
            case "the_end","end" -> Environment.THE_END;
            default -> null;
        };
    }
}
