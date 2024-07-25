package fr.mathildeuh.worldmanager.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;

import static fr.mathildeuh.worldmanager.commands.WorldManagerCommand.getUnloadedWorlds;

public class Placeholders extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "worldmanager";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Mathildeuh";
    }

    @Override
    public @NotNull String getVersion() {
        return Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("WorldManager")).getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        System.out.println(identifier);
        if (player == null) {
            return "";
        }

        return switch (identifier) {
            case "version" -> getVersion();
            case "current_world" -> player.getWorld().getName();
            case "loaded" -> String.valueOf(Bukkit.getWorlds().size());
            case "loaded_list" -> Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.joining(", "));
            case "unloaded" -> String.valueOf(getUnloadedWorlds().size());
            case "unloaded_list" -> getUnloadedWorlds().stream()
                    .collect(Collectors.joining(", "));
            case "total_world_amount" -> String.valueOf(Bukkit.getWorlds().size() + getUnloadedWorlds().size());
            case "total_player_amount" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            default -> {
                if (identifier.startsWith("amount_")) {
                    if (identifier.length() > "amount_".length()) {
                        String worldName = identifier.substring("amount_".length());
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            yield String.valueOf(world.getPlayers().size());
                        } else {
                            yield "§cThis world does not exist.";
                        }
                    } else {
                        yield "0";
                    }
                }
                yield "";
            }
        };
    }

}
