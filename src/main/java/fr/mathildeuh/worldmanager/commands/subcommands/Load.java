package fr.mathildeuh.worldmanager.commands.subcommands;

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

    public void execute(String worldName, String dimension) {
        // Vérifier si le monde est déjà chargé
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            sender.sendMessage("The world " + worldName + " is already loaded.");
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
        if (env == null) {
            sender.sendMessage("Invalid dimension type: " + dimension);
            sender.sendMessage("Available dimension types:");
            for (Environment env2 : Environment.values()) {
                sender.sendMessage(env2.toString());
            }
            return;
        } else {
            worldCreator.environment(env);
        }

        // Charger le monde
        world = Bukkit.createWorld(worldCreator);

        // Vérifier si le monde a été chargé correctement
        if (world != null) {
            message.load(worldName);
            sender.sendMessage("World " + worldName + " loaded successfully!");
        } else {
            sender.sendMessage("Failed to load world " + worldName + ".");
        }
    }


    private Environment getEnvironment(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "normal" -> Environment.NORMAL;
            case "nether" -> Environment.NETHER;
            case "end" -> Environment.THE_END;
            default -> null;
        };
    }
}
