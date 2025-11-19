package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.*;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static fr.mathildeuh.worldmanager.commands.WorldManagerCommand.getUnloadedWorlds;

public class GUISManager {

    ChestGui gui;

    public GUISManager() {

        gui = new ChestGui(3, "World Manager - GUI");
        StaticPane pane = new StaticPane(0, 0, 9, 3);
        gui.setOnGlobalClick(event -> event.setCancelled(true));


        ItemBuilder grass = new ItemBuilder(Material.GRASS_BLOCK).name("&2&nCreate a world").lore("", "&7&oCreate a new fully customizable world");
        pane.addItem(new GuiItem(grass.build(), event -> { if (event.getWhoClicked() instanceof Player player) GUIList.CREATOR.open(player); }), Slot.fromIndex(11));


        ItemBuilder cmd = new ItemBuilder(Material.COMMAND_BLOCK).name("&2&nEdit your worlds").lore("", "&7&oManage your loaded worlds");
        pane.addItem(new GuiItem(cmd.build(), event -> { if (event.getWhoClicked() instanceof Player player) GUIList.EDITOR.open(player); }), Slot.fromIndex(13));


        ItemBuilder struct = new ItemBuilder(Material.STRUCTURE_VOID).name("&2&nLoad a world").lore("", "&7&oLoad a world that is", "&7&oin server folders");
        pane.addItem(new GuiItem(struct.build(), event -> { if (event.getWhoClicked() instanceof Player player) GUIList.LOADER.open(player); }), Slot.fromIndex(15));


        gui.addPane(pane);

    }

    public ChestGui getGui() {
        return gui;
    }


    public static class Creator implements Listener {
        private static final Map<Player, String> playersInEditor = new HashMap<>();
        private final List<String> worldTypesAndEnvironments = new ArrayList<>();
        ChestGui gui;
        int currentIndex = 0;
        private String worldName, seed, generator;

        public Creator() {
            for (World.Environment env : World.Environment.values()) {
                if (env != World.Environment.CUSTOM)
                    this.worldTypesAndEnvironments.add("➥ " + env.name());
            }

            for (WorldType wType : WorldType.values()) {
                if (wType != WorldType.NORMAL)
                    this.worldTypesAndEnvironments.add("➥ " + wType.getName());
            }
            this.gui = createGui();
            Bukkit.getServer().getPluginManager().registerEvents(this, JavaPlugin.getPlugin(WorldManager.class));
        }

