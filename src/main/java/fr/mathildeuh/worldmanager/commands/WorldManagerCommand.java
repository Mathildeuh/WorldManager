package fr.mathildeuh.worldmanager.commands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.*;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WorldManagerCommand implements CommandExecutor, TabCompleter {
    WorldManager plugin;

    public WorldManagerCommand(WorldManager pluginClass) {
        plugin = pluginClass;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String arg, String[] args) {
        MessageManager message = new MessageManager(sender);
        if (args.length == 0) {
            message.help();
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {

                new Lists(sender).execute();
                return true;
            }
        }
        if (args.length >= 2) {
            String name = args[1];
            String type;
            switch (args[0].toLowerCase()) {
                case "c":
                case "create":
                    type = (args.length >= 3) ? args[2] : null;
                    String seed = (args.length >= 4) ? args[3] : null;
                    String gen = (args.length >= 5) ? args[4] : null;
                    new Create(sender).execute(name, type, seed, gen);
                    break;
                case "del":
                case "delete":
                    new Delete(sender).execute(name);
                    break;
                case "l":
                case "load":
                case "import":
                    type = (args.length >= 3) ? args[2] : null;
                    new Load(sender).execute(name, type);
                    break;
                case "u":
                case "unload":
                    new Unload(sender).execute(name);
                    break;
                case "tp":
                case "teleport":
                    new Teleport(sender).execute(args);
                    break;
                case "help":
                default:
                    message.help();
                    break;
            }
        } else {
            message.help();
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] commands = {"create", "delete", "load", "unload", "teleport", "list"};
            String partialName = args[0].toLowerCase();

            // Ajout des commandes correspondantes
            for (String cmd : commands) {
                if (cmd.contains(partialName)) {
                    completions.add(cmd);
                }
            }
            return completions;
        } else if (args.length == 2) {
            if (!(args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("c")))
                for (World world : Bukkit.getWorlds()) {
                    completions.add(world.getName());
                }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("load")) {
                for (World.Environment env2 : World.Environment.values()) {
                    completions.add(env2.name().toLowerCase());
                    completions.remove("custom");
                }
                return completions;
            }
            String partialName = args[2].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().contains(partialName)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
