package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.SignPortal;
import fr.mathildeuh.worldmanager.configs.SignPortalsManager;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import org.bukkit.command.CommandSender;

public class SignPortalCommand {

    private final CommandSender sender;

    public SignPortalCommand(CommandSender sender) {
        this.sender = sender;
    }

    public void execute(String... args) {
        String action = args.length > 1 ? args[1].toLowerCase() : "";
        switch (action) {
            case "list" -> list();
            case "" -> sendGuide();
            default -> {
                WorldManager.langConfig.sendError(sender, "signportal.unknown_action", action);
                sendGuide();
            }
        }
    }

    private void list() {
        if (SignPortalsManager.all().isEmpty()) {
            WorldManager.langConfig.sendWaiting(sender, "signportal.list_empty");
            return;
        }

        WorldManager.langConfig.sendWaiting(sender, "signportal.list_header");
        for (SignPortal portal : SignPortalsManager.all()) {
            WorldManager.langConfig.sendWaiting(sender, "signportal.list_item",
                    portal.world(), portal.x(), portal.y(), portal.z(), portal.destinationWorld(), portal.createdBy());
        }
    }

    /** Guided explanation, shown for bare {@code /wm sign} or an unrecognized action - sign portals
     * have no creation command (see {@code events/SignPortalListener}), so this teaches the in-world
     * ritual instead of a command sequence. */
    private void sendGuide() {
        for (String line : WorldManager.langConfig.getStringList("signportal.guide")) {
            MessageUtils.sendMini(sender, line);
        }
    }
}