        private ChestGui createGui() {
            AtomicReference<ChestGui> gui = new AtomicReference<>(new ChestGui(3, "Create a world"));
            gui.get().setOnGlobalClick(event -> event.setCancelled(true));

            StaticPane pane = new StaticPane(3, 0, 6, 3);

            String seedString = seed != null ? seed : "Random";

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

            if (createItem.getMaterial() == Material.GREEN_WOOL)
                createItem = createItem.glow();
            GuiItem createGuiItem = new GuiItem(createItem.build(), event -> {
                if (worldName == null) return;
                event.getWhoClicked().closeInventory();
                new Create(event.getWhoClicked()).run(worldName, worldTypesAndEnvironments.get(currentIndex).split(" ")[1].toLowerCase(), seed, generator);
            });

            pane.addItem(createGuiItem, 0, 0);

            ItemStack nameItem = new ItemBuilder(Material.ANVIL)
                    .name(worldName == null ? "&cRequired: §6World name" : "&6World name: &b" + worldName)
                    .lore("&7Click to edit world name").build();
            GuiItem nameGuiItem = new GuiItem(nameItem, event -> { MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write world name in chat</b></green>"); playersInEditor.put(Bukkit.getPlayer(event.getWhoClicked().getName()), "name"); event.getWhoClicked().closeInventory(); });

            pane.addItem(nameGuiItem, 1, 1);

            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name("&cCancel")
                    .lore("&7Click to re-open the main menu").build();
            GuiItem backGuiItem = new GuiItem(backItem, event -> { if (event.getWhoClicked() instanceof Player player) GUIList.MAIN.open(player); });

            pane.addItem(backGuiItem, 2, 0);

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
            GuiItem worldTypeGuiItem = new GuiItem(worldTypeItem, event -> { currentIndex = (currentIndex + 1) % worldTypesAndEnvironments.size(); gui.set(createGui()); gui.get().show(event.getWhoClicked()); });

            pane.addItem(worldTypeGuiItem, 0, 2);

            ItemStack generatorItem = new ItemBuilder(Material.STRUCTURE_BLOCK)
                    .name(generator == null ? "&7Optional: &6World Generator" : "&6World Generator: &b" + generator)
                    .lore("&7Click to edit world generator").build();
            GuiItem generatorGuiItem = new GuiItem(generatorItem, event -> { MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write custom generator in chat</b></green>"); playersInEditor.put(Bukkit.getPlayer(event.getWhoClicked().getName()), "generator"); event.getWhoClicked().closeInventory(); });

            pane.addItem(generatorGuiItem, 1, 2);

            ItemStack seedItem = new ItemBuilder(Material.BOOK)
                    .name(seed == null ? "&7Optional: &6World Seed" : "&6World Seed: &b" + seed)
                    .lore("&7Click to edit world seed").build();
            GuiItem seedGuiItem = new GuiItem(seedItem, event -> { MessageUtils.sendMini(event.getWhoClicked(), "<dark_gray>></dark_gray> <green><b>Write world seed in chat</b></green>"); playersInEditor.put(Bukkit.getPlayer(event.getWhoClicked().getName()), "seed"); event.getWhoClicked().closeInventory(); });

            pane.addItem(seedGuiItem, 2, 2);

            ItemStack emptyWorld = new ItemBuilder(Material.BARRIER)
                    .name("Generate an empty world").lore(worldName == null ? "&cRequired: §6World name" : "&6World name: &b" + worldName).build();

            GuiItem emptyWorldGuiItem = new GuiItem(emptyWorld, event -> {
                if (worldName == null) return;
                event.getWhoClicked().closeInventory();

                long seedValue = 0L;
                if (seed != null && seed.matches("-?\\d+")) {
                    seedValue = Long.parseLong(seed);
                } else if (seed != null && !seed.isEmpty()) {
                    WorldManager.langConfig.sendError(event.getWhoClicked(), "create.invalid_seed");
                    return;
                }
                if (Bukkit.getWorld(worldName) != null) {
                    WorldManager.langConfig.sendError(event.getWhoClicked(), "create.already_exists");
                    return;
                }

                if (worldName.equalsIgnoreCase("plugins") || worldName.equalsIgnoreCase("logs") || worldName.equalsIgnoreCase("libraries") || worldName.equalsIgnoreCase("versions") || worldName.equalsIgnoreCase("config") || worldName.equalsIgnoreCase("cache")) {
                    WorldManager.langConfig.sendError(event.getWhoClicked(), "create.name_unusable");
                    return;
                }

                if (getUnloadedWorlds().contains(worldName)) {
                    WorldManager.langConfig.sendError(event.getWhoClicked(), "create.exists_but_not_loaded", worldName);
                    return;
                }

                WorldCreator creator = new WorldCreator(worldName);
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.seed(seedValue);
                creator.generator(new EmptyWorldGenerator());
                World world = Bukkit.createWorld(creator);

                Create.sendStarting(event.getWhoClicked(), "Empty", worldName, seed, "Empty");
                WorldManager.langConfig.sendSuccess(event.getWhoClicked(), "create.success", worldName);
                if (world != null) {
                    try {
                        world.save();
                    } catch (Exception ignored) { }
                    WorldManager.addWorld(event.getWhoClicked(), creator.name(), creator.type().name(), creator.environment(), generator);
                } else {
                    WorldManager.langConfig.sendError(event.getWhoClicked(), "create.error", "World creation returned null");
                }
            });

            // Ajout de l'élément emptyWorld dans le coin en bas à droite du panneau
            pane.addItem(emptyWorldGuiItem, 5, 2);

            gui.get().addPane(pane);
            return gui.get();
        }

        public ChestGui getGui() {
            return this.gui;
        }

        @EventHandler
        public void onChat(AsyncPlayerChatEvent e) {
            Player player = e.getPlayer();
            if (playersInEditor.containsKey(player)) {
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
                    case "name":
                        runTask(() -> worldName = words[0]);
                        break;
                    case "seed":
                        runTask(() -> seed = words[0]);
                        break;
                    case "generator":
                        runTask(() -> generator = words[0]);
                        break;
                }

                runTask(() -> {
                    gui = createGui();
                    gui.show(player);
                });
            }
        }

        private void runTask(Runnable task) {
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), task);
        }
    }


    public static class Editor {
        private final ChestGui gui;

        public Editor() {
            List<File> worldFolders = getWorldFolders();
            this.gui = createGui(worldFolders);
        }

        private List<File> getWorldFolders() {
            return Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                    .filter(File::isDirectory)
                    .filter(folder -> new File(folder, "level.dat").exists())
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());
        }

        private ChestGui createGui(List<File> worldFolders) {
            // Use provided worldFolders list instead of Bukkit.getWorlds() to avoid unused parameter warning
            List<String> worldNames = new ArrayList<>(worldFolders.stream().map(File::getName).toList());


//            Fake loaded worlds
//            for (int i = 0; i <= 1500; i++) {
//                worldNames.add("FAKE_" + i);
//            }

            ChestGui gui = new ChestGui(3, "World Editor");
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 3);

            int worldsPerPage = 18;
            int pageCount = (int) Math.ceil((double) worldNames.size() / worldsPerPage);

            for (int page = 0; page < pageCount; page++) {
                StaticPane pane = new StaticPane(0, 0, 9, 4);
                int start = page * worldsPerPage;
                int end = Math.min(start + worldsPerPage, worldNames.size());

                for (int index = start; index < end; index++) {
                    String worldName = worldNames.get(index);

                    ItemStack worldItem = new ItemBuilder(Material.GRASS_BLOCK)
                            .name("&2&n" + worldName)
                            .lore("", "&6&nCreated by:&r &b" + WorldsConfig.getCreator(worldName), "&7&oEdit the world " + worldName)
                            .build();

                    int slotIndex = index % worldsPerPage;
                    pane.addItem(new GuiItem(worldItem, event -> handleItemClick(event, worldName)), Slot.fromIndex(slotIndex));
                }

                paginatedPane.addPane(page, pane);
            }

