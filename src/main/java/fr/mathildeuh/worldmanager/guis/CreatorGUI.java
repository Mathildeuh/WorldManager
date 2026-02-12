package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Create;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static fr.mathildeuh.worldmanager.commands.WorldManagerCommand.getUnloadedWorlds;

public class CreatorGUI implements Listener {
    private static final Map<Player, String> playersInEditor = new HashMap<>();
    private final List<String> worldTypesAndEnvironments = new ArrayList<>();
    private ChestGui gui;
    private int currentIndex = 0;
    private String worldName;
    private String seed;
    private String generator;

    public CreatorGUI() {
        for (World.Environment env : World.Environment.values()) {
            if (env != World.Environment.CUSTOM) {
                this.worldTypesAndEnvironments.add("➥ " + env.name());
            }
        }

        for (WorldType wType : WorldType.values()) {
            if (wType != WorldType.NORMAL) {
                this.worldTypesAndEnvironments.add("➥ " + wType.getName());
            }
        }

        this.gui = createGui();
        Bukkit.getServer().getPluginManager().registerEvents(this, JavaPlugin.getPlugin(WorldManager.class));
    }

    private ChestGui createGui() {
        AtomicReference<ChestGui> guiRef = new AtomicReference<>(new ChestGui(3, "Create a world"));
        guiRef.get().setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(3, 0, 6, 3);
        String seedString = seed != null ? seed : "Random";

        // Create button
        ItemBuilder createItem = new ItemBuilder(worldName == null ? Material.RED_WOOL : Material.GREEN_WOOL)
                .name(worldName == null ? "&c&nCan't create new world" : "&2&nCreate world")
                .lore(worldName == null ? List.of("", "&4ERROR: &7Please click on ", "&7the anvil to set a name") :
                        List.of(
                                "",
                                "&6Name: &b" + worldName,
                                "&6Type: &b" + worldTypesAndEnvironments.get(currentIndex).replace("➥ ", "").toLowerCase(),
                                "&6Seed: &b" + seedString,
                                "&6Generator: &b" + (generator == null ? "Default" : generator)
                        ));

        if (createItem.getMaterial() == Material.GREEN_WOOL) {
            createItem = createItem.glow();
        }

        GuiItem createGuiItem = new GuiItem(createItem.build(), event -> {
            if (worldName == null) return;
            event.getWhoClicked().closeInventory();
            new Create(event.getWhoClicked()).run(worldName, worldTypesAndEnvironments.get(currentIndex).split(" ")[1].toLowerCase(), seed, generator);
        });
        pane.addItem(createGuiItem, 0, 0);

        // Name editor button
        ItemStack nameItem = new ItemBuilder(Material.ANVIL)
                .name(worldName == null ? "&cRequired: §6World name" : "&6World name: &b" + worldName)
                .lore("&7Click to edit world name").build();
        GuiItem nameGuiItem = new GuiItem(nameItem, event -> {
            MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write world name in chat</b></green>");
            playersInEditor.put((Player) event.getWhoClicked(), "name");
            event.getWhoClicked().closeInventory();
        });
        pane.addItem(nameGuiItem, 1, 1);

        // Cancel button
        ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&cCancel")
                .lore("&7Click to re-open the main menu").build();
        GuiItem backGuiItem = new GuiItem(backItem, event -> {
            if (event.getWhoClicked() instanceof Player player) {
                GUIList.MAIN.open(player);
            }
        });
        pane.addItem(backGuiItem, 2, 0);

        // World type selector
        List<String> lore = new ArrayList<>();
        for (int i = 0; i < worldTypesAndEnvironments.size(); i++) {
            String type = worldTypesAndEnvironments.get(i);
            if (i == currentIndex) {
                lore.add("§b" + type.toLowerCase());
            } else {
                lore.add("§7" + type.toLowerCase());
            }
        }

        ItemStack worldTypeItem = new ItemBuilder(Material.GRASS_BLOCK)
                .name("&7Optional: &6World Type")
                .lore(lore).build();
        GuiItem worldTypeGuiItem = new GuiItem(worldTypeItem, event -> {
            currentIndex = (currentIndex + 1) % worldTypesAndEnvironments.size();
            guiRef.set(createGui());
            guiRef.get().show(event.getWhoClicked());
        });
        pane.addItem(worldTypeGuiItem, 0, 2);

        // Generator editor button
        ItemStack generatorItem = new ItemBuilder(Material.STRUCTURE_BLOCK)
                .name(generator == null ? "&7Optional: &6World Generator" : "&6World Generator: &b" + generator)
                .lore("&7Click to edit world generator").build();
        GuiItem generatorGuiItem = new GuiItem(generatorItem, event -> {
            MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write custom generator in chat</b></green>");
            playersInEditor.put((Player) event.getWhoClicked(), "generator");
            event.getWhoClicked().closeInventory();
        });
        pane.addItem(generatorGuiItem, 1, 2);

        // Seed editor button
        ItemStack seedItem = new ItemBuilder(Material.BOOK)
                .name(seed == null ? "&7Optional: &6World Seed" : "&6World Seed: &b" + seed)
                .lore("&7Click to edit world seed").build();
        GuiItem seedGuiItem = new GuiItem(seedItem, event -> {
            MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write world seed in chat</b></green>");
            playersInEditor.put((Player) event.getWhoClicked(), "seed");
            event.getWhoClicked().closeInventory();
        });
        pane.addItem(seedGuiItem, 2, 2);

        // Empty world creator button
        ItemStack emptyWorld = new ItemBuilder(Material.BARRIER)
                .name("Generate an empty world")
                .lore(worldName == null ? "&cRequired: §6World name" : "&6World name: &b" + worldName)
                .build();

        GuiItem emptyWorldGuiItem = new GuiItem(emptyWorld, event -> createEmptyWorld((Player) event.getWhoClicked()));
        pane.addItem(emptyWorldGuiItem, 5, 2);

        guiRef.get().addPane(pane);
        return guiRef.get();
    }

