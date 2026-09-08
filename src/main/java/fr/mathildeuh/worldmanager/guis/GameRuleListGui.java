package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Every game rule for a world in one flat, alphabetised, paginated list. */
public final class GameRuleListGui implements WMGui {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final Player player;
    private final String worldName;
    private final Inventory inventory;
    private final List<GameRule<?>> rulesOnPage;
    private final GuiUtils.Page page;

    private GameRuleListGui(Player player, String worldName, int requestedPage) {
        this.player = player;
        this.worldName = worldName;
        World world = Bukkit.getWorld(worldName);
        List<GameRule<?>> rules = world == null ? List.of() : availableRules(world);
        this.page = GuiUtils.paginate(requestedPage, rules.size(), PAGE_SIZE);
        this.rulesOnPage = rules.subList(page.fromInclusive(), page.toExclusive());

        Component title = page.count() > 1
                ? GuiUtils.miniFromLang("gui.gamerules.rules_title_paged", worldName, page.index() + 1, page.count())
                : GuiUtils.miniFromLang("gui.gamerules.category_title", worldName);
        this.inventory = Bukkit.createInventory(null, 54, title);
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.INFO);

        if (world != null) {
            for (int i = 0; i < rulesOnPage.size(); i++) {
                inventory.setItem(i, ruleItem(world, rulesOnPage.get(i)));
            }
        }

        if (page.index() > 0) {
            inventory.setItem(PREV_SLOT, GuiUtils.item(Material.ARROW, GuiUtils.miniFromLang("gui.editor.previous")));
        }
        inventory.setItem(BACK_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.back")));
        if (page.index() < page.count() - 1) {
            inventory.setItem(NEXT_SLOT, GuiUtils.item(Material.ARROW, GuiUtils.miniFromLang("gui.editor.next")));
        }
    }

    public static void open(Player player, String worldName, int page) {
        GuiManager.open(player, new GameRuleListGui(player, worldName, page));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == PREV_SLOT && page.index() > 0) {
            GameRuleListGui.open(player, worldName, page.index() - 1);
        } else if (slot == BACK_SLOT) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                saveGameRules(world);
            }
            WorldOptionsGui.open(player, worldName);
        } else if (slot == NEXT_SLOT && page.index() < page.count() - 1) {
            GameRuleListGui.open(player, worldName, page.index() + 1);
        } else if (slot < rulesOnPage.size()) {
            GameRuleEditGui.open(player, worldName, rulesOnPage.get(slot), page.index());
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack ruleItem(World world, GameRule<?> rule) {
        String readable = GuiUtils.readableName(rule.getName());
        Material icon;
        Component name;
        if (rule.getType() == Boolean.class) {
            boolean value = Boolean.TRUE.equals(world.getGameRuleValue((GameRule<Boolean>) rule));
            icon = value ? Material.LIME_DYE : Material.GRAY_DYE;
            name = Component.text((value ? "✔ " : "✘ ") + readable, value ? NamedTextColor.GREEN : NamedTextColor.RED);
        } else if (rule.getType() == Integer.class) {
            Integer value = world.getGameRuleValue((GameRule<Integer>) rule);
            icon = Material.REPEATER;
            name = Component.text(readable + ": " + value, NamedTextColor.AQUA);
        } else {
            icon = Material.PAPER;
            name = Component.text(readable, NamedTextColor.WHITE);
        }
        return GuiUtils.item(icon, name, Component.text(rule.getName(), NamedTextColor.GRAY));
    }

    @SuppressWarnings("removal")
    static List<GameRule<?>> availableRules(World world) {
        List<GameRule<?>> rules = new ArrayList<>();
        for (GameRule<?> rule : GameRule.values()) {
            try {
                world.getGameRuleValue(rule);
                rules.add(rule);
            } catch (IllegalArgumentException ignored) {
                // Not available for this world/version.
            }
        }
        rules.sort(Comparator.comparing(GameRule::getName, String.CASE_INSENSITIVE_ORDER));
        return rules;
    }

    static void saveGameRules(World world) {
        for (GameRule<?> rule : availableRules(world)) {
            Object value = world.getGameRuleValue(rule);
            WorldManager.worldsConfig.set("worlds." + world.getName() + ".gameRules." + rule.getName(), value);
        }
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[WorldManager] Failed to save game rules for " + world.getName() + ": " + e.getMessage());
        }
    }
}