            String guiTitle = buildGuiTitle(paginatedPane);

            gui.setTitle(guiTitle);

            if (pageCount > 0) {
                paginatedPane.setPage(0);
            }

            gui.addPane(paginatedPane);

            StaticPane navigationPane = new StaticPane(0, 2, 9, 1);


            ItemStack prevPageItem = new ItemBuilder(Material.MAGENTA_DYE)
                    .name("&aPrevious Page")
                    .build();

            navigationPane.addItem(new GuiItem(prevPageItem, event -> { if (paginatedPane.getPage() > 0) { paginatedPane.setPage(paginatedPane.getPage() - 1); gui.setTitle(buildGuiTitle(paginatedPane)); gui.update(); } }), Slot.fromIndex(3));

            ItemStack nextPageItem = new ItemBuilder(Material.LIME_DYE)
                    .name("&aNext Page")
                    .build();
            navigationPane.addItem(new GuiItem(nextPageItem, event -> { if (paginatedPane.getPage() < pageCount - 1) { paginatedPane.setPage(paginatedPane.getPage() + 1); gui.setTitle(buildGuiTitle(paginatedPane)); gui.update(); } }), Slot.fromIndex(5));

            gui.addPane(navigationPane);

            StaticPane backButton = new StaticPane(0, 2, 9, 1);
            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name("&cBack")
                    .build();
            backButton.addItem(new GuiItem(backItem, event -> { if (event.getWhoClicked() instanceof Player player) GUIList.MAIN.open(player); }), Slot.fromIndex(8));

            gui.addPane(backButton);


            return gui;
        }


        private String buildGuiTitle(PaginatedPane paginatedPane) {
            int currentPage = paginatedPane.getPage() + 1;
            int totalPageCount = paginatedPane.getPages();

            return "World Editor - Page " + currentPage + "/" + totalPageCount;
        }

        private void handleItemClick(InventoryClickEvent event, String worldName) {

            if (event.getWhoClicked() instanceof Player player) {
                new EditorOptions(Bukkit.getWorld(worldName)).getGui().show(player);
            }
        }

