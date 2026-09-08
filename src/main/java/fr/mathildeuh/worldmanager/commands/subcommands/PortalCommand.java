package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.CustomPortal;
import fr.mathildeuh.worldmanager.configs.CustomPortalsManager;
import fr.mathildeuh.worldmanager.configs.SavedLocation;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles every {@code /wm portal <action>} subcommand - see {@code configs/CustomPortalsManager}. */
public class PortalCommand {

    private final CommandSender sender;

    public PortalCommand(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String... args) {
        String action = args.length > 1 ? args[1].toLowerCase() : "";
        switch (action) {
            case "pos1" -> setPos(true);
            case "pos2" -> setPos(false);
            case "create" -> create(args);
            case "remove", "delete" -> remove(args);
            case "destination", "dest" -> destination(args);
            case "permission", "perm" -> permission(args);
            case "price" -> price(args);
            case "launch" -> launch(args);
            case "list" -> list();
            case "" -> sendGuide();
            default -> {
                WorldManager.langConfig.sendError(sender, "portal.unknown_action", action);
                sendGuide();
            }
        }
    }

    private void setPos(boolean first) {
        Player player = requirePlayer();
        if (player == null) return;
        Location loc = player.getLocation();
        if (first) {
            CustomPortalsManager.setPos1(player, loc);
            WorldManager.langConfig.sendSuccess(sender, "portal.pos1_set", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } else {
            CustomPortalsManager.setPos2(player, loc);
            WorldManager.langConfig.sendSuccess(sender, "portal.pos2_set", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }

    private void create(String[] args) {
        Player player = requirePlayer();
        if (player == null) return;
        if (args.length < 3) {
            WorldManager.langConfig.sendError(sender, "portal.usage_create");
            return;
        }
        String name = args[2];
        CustomPortalsManager.CreateResult result = CustomPortalsManager.create(player, name);
        switch (result) {
            case OK -> WorldManager.langConfig.sendSuccess(sender, "portal.created", name);
            case MISSING_SELECTION -> WorldManager.langConfig.sendError(sender, "portal.missing_selection");
            case NAME_TAKEN -> WorldManager.langConfig.sendError(sender, "portal.name_taken", name);
            case INVALID_NAME -> WorldManager.langConfig.sendError(sender, "general.invalid_world_name", name);
        }
    }

    private void remove(String[] args) {
        if (args.length < 3) {
            WorldManager.langConfig.sendError(sender, "portal.usage_remove");
            return;
        }
        if (CustomPortalsManager.remove(args[2])) {
            WorldManager.langConfig.sendSuccess(sender, "portal.removed", args[2]);
        } else {
            WorldManager.langConfig.sendError(sender, "portal.not_found", args[2]);
        }
    }

    private void destination(String[] args) {
        if (args.length < 3) {
            WorldManager.langConfig.sendError(sender, "portal.usage_destination");
            return;
        }
        CustomPortal portal = CustomPortalsManager.get(args[2]);
        if (portal == null) {
            WorldManager.langConfig.sendError(sender, "portal.not_found", args[2]);
            return;
        }

        Location target;
        if (args.length > 3) {
            World world = Bukkit.getWorld(args[3]);
            if (world == null) {
                WorldManager.langConfig.sendError(sender, "teleport.world_not_found", args[3]);
                return;
            }
            Location spawn = WorldsConfig.getSpawn(world);
            target = spawn != null ? spawn : world.getSpawnLocation();
        } else {
            Player player = requirePlayer();
            if (player == null) return;
            target = player.getLocation();
        }

        portal.destination = SavedLocation.of(target);
        CustomPortalsManager.save(portal);
        WorldManager.langConfig.sendSuccess(sender, "portal.destination_set", portal.name, target.getWorld().getName());
    }

    private void permission(String[] args) {
        if (args.length < 3) {
            WorldManager.langConfig.sendError(sender, "portal.usage_permission");
            return;
        }
        CustomPortal portal = CustomPortalsManager.get(args[2]);
        if (portal == null) {
            WorldManager.langConfig.sendError(sender, "portal.not_found", args[2]);
            return;
        }
        String node = args.length > 3 ? args[3] : null;
        portal.permission = (node == null || node.equalsIgnoreCase("clear")) ? null : node;
        CustomPortalsManager.save(portal);
        WorldManager.langConfig.sendSuccess(sender, "portal.permission_set", portal.name, portal.permission == null ? "-" : portal.permission);
    }

    private void price(String[] args) {
        if (args.length < 4) {
            WorldManager.langConfig.sendError(sender, "portal.usage_price");
            return;
        }
        CustomPortal portal = CustomPortalsManager.get(args[2]);
        if (portal == null) {
            WorldManager.langConfig.sendError(sender, "portal.not_found", args[2]);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            WorldManager.langConfig.sendError(sender, "portal.invalid_price", args[3]);
            return;
        }
        portal.price = Math.max(0, amount);
        CustomPortalsManager.save(portal);
        WorldManager.langConfig.sendSuccess(sender, "portal.price_set", portal.name, String.valueOf(portal.price));
    }

    private void launch(String[] args) {
        if (args.length < 4) {
            WorldManager.langConfig.sendError(sender, "portal.usage_launch");
            return;
        }
        CustomPortal portal = CustomPortalsManager.get(args[2]);
        if (portal == null) {
            WorldManager.langConfig.sendError(sender, "portal.not_found", args[2]);
            return;
        }
        if (args[3].equalsIgnoreCase("clear")) {
            portal.launch = null;
            CustomPortalsManager.save(portal);
            WorldManager.langConfig.sendSuccess(sender, "portal.launch_cleared", portal.name);
            return;
        }
        if (args.length < 6) {
            WorldManager.langConfig.sendError(sender, "portal.usage_launch");
            return;
        }
        try {
            double dx = Double.parseDouble(args[3]);
            double dy = Double.parseDouble(args[4]);
            double dz = Double.parseDouble(args[5]);
            portal.launch = new double[]{dx, dy, dz};
            CustomPortalsManager.save(portal);
            WorldManager.langConfig.sendSuccess(sender, "portal.launch_set", portal.name);
        } catch (NumberFormatException e) {
            WorldManager.langConfig.sendError(sender, "portal.invalid_launch");
        }
    }

    private void list() {
        if (CustomPortalsManager.all().isEmpty()) {
            WorldManager.langConfig.sendWaiting(sender, "portal.list_empty");
            return;
        }
        WorldManager.langConfig.sendWaiting(sender, "portal.list_header");
        for (CustomPortal portal : CustomPortalsManager.all()) {
            String dest = portal.destination != null ? portal.destination.world() : "-";
            WorldManager.langConfig.sendWaiting(sender, "portal.list_item",
                    portal.name, portal.world, dest, portal.price, portal.permission == null ? "-" : portal.permission);
        }
    }

    /** Guided step-by-step help, shown for bare {@code /wm portal} or an unrecognized action. */
    private void sendGuide() {
        for (String line : WorldManager.langConfig.getStringList("portal.guide")) {
            MessageUtils.sendMini(sender, line);
        }
    }

    private Player requirePlayer() {
        if (sender instanceof Player player) {
            return player;
        }
        WorldManager.langConfig.sendError(sender, "general.players_only");
        return null;
    }
}
