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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldManagerCommand implements CommandExecutor, TabCompleter {
    /** One pre-generation run per world, keyed by world name, so multiple worlds can pre-generate concurrently. */
    public static final java.util.Map<String, ChunkGenerator> activeGenerators = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (hasPermission(sender, "worldmanager.gui")) {
                new Gui(sender).execute();
            } else {
                WorldManager.langConfig.sendError(sender, "permission.no_permission");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list":
                if (hasPermission(sender, "worldmanager.list")) {
                    new Lists(sender).execute();
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.no_permission");
                }
                break;
            case "gui":
            case "open":
                if (hasPermission(sender, "worldmanager.gui")) {
                    new Gui(sender).execute();
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.no_permission");
                }
                break;
            case "backup":
                if (!hasPermission(sender, "worldmanager.backup")) {
                    WorldManager.langConfig.sendError(sender, "permission.backup");
                } else if (args.length > 1) {
                    new Backup(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendError(sender, "backup.usage");
                }
                break;
            case "restore":
                if (!hasPermission(sender, "worldmanager.restore")) {
                    WorldManager.langConfig.sendError(sender, "permission.restore");
                } else if (args.length > 1) {
                    new Restore(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendError(sender, "restore.usage");
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
                    WorldManager.langConfig.sendError(sender, "permission.create");
                }
                break;
            case "del":
            case "delete":
                if (!hasPermission(sender, "worldmanager.delete")) {
                    WorldManager.langConfig.sendError(sender, "permission.delete");
                } else if (args.length > 1) {
                    new Delete(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendError(sender, "delete.usage");
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
                    WorldManager.langConfig.sendError(sender, "permission.load");
                }
                break;
            case "u":
            case "unload":
                if (!hasPermission(sender, "worldmanager.unload")) {
                    WorldManager.langConfig.sendError(sender, "permission.unload");
                } else if (args.length > 1) {
                    new Unload(sender).execute(args[1]);
                } else {
                    WorldManager.langConfig.sendError(sender, "unload.usage");
                }
                break;
            case "tp":
            case "teleport":
                if (hasPermission(sender, "worldmanager.teleport")) {
                    new Teleport(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.teleport");
                }
                break;
            case "pregen":
                if (hasPermission(sender, "worldmanager.pregen")) {
                    new Pregen(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.no_permission");
                }
                break;
            case "sign":
                if (hasPermission(sender, "worldmanager.sign.create")) {
                    new SignPortalCommand(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.no_permission");
                }
                break;
            case "portal":
                if (hasPermission(sender, "worldmanager.portal")) {
                    new PortalCommand(sender).execute(args);
                } else {
                    WorldManager.langConfig.sendError(sender, "permission.no_permission");
                }
                break;
            case "help":
                new MessageManager(sender).sendHelp();
                break;
            default:
                WorldManager.langConfig.sendError(sender, "general.unknown_command", subCommand);
                new MessageManager(sender).sendHelp();
                break;
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        // Bukkit already grants every permission declared "default: op" in plugin.yml to
        // operators, and respects explicit negations set by a permissions plugin - so this must
        // not also fall back to sender.isOp(), or an explicit negation could never take effect.
        return sender.hasPermission(permission);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] commands = {"create", "delete", "list", "load", "teleport", "unload", "pregen", "gui", "open", "help", "restore", "backup", "sign", "portal"};
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
                case "sign":
                    completions.add("list");
                    break;
                case "portal":
                    completions.addAll(List.of("pos1", "pos2", "create", "remove", "destination", "permission", "price", "launch", "list"));
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
                case "create":
                case "c":
                    completions.addAll(List.of("normal", "flat", "amplified", "large_biomes", "the_end", "the_nether"));
                    break;
                case "teleport":
                case "tp":
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
                case "portal":
                    if (List.of("remove", "destination", "permission", "price", "launch").contains(args[1].toLowerCase())) {
                        for (fr.mathildeuh.worldmanager.configs.CustomPortal portal : fr.mathildeuh.worldmanager.configs.CustomPortalsManager.all()) {
                            completions.add(portal.name);
                        }
                    }
                    break;
                default:
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("portal") && "destination".equalsIgnoreCase(args[1])) {
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("pregen") && "start".equalsIgnoreCase(args[1])) {
            // handleStart() reads center:x,z / radius:n tokens from here onward, in any order.
            completions.add("center:");
            completions.add("radius:");
        }

        return completions;
    }

    public static List<String> getUnloadedWorlds() {
        return fr.mathildeuh.worldmanager.util.WorldFolders.listUnloadedWorldFolders();
    }
}