    private void createEmptyWorld(Player player) {
        if (worldName == null) return;
        player.closeInventory();

        long seedValue;
        if (seed != null && seed.matches("-?\\d+")) {
            seedValue = Long.parseLong(seed);
        } else if (seed != null && !seed.isEmpty()) {
            WorldManager.langConfig.sendError(player, "create.invalid_seed");
            return;
        } else {
            seedValue = new Random().nextLong();
        }

        if (Bukkit.getWorld(worldName) != null) {
            WorldManager.langConfig.sendError(player, "create.already_exists");
            return;
        }

        if (worldName.equalsIgnoreCase("plugins") || worldName.equalsIgnoreCase("logs") ||
            worldName.equalsIgnoreCase("libraries") || worldName.equalsIgnoreCase("versions") ||
            worldName.equalsIgnoreCase("config") || worldName.equalsIgnoreCase("cache")) {
            WorldManager.langConfig.sendError(player, "create.name_unusable");
            return;
        }

        if (getUnloadedWorlds().contains(worldName)) {
            WorldManager.langConfig.sendError(player, "create.exists_but_not_loaded", worldName);
            return;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.seed(seedValue);
        creator.generator(new EmptyWorldGenerator());
        World world = Bukkit.createWorld(creator);

        Create.sendStarting(player, "Empty", worldName, seed, "Empty");
        WorldManager.langConfig.sendSuccess(player, "create.success", worldName);
        if (world != null) {
            try {
                world.save();
            } catch (Exception ignored) { }
            WorldManager.addWorld(player, creator.name(), creator.type().name(), creator.environment(), generator);
        } else {
            WorldManager.langConfig.sendError(player, "create.error", "World creation returned null");
        }
    }

    public ChestGui getGui() {
        return this.gui;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!playersInEditor.containsKey(player)) return;

        e.setCancelled(true);
        String input = e.getMessage();
        String[] words = input.split("\\s+");
        String type = playersInEditor.get(player);
        playersInEditor.remove(player);

        if (words[0].equalsIgnoreCase("cancel")) {
            runTask(() -> {
                gui = createGui();
                gui.show(player);
            });
            return;
        }

        switch (type) {
            case "name" -> runTask(() -> worldName = words[0]);
            case "seed" -> runTask(() -> seed = words[0]);
            case "generator" -> runTask(() -> generator = words[0]);
        }

        runTask(() -> {
            gui = createGui();
            gui.show(player);
        });
    }

    private void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), task);
    }
}