        public ChestGui getGui() {
            return this.gui;
        }
    }


    public static class Loader {
        private final ChestGui gui;

        public Loader() {
            List<File> worldFolders = getWorldFolders();
            this.gui = createGui(worldFolders);
        }

        private List<File> getWorldFolders() {
            return Arrays.stream(Objects.requireNonNull(Bukkit.getWorldContainer().listFiles()))
                    .filter(File::isDirectory)
                    .filter(folder -> new File(folder, "level.dat").exists())
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());
        }

        private ChestGui createGui(List<File> worldFolders) {
            List<File> unloadedWorldFolders = new ArrayList<>(worldFolders.stream()
                    .filter(worldFolder -> Bukkit.getWorld(worldFolder.getName()) == null).toList());


//            // Fake unloaded worlds
//            for (int i = 0; i <= 1500; i++) {
//                unloadedWorldFolders.add(new File("FAKE_" + i));
//            }

            ChestGui gui = new ChestGui(3, "World Loader");
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 3);

            int worldsPerPage = 18;
            int pageCount = (int) Math.ceil((double) unloadedWorldFolders.size() / worldsPerPage);

            for (int page = 0; page < pageCount; page++) {
                StaticPane pane = new StaticPane(0, 0, 9, 4);
                int start = page * worldsPerPage;
                int end = Math.min(start + worldsPerPage, unloadedWorldFolders.size());

                for (int index = start; index < end; index++) {
                    File worldFolder = unloadedWorldFolders.get(index);
                    String worldName = worldFolder.getName();

                    ItemStack worldItem = new ItemBuilder(Material.GRASS_BLOCK)
                            .name("&2&n" + worldName)
                            .lore("", "&7&oLoad the world " + worldName)
                            .build();

                    int slotIndex = index % worldsPerPage;
                    pane.addItem(new GuiItem(worldItem, event -> handleItemClick(event, worldName)), Slot.fromIndex(slotIndex));
                }

                paginatedPane.addPane(page, pane);
            }

            String guiTitle = buildGuiTitle(paginatedPane);

            gui.setTitle(guiTitle);

            if (pageCount > 0) {
                paginatedPane.setPage(0);
            }

            gui.addPane(paginatedPane);

            StaticPane navigationPane = new StaticPane(0, 2, 9, 1);


            ItemStack prevPageItem = new ItemBuilder(Material.MAGENTA_DYE)
                    .name("&aPrevious Page")
                    .build();

            navigationPane.addItem(new GuiItem(prevPageItem, event -> { if (paginatedPane.getPage() > 0) { paginatedPane.setPage(paginatedPane.getPage() - 1); gui.setTitle(buildGuiTitle(paginatedPane)); gui.update(); } }), Slot.fromIndex(3));

            ItemStack nextPageItem = new ItemBuilder(Material.LIME_DYE)
                    .name("&aNext Page")
                    .build();
            navigationPane.addItem(new GuiItem(nextPageItem, event -> { if (paginatedPane.getPage() < pageCount - 1) { paginatedPane.setPage(paginatedPane.getPage() + 1); gui.setTitle(buildGuiTitle(paginatedPane)); gui.update(); } }), Slot.fromIndex(5));

            gui.addPane(navigationPane);

            StaticPane backButton = new StaticPane(0, 2, 9, 1);
            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name("&cBack")
                    .build();
            backButton.addItem(new GuiItem(backItem, event -> { if (event.getWhoClicked() instanceof Player player) GUIList.MAIN.open(player); }), Slot.fromIndex(8));

            gui.addPane(backButton);


            return gui;
        }


        private String buildGuiTitle(PaginatedPane paginatedPane) {
            int currentPage = paginatedPane.getPage() + 1;
            int totalPageCount = paginatedPane.getPages();

            return "World Loader - Page " + currentPage + "/" + totalPageCount;
        }

        private void handleItemClick(InventoryClickEvent event, String worldName) {

            if (event.getWhoClicked() instanceof Player player) {
                new Load(player).execute(worldName, "normal", null);
                GUIList.LOADER.open(player);
            }
        }

        public ChestGui getGui() {
            return this.gui;
        }
    }

    public static class EditorOptions {
        private final ChestGui gui;
        private final World currentWorld;

        public EditorOptions(World currentWorld) {
            this.currentWorld = currentWorld == null ? Bukkit.getWorlds().get(0) : currentWorld;
            this.gui = createGui();
        }

        private ChestGui createGui() {
            ChestGui gui = new ChestGui(3, "Options for " + currentWorld.getName());

            List<String> disabledLore = List.of("", "&4&nThis feature is disabled on default world");
            boolean isDefaultWorld = currentWorld == Bukkit.getWorlds().get(0);

            gui.setOnGlobalClick(event -> event.setCancelled(true));

            StaticPane pane = new StaticPane(0, 0, 9, 3);  // 9 colonnes et 3 rangées

            pane.addItem(new GuiItem(new ItemBuilder(Material.COMMAND_BLOCK).name("&c&nGame Rules").build(), event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    // small feedback to ensure the click was registered
                    MessageUtils.sendMini(player, "<gray>></gray> <yellow>Opening game rules editor...</yellow>");
                    GameRuleEditor editor = new GameRuleEditor(currentWorld);
                    editor.getGui().show(player);
                }
            }), Slot.fromIndex(3));

            pane.addItem(new GuiItem(new ItemBuilder(Material.STRUCTURE_BLOCK).name("&e&nUnload").lore(isDefaultWorld ? disabledLore : List.of("")).build(), event -> {
                if (!isDefaultWorld)
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.UNLOAD).show(event.getWhoClicked());
            }), Slot.fromIndex(5));

            pane.addItem(new GuiItem(new ItemBuilder(Material.ENDER_PEARL).name("&d&nTeleport").build(), event -> {

                Player player = (Player) event.getWhoClicked();
                new Teleport(player).execute(player.getName(), currentWorld.getName());

            }), Slot.fromIndex(11));

//            pane.addItem(new GuiItem(new ItemBuilder(Material.DARK_OAK_SAPLING).name("&8&nPre-Gen").build(), event -> {
//
//                Player player = (Player) event.getWhoClicked();
//                pregenGui((Player) event.getWhoClicked(), currentWorld.getName(), 0, null).show(event.getWhoClicked());
//
//
//            }), Slot.fromIndex(13));

            pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK).name("&4&nDelete").lore(isDefaultWorld ? disabledLore : List.of("")).build(), event -> {
                if (!isDefaultWorld)
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.DELETE).show(event.getWhoClicked());
            }), Slot.fromIndex(15));


            pane.addItem(new GuiItem(new ItemBuilder(Material.EMERALD).name("&a&nBackup").lore(isDefaultWorld ? disabledLore : List.of("")).build(), event -> {
                if (!isDefaultWorld)
                    new Backup(event.getWhoClicked()).execute(currentWorld.getName());

            }), Slot.fromIndex(21));

            pane.addItem(new GuiItem(new ItemBuilder(Material.DIAMOND).name("&b&nRestore").lore(isDefaultWorld ? disabledLore : List.of("")).build(), event -> {
                if (!isDefaultWorld)
                    confirmationGui((Player) event.getWhoClicked(), ConfirmTypes.RESTORE).show(event.getWhoClicked());

            }), Slot.fromIndex(23));

            pane.addItem(new GuiItem(new ItemBuilder(Material.DARK_OAK_DOOR).name("&c&nBack").build(), event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    GUIList.EDITOR.open(player);
                }
            }), Slot.fromIndex(26));

            gui.addPane(pane);

            return gui;
        }

