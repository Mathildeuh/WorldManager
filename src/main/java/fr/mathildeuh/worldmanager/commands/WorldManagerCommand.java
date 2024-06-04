package fr.mathildeuh.worldmanager.commands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.*;
import fr.mathildeuh.worldmanager.commands.subcommands.pregen.Pregen;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
            new Gui(sender).execute();
            return true;
        }
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "list":
                    new Lists(sender).execute();
                    return true;

                case "gui":
                case "open":
                    new Gui(sender).execute();
                    break;

                default:
                    message.help();
                    break;
            }

            return true;
        }
        String name = args[1];
        String type;
        String gen;
        switch (args[0].toLowerCase()) {
            case "backup":
                new Backup(sender).execute(name);
                break;
            case "restore":
                new Restore(sender).execute(name);
                break;
            case "c":
            case "create":
                type = (args.length >= 3) ? args[2] : null;
                String seed = (args.length >= 4) ? args[3] : null;
                gen = (args.length >= 5) ? args[4] : null;
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
                gen = (args.length >= 4) ? args[3] : null;
                new Load(sender).execute(name, type, gen);
                break;
            case "u":
            case "unload":
                new Unload(sender).execute(name);
                break;
            case "tp":
            case "teleport":
                new Teleport(sender).execute(args);
                break;
            case "pregen":
                new Pregen(sender).execute(args);
                break;

            case "help":
            default:
                message.help();
                break;
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] commands = {"create", "delete", "list", "load", "teleport", "unload", "pregen", "gui", "open", "help", "open", "gui", "restore", "backup"};
            Arrays.sort(commands);
            String partialName = args[0].toLowerCase();

            for (String cmd : commands) {
                if (cmd.startsWith(partialName)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "load", "l", "import":
                    completions.addAll(getUnloadedWorlds());
                    break;

                case "delete", "teleport", "tp", "unload", "del", "u", "backup", "restore":
                    for (World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    break;
                case "pregen":
                    completions.add("start");
                default:
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "load":
                    for (World.Environment env : World.Environment.values()) {
                        if (!env.equals(World.Environment.CUSTOM)) {
                            completions.add(env.name().toLowerCase());
                        }
                    }
                    break;
                case "teleport":
                    String partialName = args[2].toLowerCase();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(partialName)) {
                            completions.add(player.getName());
                        }
                    }
                    break;
                case "pregen":
                    for (World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                default:
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("pregen") && args[1].equalsIgnoreCase("start")) {
            String partialName = args[2].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }


    public List<String> getUnloadedWorlds() {
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

    public boolean containsLevelDat(File folder) {
        File levelDat = new File(folder, "level.dat");
        return levelDat.exists();
    }


}