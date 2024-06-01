package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.buttons.SGButtonListener;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUICreate implements Listener {

    private final Map<Player, String> playersInEditor;
    private final List<String> worldTypesAndEnvironments;
    private String worldName, worldSeed, generator;
    private int currentTypeIndex;
    private MessageManager message;

    public GUICreate(WorldManager plugin) {
        this.playersInEditor = new HashMap<>();
        this.worldTypesAndEnvironments = new ArrayList<>();
        for (World.Environment env : World.Environment.values()) {
            if (env != World.Environment.CUSTOM)
                this.worldTypesAndEnvironments.add("➥ " + env.name());
        }

        for (WorldType wType : WorldType.values()) {
            if (wType != WorldType.NORMAL)
                this.worldTypesAndEnvironments.add("➥ " + wType.getName());
        }

        this.currentTypeIndex = 0;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void open(Player player) {
        this.message = new MessageManager(player);

        SGMenu menu = WorldManager.getSpiGUI().create("&aCreate a world", 1);

        String name = worldName != null ? worldName : "§aSet world name";
        String seed = worldSeed != null ? worldSeed : "§aSet world seed";
        String gen = generator != null ? generator : "§aAdd custom generator";

        List<String> lore = new ArrayList<>();
        for (int i = 0; i < worldTypesAndEnvironments.size(); i++) {
            String type = worldTypesAndEnvironments.get(i);
            if (i == currentTypeIndex) {
                lore.add("§a" + type.toLowerCase());
            } else {
                lore.add("§7" + type.toLowerCase());
            }
        }

        menu.setButton(0, createButton(Material.ANVIL, "§3" + name + " §cRequired !", "§7Click to set world name", event -> {
            message.parse(MessageManager.MessageType.CUSTOM, "<dark_gray>></dark_gray> <green><b>Write world name in chat</b></green>");
            playersInEditor.put(player, "name");
            player.closeInventory();
        }));

        menu.setButton(3, createButton(Material.GRASS_BLOCK, "§aClick to switch world type §5Optional", lore, event -> {
            currentTypeIndex = (currentTypeIndex + 1) % worldTypesAndEnvironments.size();
            open(player);
        }));

        menu.setButton(4, createButton(Material.STRUCTURE_BLOCK, "§3" + seed + " §5Optional", "§7Click to set world seed", event -> {
            message.parse(MessageManager.MessageType.CUSTOM, "<dark_gray>></dark_gray> <green><b>Write world seed in chat</b></green>");
            playersInEditor.put(player, "seed");
            player.closeInventory();
        }));

        menu.setButton(5, createButton(Material.BOOK, "§3" + gen + " §5Optional", "§7Click to add a custom generator", event -> {
            message.parse(MessageManager.MessageType.CUSTOM, "<dark_gray>></dark_gray> <green><b>Write custom generator in chat</b></green>");
            playersInEditor.put(player, "generator");
            player.closeInventory();
        }));

        menu.setButton(7, createButton(Material.BARRIER, "§cCancel", "§7Click to close this inventory", event -> {
            message.parse(MessageManager.MessageType.ERROR, "Operation canceled");
            new GUIMain(player);
        }));

        if (worldName == null) {
            menu.setButton(8, createButton(Material.RED_WOOL, "§5Create", "§7You must set a world name", event -> {
                message.parse(MessageManager.MessageType.ERROR, "You must set a world name");
            }));
        } else {
            menu.setButton(8, createButton(Material.GREEN_WOOL, "§aCreate", "§7Click to create this world", event -> {
                player.closeInventory();
                new fr.mathildeuh.worldmanager.commands.subcommands.Create(player).run(worldName, worldTypesAndEnvironments.get(currentTypeIndex).split(" ")[1].toLowerCase(), worldSeed, generator);
            }));
        }

        player.openInventory(menu.getInventory());
    }

    private SGButton createButton(Material material, String name, String lore, SGButtonListener listener) {
        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build()
        ).withListener(listener);
    }

    private SGButton createButton(Material material, String name, List<String> lore, SGButtonListener listener) {
        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build()
        ).withListener(listener);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (playersInEditor.containsKey(player)) {
            e.setCancelled(true);
            String input = e.getMessage();
            String[] words = input.split("\\s+"); // Split the input by whitespace
            String type = playersInEditor.get(player);
            playersInEditor.remove(player);

            // Get the first word
            String firstWord = words[0];

            switch (type) {
                case "name":
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                        message.parse(MessageManager.MessageType.SUCCESS, "World name set to: \"" + firstWord + "\"");
                        worldName = firstWord;
                    });
                    break;
                case "seed":
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                        message.parse(MessageManager.MessageType.SUCCESS, "World seed set to: \"" + firstWord + "\"");
                        worldSeed = firstWord;
                    });
                    break;
                case "generator":
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                        message.parse(MessageManager.MessageType.SUCCESS, "Custom generator added: \"" + firstWord + "\"");
                        generator = firstWord;
                    });
                    break;
            }

            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                open(player);
            });
        }
    }


}
