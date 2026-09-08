package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.Create;
import fr.mathildeuh.worldmanager.util.WorldFolders;
import fr.mathildeuh.worldmanager.util.WorldNameValidator;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * World-creation form. Unlike every other screen in this package, this one is NOT rebuilt
 * from scratch on every open: a chest inventory has no native multi-field form the way the
 * old Dialog did, so this instance holds the draft (name/type/seed/generator/empty) itself
 * across the several round trips (each {@link AnvilInput} prompt, each type-cycle click)
 * needed to fill it in, re-rendering the same {@link Inventory} in place each time.
 */
public final class CreatorGui implements WMGui {

    private static final List<String> TYPES = List.of("normal", "flat", "large_biomes", "amplified", "nether", "the_end");
    private static final Set<String> RESERVED_NAMES = Set.of("plugins", "logs", "libraries", "versions", "config", "cache");

    private static final int NAME_SLOT = 11;
    private static final int TYPE_SLOT = 12;
    private static final int SEED_SLOT = 13;
    private static final int GENERATOR_SLOT = 14;
    private static final int EMPTY_SLOT = 15;
    private static final int CANCEL_SLOT = 20;
    private static final int CONFIRM_SLOT = 24;

    private final Player player;
    private final Inventory inventory;

    private String name = "";
    private String type = "normal";
    private String seed = "";
    private String generator = "";
    private boolean empty = false;

    private CreatorGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, GuiUtils.miniFromLang("gui.creator.title"));
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.CREATE);
        render();
    }

    public static void open(Player player) {
        GuiManager.open(player, new CreatorGui(player));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        Component nameValue = name.isEmpty() ? GuiUtils.miniFromLang("gui.creator.not_set") : Component.text(name, NamedTextColor.WHITE);
        inventory.setItem(NAME_SLOT, GuiUtils.item(Material.NAME_TAG,
                GuiUtils.miniFromLang("gui.creator.name"), nameValue, GuiUtils.miniFromLang("gui.creator.name_lore")));

        inventory.setItem(TYPE_SLOT, GuiUtils.item(iconForType(type),
                GuiUtils.miniFromLang("gui.creator.type"),
                Component.text(displayType(type), NamedTextColor.WHITE),
                GuiUtils.miniFromLang("gui.creator.type_lore")));

        Component seedValue = seed.isEmpty() ? GuiUtils.miniFromLang("gui.creator.random") : Component.text(seed, NamedTextColor.WHITE);
        inventory.setItem(SEED_SLOT, GuiUtils.item(Material.NETHER_STAR,
                GuiUtils.miniFromLang("gui.creator.seed"), seedValue, GuiUtils.miniFromLang("gui.creator.seed_lore")));

        Component generatorValue = generator.isEmpty() ? GuiUtils.miniFromLang("gui.creator.default_generator") : Component.text(generator, NamedTextColor.WHITE);
        inventory.setItem(GENERATOR_SLOT, GuiUtils.item(Material.COMMAND_BLOCK,
                GuiUtils.miniFromLang("gui.creator.generator"), generatorValue, GuiUtils.miniFromLang("gui.creator.generator_lore")));

        inventory.setItem(EMPTY_SLOT, GuiUtils.item(empty ? Material.LIME_DYE : Material.GRAY_DYE,
                GuiUtils.miniFromLang("gui.creator.empty"),
                empty ? GuiUtils.miniFromLang("gui.creator.empty_on") : GuiUtils.miniFromLang("gui.creator.empty_off")));

        inventory.setItem(CANCEL_SLOT, GuiUtils.item(Material.RED_CONCRETE, GuiUtils.miniFromLang("gui.creator.cancel")));
        inventory.setItem(CONFIRM_SLOT, GuiUtils.glinted(GuiUtils.item(Material.LIME_CONCRETE, GuiUtils.miniFromLang("gui.creator.confirm"))));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case NAME_SLOT -> promptName();
            case TYPE_SLOT -> cycleType(event.isRightClick());
            case SEED_SLOT -> promptSeed();
            case GENERATOR_SLOT -> promptGenerator();
            case EMPTY_SLOT -> {
                empty = !empty;
                render();
            }
            case CANCEL_SLOT -> MainMenuGui.open(player);
            case CONFIRM_SLOT -> submit();
            default -> { }
        }
    }

    private void cycleType(boolean reverse) {
        int index = TYPES.indexOf(type);
        int size = TYPES.size();
        index = reverse ? (index - 1 + size) % size : (index + 1) % size;
        type = TYPES.get(index);
        render();
    }

    private void promptName() {
        ChatInput.open(player, text -> {
            if (text != null) {
                name = text.trim();
            }
            render();
            GuiManager.open(player, this);
        }, GuiUtils.miniFromLang("gui.chat_input.name_prompt"));
    }

    private void promptSeed() {
        ChatInput.open(player, text -> {
            if (text != null) {
                seed = text.trim();
            }
            render();
            GuiManager.open(player, this);
        }, GuiUtils.miniFromLang("gui.chat_input.seed_prompt"));
    }

    private void promptGenerator() {
        ChatInput.open(player, text -> {
            if (text != null) {
                generator = text.trim();
            }
            render();
            GuiManager.open(player, this);
        }, GuiUtils.miniFromLang("gui.chat_input.generator_prompt"));
    }

    private void submit() {
        if (name.isEmpty()) {
            WorldManager.langConfig.sendError(player, "create.name_required");
            GuiUtils.playError(player);
            return;
        }
        if (RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            WorldManager.langConfig.sendError(player, "create.name_unusable");
            GuiUtils.playError(player);
            return;
        }
        if (!WorldNameValidator.isValid(name)) {
            WorldManager.langConfig.sendError(player, "general.invalid_world_name", name);
            GuiUtils.playError(player);
            return;
        }
        if (Bukkit.getWorld(name) != null) {
            WorldManager.langConfig.sendError(player, "create.already_exists");
            GuiUtils.playError(player);
            return;
        }
        if (WorldFolders.listUnloadedWorldFolders().contains(name)) {
            WorldManager.langConfig.sendError(player, "create.exists_but_not_loaded", name);
            GuiUtils.playError(player);
            return;
        }
        if (!seed.isEmpty() && !seed.matches("-?\\d+")) {
            WorldManager.langConfig.sendError(player, "create.invalid_seed");
            GuiUtils.playError(player);
            return;
        }

        player.closeInventory();
        if (empty) {
            createEmptyWorld();
        } else {
            new Create(player).execute(name, type, seed.isEmpty() ? null : seed, generator.isEmpty() ? null : generator);
        }
    }

    private void createEmptyWorld() {
        long seedValue = seed.isEmpty() ? new Random().nextLong() : Long.parseLong(seed);

        Create.sendStarting(player, "Empty", name, seed, "Empty");

        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .seed(seedValue)
                .generator(new EmptyWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            WorldManager.langConfig.sendError(player, "create.error", name);
            GuiUtils.playError(player);
            return;
        }
        WorldManager.langConfig.sendSuccess(player, "create.success", name);
        GuiUtils.playSuccess(player);
        try {
            world.save();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WorldManager] Initial save failed for newly created world " + name + ": " + e.getMessage());
        }
        WorldManager.addWorld(player, name, WorldType.FLAT.name(), World.Environment.NORMAL, "Empty");
    }

    private static Material iconForType(String type) {
        return switch (type) {
            case "flat" -> Material.SANDSTONE;
            case "large_biomes" -> Material.OAK_LEAVES;
            case "amplified" -> Material.EMERALD_BLOCK;
            case "nether" -> Material.NETHERRACK;
            case "the_end" -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    private static String displayType(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