//        private ChestGui pregenGui(Player whoClicked, String name, int radius, String xy) {
//            ChestGui gui = new ChestGui(3, "Pre-Gen for " + name);
//            gui.setOnGlobalClick(event -> event.setCancelled(true));
//
//            StaticPane pane = new StaticPane(0, 0, 9, 3);  // 9 colonnes et 3 rangées
//
//            xy = xy == null ? xy : "0,0";
//            radius = radius == 0 ? 200 : radius;
//
//            pane.addItem(new GuiItem(new ItemBuilder(Material.GREEN_CONCRETE_POWDER).name("&c&nStart Pre-Generate").build(), event -> {
//
//            }), Slot.fromIndex(11));
//
//            // Anvil for radius
//            ItemStack radiusItem = new ItemBuilder(Material.ANVIL).name("&7Radius").lore(List.of("", "&7Radius: &b" + radius)).build();
//            int finalRadius = radius;
//            String finalXy = xy;
//            pane.addItem(new GuiItem(radiusItem, event -> {
//                if (event.getWhoClicked() instanceof Player player) {
//                    player.closeInventory();
//                    pregenGui(player, name, finalRadius, "2000,2000");
//                }
//            }), Slot.fromIndex(13));
//
//            // Sign for XY
//            pane.addItem(new GuiItem(new ItemBuilder(Material.ACACIA_SIGN).name("&7XY").lore(List.of("", "&7XY: &b" + xy)).build(), event -> {
//                if (event.getWhoClicked() instanceof Player player) {
//                    player.closeInventory();
//
//                    pregenGui(player, name, finalRadius, finalXy);
//                }
//            }), Slot.fromIndex(15));
//
//            gui.addPane(pane);
//
//            // Add the button to the GUI
//
//
//            return gui;
//
//        }

        public ChestGui getGui() {
            return this.gui;
        }

        public ChestGui confirmationGui(Player player, ConfirmTypes types) {

            ChestGui gui = new ChestGui(1, "Confirmation for " + currentWorld.getName());
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            StaticPane pane = new StaticPane(0, 0, 9, 1);  // 9 colonnes et 3 rangées

            switch (types) {
                case DELETE:
                    pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK).name("&4&nDelete").build(), event -> {
                        new Delete(player).execute(currentWorld.getName());
                        new EditorOptions(currentWorld).getGui().show(player);

                    }), Slot.fromIndex(4));
                    break;
                case UNLOAD:
                    pane.addItem(new GuiItem(new ItemBuilder(Material.STRUCTURE_BLOCK).name("&e&nUnload").build(), event -> {
                        new Unload(player).execute(currentWorld.getName());
                        new EditorOptions(currentWorld).getGui().show(player);

                    }), Slot.fromIndex(4));
                    break;
                case RESTORE:
                    pane.addItem(new GuiItem(new ItemBuilder(Material.DIAMOND).name("&b&nRestore").build(), event -> {
                        new Restore(player).execute(currentWorld.getName());
                        new EditorOptions(currentWorld).getGui().show(player);
                    }), Slot.fromIndex(4));
                    break;
            }

            pane.addItem(new GuiItem(new ItemBuilder(Material.DARK_OAK_DOOR).name("&c&nBack").build(), event -> new EditorOptions(currentWorld).getGui().show(player)), Slot.fromIndex(8));

            gui.addPane(pane);
            return gui;
        }

        public enum ConfirmTypes {
            DELETE,
            UNLOAD,
            RESTORE
        }

    }

    public static class GameRuleEditor implements Listener {

        private final World currentWorld;
        private static final Map<Player, ChatEntry> chatInputPlayers = new HashMap<>();
        private static boolean chatListenerRegistered = false;

        private static class ChatEntry {
            final World world;
            final GameRule<Integer> rule;

            ChatEntry(World world, GameRule<Integer> rule) {
                this.world = world;
                this.rule = rule;
            }
        }

        public GameRuleEditor(World currentWorld) {
            this.currentWorld = currentWorld;
            // Register the static chat listener only once
            synchronized (GameRuleEditor.class) {
                if (!chatListenerRegistered) {
                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler
                        public void onPlayerChat(AsyncPlayerChatEvent event) {
                            Player player = event.getPlayer();
                            if (!chatInputPlayers.containsKey(player)) return;
                            event.setCancelled(true);
                            ChatEntry entry = chatInputPlayers.remove(player);
                            String message = event.getMessage();
                            try {
                                int newValue = Integer.parseInt(message);
                                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                                    entry.world.setGameRule(entry.rule, newValue);
                                    WorldManager.langConfig.sendSuccess(player, "gui.game_rule_set", entry.rule.getName(), String.valueOf(newValue));
                                    // Show the editor for this world directly
                                    showGameRuleGui(player, entry.world);
                                });
                            } catch (NumberFormatException e) {
                                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), () -> {
                                    WorldManager.langConfig.sendError(player, "gui.invalid_number");
                                    chatInputPlayers.put(player, entry);
                                });
                            }
                        }
                    }, JavaPlugin.getPlugin(WorldManager.class));
                    chatListenerRegistered = true;
                }
            }
        }


        private void handleGameRuleClick(InventoryClickEvent event, GameRule<?> gameRule) {
            Player player = (Player) event.getWhoClicked();
            openGameRuleEditMenu(player, gameRule);
        }

        @SuppressWarnings("unchecked")
        private void openGameRuleEditMenu(Player player, GameRule<?> gameRule) {
            ChestGui editGui = new ChestGui(3, "Edit " + gameRule.getName());
            editGui.setOnGlobalClick(event -> event.setCancelled(true));

            StaticPane pane = new StaticPane(0, 0, 9, 3);

            Object tempValue = null;
            try {
                tempValue = currentWorld.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // GameRule is not available
                ItemStack errorItem = new ItemBuilder(Material.BARRIER)
                        .name("&cGameRule Not Available")
                        .lore("&7This GameRule is not available in this version")
                        .build();
                pane.addItem(new GuiItem(errorItem), Slot.fromIndex(4));
                ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
                pane.addItem(new GuiItem(backItem, event -> {
                    saveCurrentValues();
                    getGui().show(player);
                }), Slot.fromIndex(8));
                editGui.addPane(pane);
                editGui.show(player);
                return;
            }

            String displayValue = tempValue == null ? "null" : tempValue.toString();
            ItemStack currentValueItem = new ItemBuilder(Material.PAPER)
                    .name("&eCurrent Value: " + displayValue)
                    .build();
            pane.addItem(new GuiItem(currentValueItem), Slot.fromIndex(4));

            if (gameRule.getType() == Boolean.class) {
                addBooleanGameRuleEditOptions(pane, (GameRule<Boolean>) gameRule, player);
            } else if (gameRule.getType() == Integer.class) {
                addIntegerGameRuleEditOptions(pane, (GameRule<Integer>) gameRule, player);
            }

            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
            pane.addItem(new GuiItem(backItem, event -> {
                saveCurrentValues();
                getGui().show(player);
            }), Slot.fromIndex(8));

            editGui.addPane(pane);
            editGui.show(player);
        }

        private void addBooleanGameRuleEditOptions(StaticPane pane, GameRule<Boolean> gameRule, Player player) {
            Boolean currentValue = null;
            try {
                currentValue = currentWorld.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // Should not happen as the rule was already verified in openGameRuleEditMenu
                return;
            }

            ItemStack trueItem = new ItemBuilder(Material.LIME_DYE)
                    .name("&aTrue")
                    .lore(currentValue != null && currentValue ? "&7Current Value" : "")
                    .build();
            pane.addItem(new GuiItem(trueItem, event -> {
                try {
                    currentWorld.setGameRule(gameRule, true);
                    openGameRuleEditMenu(player, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(3));

            ItemStack falseItem = new ItemBuilder(Material.RED_DYE)
                    .name("&cFalse")
                    .lore(currentValue != null && !currentValue ? "&7Current Value" : "")
                    .build();
            pane.addItem(new GuiItem(falseItem, event -> {
                try {
                    currentWorld.setGameRule(gameRule, false);
                    openGameRuleEditMenu(player, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(5));
        }

        private void addIntegerGameRuleEditOptions(StaticPane pane, GameRule<Integer> gameRule, Player player) {
            int currentValue = 0;
            Integer tempVal = null;
            try {
                tempVal = currentWorld.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // Should not happen as the rule was already verified in openGameRuleEditMenu
                return;
            }
            if (tempVal != null) currentValue = tempVal;

            ItemStack currentValueItem = new ItemBuilder(Material.PAPER)
                    .name("&eCurrent Value: " + currentValue)
                    .build();
            pane.addItem(new GuiItem(currentValueItem), Slot.fromIndex(4));

            ItemStack increaseItem = new ItemBuilder(Material.ARROW)
                    .name("&aIncrease")
                    .build();
            pane.addItem(new GuiItem(increaseItem, event -> {
                try {
                    // compute current value at click time
                    Integer cur = currentWorld.getGameRuleValue(gameRule);
                    int newVal = (cur == null ? 0 : cur) + 1;
                    currentWorld.setGameRule(gameRule, newVal);
                    openGameRuleEditMenu(player, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(3));

            ItemStack decreaseItem = new ItemBuilder(Material.ARROW)
                    .name("&cDecrease")
                    .build();
            pane.addItem(new GuiItem(decreaseItem, event -> {
                try {
                    Integer cur = currentWorld.getGameRuleValue(gameRule);
                    int newVal = (cur == null ? 0 : cur) - 1;
                    currentWorld.setGameRule(gameRule, newVal);
                    openGameRuleEditMenu(player, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(5));

            ItemStack setItem = new ItemBuilder(Material.ANVIL)
                    .name("&eSet Value")
                    .build();
            pane.addItem(new GuiItem(setItem, event -> {
                player.closeInventory();
                WorldManager.langConfig.sendWaiting(player, "gui.enter_game_rule_value", gameRule.getName());
                chatInputPlayers.put(player, new ChatEntry(currentWorld, gameRule));
            }), Slot.fromIndex(4));
        }

        private static void showGameRuleGui(Player player, World world) {
            // Create GUI directly without creating new GameRuleEditor instance to avoid re-registering listener
            ChestGui gui = new ChestGui(6, "Game Rules for " + world.getName());
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 6);

            List<GameRule<?>> gameRules = List.of(GameRule.values());
            int rulesPerPage = 45;
            int pageCount = (int) Math.ceil((double) gameRules.size() / rulesPerPage);

            for (int page = 0; page < pageCount; page++) {
                StaticPane pane = new StaticPane(0, 0, 9, 6);
                int start = page * rulesPerPage;
                int end = Math.min(start + rulesPerPage, gameRules.size());
                int addedItems = 0;

                for (int index = start; index < end; index++) {
                    GameRule<?> gameRule = gameRules.get(index);

                    // Skip GameRules that are not available in this version
                    Object ruleValue = null;
                    try {
                        ruleValue = world.getGameRuleValue(gameRule);
                    } catch (IllegalArgumentException e) {
                        // Skip unavailable GameRule
                        continue;
                    }

                    ItemStack itemStack = new ItemBuilder(Material.PAPER)
                            .name("&e" + gameRule.getName())
                            .lore("&7Current Value: &b" + ruleValue)
                            .build();

                    pane.addItem(new GuiItem(itemStack, event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            openGameRuleEditMenu(p, world, gameRule);
                        }
                    }), Slot.fromIndex(addedItems));
                    addedItems++;
                }

                paginatedPane.addPane(page, pane);
            }

            gui.addPane(paginatedPane);

            if (pageCount > 0) {
                paginatedPane.setPage(0);
            }

            StaticPane navigationPane = new StaticPane(0, 5, 9, 1);
            ItemStack prevPageItem = new ItemBuilder(Material.MAGENTA_DYE).name("&aPrevious Page").build();
            navigationPane.addItem(new GuiItem(prevPageItem, event -> {
                if (paginatedPane.getPage() > 0) {
                    paginatedPane.setPage(paginatedPane.getPage() - 1);
                    gui.update();
                }
            }), Slot.fromIndex(3));

            ItemStack nextPageItem = new ItemBuilder(Material.LIME_DYE).name("&aNext Page").build();
            navigationPane.addItem(new GuiItem(nextPageItem, event -> {
                if (paginatedPane.getPage() < pageCount - 1) {
                    paginatedPane.setPage(paginatedPane.getPage() + 1);
                    gui.update();
                }
            }), Slot.fromIndex(5));

            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
            navigationPane.addItem(new GuiItem(backItem, event -> {
                if (event.getWhoClicked() instanceof Player p) {
                    new EditorOptions(world).getGui().show(p);
                }
            }), Slot.fromIndex(8));

            gui.addPane(navigationPane);
            gui.show(player);
        }

        private static void openGameRuleEditMenu(Player player, World world, GameRule<?> gameRule) {
            ChestGui editGui = new ChestGui(3, "Edit " + gameRule.getName());
            editGui.setOnGlobalClick(event -> event.setCancelled(true));

            StaticPane pane = new StaticPane(0, 0, 9, 3);

            Object tempValue = null;
            try {
                tempValue = world.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // GameRule is not available
                ItemStack errorItem = new ItemBuilder(Material.BARRIER)
                        .name("&cGameRule Not Available")
                        .lore("&7This GameRule is not available in this version")
                        .build();
                pane.addItem(new GuiItem(errorItem), Slot.fromIndex(4));
                ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
                pane.addItem(new GuiItem(backItem, event -> {
                    showGameRuleGui(player, world);
                }), Slot.fromIndex(8));
                editGui.addPane(pane);
                editGui.show(player);
                return;
            }

            String displayValue = tempValue == null ? "null" : tempValue.toString();
            ItemStack currentValueItem = new ItemBuilder(Material.PAPER)
                    .name("&eCurrent Value: " + displayValue)
                    .build();
            pane.addItem(new GuiItem(currentValueItem), Slot.fromIndex(4));

            if (gameRule.getType() == Boolean.class) {
                addBooleanGameRuleEditOptions(pane, (GameRule<Boolean>) gameRule, player, world);
            } else if (gameRule.getType() == Integer.class) {
                addIntegerGameRuleEditOptions(pane, (GameRule<Integer>) gameRule, player, world);
            }

            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
            pane.addItem(new GuiItem(backItem, event -> {
                showGameRuleGui(player, world);
            }), Slot.fromIndex(8));

            editGui.addPane(pane);
            editGui.show(player);
        }

        private static void addBooleanGameRuleEditOptions(StaticPane pane, GameRule<Boolean> gameRule, Player player, World world) {
            Boolean currentValue = null;
            try {
                currentValue = world.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // Should not happen as the rule was already verified in openGameRuleEditMenu
                return;
            }

            ItemStack trueItem = new ItemBuilder(Material.LIME_DYE)
                    .name("&aTrue")
                    .lore(currentValue != null && currentValue ? "&7Current Value" : "")
                    .build();
            pane.addItem(new GuiItem(trueItem, event -> {
                try {
                    world.setGameRule(gameRule, true);
                    openGameRuleEditMenu(player, world, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(3));

            ItemStack falseItem = new ItemBuilder(Material.RED_DYE)
                    .name("&cFalse")
                    .lore(currentValue != null && !currentValue ? "&7Current Value" : "")
                    .build();
            pane.addItem(new GuiItem(falseItem, event -> {
                try {
                    world.setGameRule(gameRule, false);
                    openGameRuleEditMenu(player, world, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(5));
        }

        private static void addIntegerGameRuleEditOptions(StaticPane pane, GameRule<Integer> gameRule, Player player, World world) {
            Integer tempVal = null;
            try {
                tempVal = world.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException e) {
                // Should not happen as the rule was already verified in openGameRuleEditMenu
                return;
            }
            int currentValue = tempVal == null ? 0 : tempVal;

            ItemStack currentValueItem = new ItemBuilder(Material.PAPER)
                    .name("&eCurrent Value: " + currentValue)
                    .build();
            pane.addItem(new GuiItem(currentValueItem), Slot.fromIndex(4));

            ItemStack increaseItem = new ItemBuilder(Material.ARROW)
                    .name("&aIncrease")
                    .build();
            pane.addItem(new GuiItem(increaseItem, event -> {
                try {
                    Integer cur = world.getGameRuleValue(gameRule);
                    int newVal = (cur == null ? 0 : cur) + 1;
                    world.setGameRule(gameRule, newVal);
                    openGameRuleEditMenu(player, world, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(3));

            ItemStack decreaseItem = new ItemBuilder(Material.ARROW)
                    .name("&cDecrease")
                    .build();
            pane.addItem(new GuiItem(decreaseItem, event -> {
                try {
                    Integer cur = world.getGameRuleValue(gameRule);
                    int newVal = (cur == null ? 0 : cur) - 1;
                    world.setGameRule(gameRule, newVal);
                    openGameRuleEditMenu(player, world, gameRule);
                } catch (IllegalArgumentException e) {
                    // Silently fail
                }
            }), Slot.fromIndex(5));

            ItemStack setItem = new ItemBuilder(Material.ANVIL)
                    .name("&eSet Value")
                    .build();
            pane.addItem(new GuiItem(setItem, event -> {
                player.closeInventory();
                WorldManager.langConfig.sendWaiting(player, "gui.enter_game_rule_value", gameRule.getName());
                chatInputPlayers.put(player, new ChatEntry(world, gameRule));
            }), Slot.fromIndex(4));
        }

        private void saveCurrentValues() {
            // This method can be used to save the current values to persistent storage if needed.
            // For now, it just logs the current game rule values.
            for (GameRule<?> gameRule : GameRule.values()) {
                try {
                    System.out.println("GameRule: " + gameRule.getName() + " = " + currentWorld.getGameRuleValue(gameRule));
                } catch (IllegalArgumentException e) {
                    // GameRule is not available in this version, skip it
                }
            }
        }

        private boolean isGameRuleAvailable(GameRule<?> gameRule) {
            try {
                currentWorld.getGameRuleValue(gameRule);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public ChestGui getGui() {
            // Create the GUI directly without creating recursion
            ChestGui gui = new ChestGui(6, "Game Rules for " + currentWorld.getName());
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 6);

            List<GameRule<?>> gameRules = List.of(GameRule.values());
            int rulesPerPage = 45;
            int pageCount = (int) Math.ceil((double) gameRules.size() / rulesPerPage);

            for (int page = 0; page < pageCount; page++) {
                StaticPane pane = new StaticPane(0, 0, 9, 6);
                int start = page * rulesPerPage;
                int end = Math.min(start + rulesPerPage, gameRules.size());
                int addedItems = 0;

                for (int index = start; index < end; index++) {
                    GameRule<?> gameRule = gameRules.get(index);

                    // Skip GameRules that are not available in this version
                    if (!isGameRuleAvailable(gameRule)) {
                        continue;
                    }

                    Object ruleValue = null;
                    try {
                        ruleValue = currentWorld.getGameRuleValue(gameRule);
                    } catch (IllegalArgumentException e) {
                        // Skip unavailable GameRule
                        continue;
                    }

                    ItemStack itemStack = new ItemBuilder(Material.PAPER)
                            .name("&e" + gameRule.getName())
                            .lore("&7Current Value: &b" + ruleValue)
                            .build();

                    pane.addItem(new GuiItem(itemStack, event -> handleGameRuleClick(event, gameRule)), Slot.fromIndex(addedItems));
                    addedItems++;
                }

                paginatedPane.addPane(page, pane);
            }

            gui.addPane(paginatedPane);

            if (pageCount > 0) {
                paginatedPane.setPage(0);
            }

            StaticPane navigationPane = new StaticPane(0, 5, 9, 1);
            ItemStack prevPageItem = new ItemBuilder(Material.MAGENTA_DYE).name("&aPrevious Page").build();
            navigationPane.addItem(new GuiItem(prevPageItem, event -> {
                if (paginatedPane.getPage() > 0) {
                    paginatedPane.setPage(paginatedPane.getPage() - 1);
                    gui.update();
                }
            }), Slot.fromIndex(3));

            ItemStack nextPageItem = new ItemBuilder(Material.LIME_DYE).name("&aNext Page").build();
            navigationPane.addItem(new GuiItem(nextPageItem, event -> {
                if (paginatedPane.getPage() < pageCount - 1) {
                    paginatedPane.setPage(paginatedPane.getPage() + 1);
                    gui.update();
                }
            }), Slot.fromIndex(5));

            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
            navigationPane.addItem(new GuiItem(backItem, event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    saveCurrentValues();
                    new EditorOptions(currentWorld).getGui().show(player);
                }
            }), Slot.fromIndex(8));

            gui.addPane(navigationPane);
            return gui;
        }
    }


}
