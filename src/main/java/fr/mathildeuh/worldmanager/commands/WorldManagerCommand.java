package fr.mathildeuh.worldmanager.commands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.*;
import fr.mathildeuh.worldmanager.commands.subcommands.pregenerator.ChunkGenerator;
import fr.mathildeuh.worldmanager.commands.subcommands.pregenerator.Pregen;
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
    public static ChunkGenerator generator;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (hasPermission(sender, "worldmanager.gui")) {
                new Gui(sender).execute();
            } else {
                WorldManager.langConfig.sendFormat(sender, "permission.noPermission");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list":
                if (hasPermission(sender, "worldmanager.list")) {
                    new Lists(sender).execute();
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermission");
                }
                break;
            case "gui":
            case "open":
                if (hasPermission(sender, "worldmanager.gui")) {
                    new Gui(sender).execute();
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermission");
                }
                break;
            case "backup":
                if (args.length > 1 && hasPermission(sender, "worldmanager.backup")) {
                    new Backup(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToBackup");
                }
                break;
            case "restore":
                if (args.length > 1 && hasPermission(sender, "worldmanager.restore")) {
                    new Restore(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToRestore");
                }
                break;
            case "c":
            case "create":
                if (hasPermission(sender, "worldmanager.create")) {
                    String name = args.length > 1 ? args[1] : null;
                    String type = args.length > 2 ? args[2] : null;
                    String seed = args.length > 3 ? args[3] : null;
                    String gen = args.length > 4 ? args[4] : null;
                    new Create(sender).execute(name, type, seed, gen);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToCreate");
                }
                break;
            case "del":
            case "delete":
                if (args.length > 1 && hasPermission(sender, "worldmanager.delete")) {
                    new Delete(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToDelete");
                }
                break;
            case "l":
            case "load":
            case "import":
                if (hasPermission(sender, "worldmanager.load")) {
                    String name = args.length > 1 ? args[1] : null;
                    String type = args.length > 2 ? args[2] : null;
                    String gen = args.length > 3 ? args[3] : null;
                    new Load(sender).execute(name, type, gen);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToLoad");
                }
                break;
            case "u":
            case "unload":
                if (args.length > 1 && hasPermission(sender, "worldmanager.unload")) {
                    new Unload(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToUnload");
                }
                break;
            case "tp":
            case "teleport":
                if (hasPermission(sender, "worldmanager.teleport")) {
                    new Teleport(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermissionToTeleport");
                }
                break;
            case "pregen":
                if (hasPermission(sender, "worldmanager.pregen")) {
                    new Pregen(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendFormat(sender, "permission.noPermission");
                }
                break;
            case "help":
            default:
                new MessageManager(sender).sendHelp();
                break;
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.isOp();
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] commands = {"create", "delete", "list", "load", "teleport", "unload", "pregen", "gui", "open", "help", "restore", "backup"};
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
                case "load":
                case "l":
                case "import":
                    completions.addAll(getUnloadedWorlds());
                    break;
                case "delete":
                case "teleport":
                case "tp":
                case "unload":
                case "del":
                case "u":
                case "backup":
                case "restore":
                    for (World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    break;
                case "pregen":
                    completions.add("start");
                    completions.add("stop");
                    completions.add("pause");
                    completions.add("resume");
                    break;
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
                    break;
                default:
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("pregen")) {
            String partialName = args[2].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
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
