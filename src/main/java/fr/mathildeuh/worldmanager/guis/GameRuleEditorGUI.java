package fr.mathildeuh.worldmanager.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ClassCanBeRecord")
public class GameRuleEditorGUI implements Listener {
    private final World currentWorld;
    private static final Map<Player, ChatEntry> chatInputPlayers = new HashMap<>();
    private static boolean chatListenerRegistered = false;

    private record ChatEntry(World world, GameRule<Integer> rule) {
    }

    public GameRuleEditorGUI(World currentWorld) {
        this.currentWorld = currentWorld;
        synchronized (GameRuleEditorGUI.class) {
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
                                WorldManager.langConfig.sendSuccess(player, "gui.game_rule_set", entry.rule.key().asString(), String.valueOf(newValue));
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

    public ChestGui getGui() {
        return createGameRuleGui();
    }

    private ChestGui createGameRuleGui() {
        ChestGui gui = new ChestGui(6, "Game Rules for " + currentWorld.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 6);

        // Get all GameRules available
        @SuppressWarnings("removal")
        List<GameRule<?>> gameRules = java.util.Arrays.asList(GameRule.values());
        int rulesPerPage = 45;
        int pageCount = (int) Math.ceil((double) gameRules.size() / rulesPerPage);

        for (int page = 0; page < pageCount; page++) {
            StaticPane pane = new StaticPane(0, 0, 9, 6);
            int start = page * rulesPerPage;
            int end = Math.min(start + rulesPerPage, gameRules.size());
            int addedItems = 0;

            for (int index = start; index < end; index++) {
                GameRule<?> gameRule = gameRules.get(index);

                if (!isGameRuleAvailable(gameRule)) {
                    continue;
                }

                Object ruleValue;
                try {
                    ruleValue = currentWorld.getGameRuleValue(gameRule);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                ItemStack itemStack = new ItemBuilder(Material.PAPER)
                        .name("&e" + gameRule.key().asString())
                        .lore("&7Current Value: &b" + ruleValue)
                        .build();

                pane.addItem(new GuiItem(itemStack, event -> openEditMenu((Player) event.getWhoClicked(), gameRule)), Slot.fromIndex(addedItems));
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
                saveGameRules();
                new EditorOptionsGUI(currentWorld).getGui().show(player);
            }
        }), Slot.fromIndex(8));

        gui.addPane(navigationPane);
        return gui;
    }

    @SuppressWarnings("unchecked")
    private void openEditMenu(Player player, GameRule<?> gameRule) {
        ChestGui editGui = new ChestGui(3, "Edit " + gameRule.key().asString());
        editGui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        Object tempValue;
        try {
            tempValue = currentWorld.getGameRuleValue(gameRule);
        } catch (IllegalArgumentException e) {
            ItemStack errorItem = new ItemBuilder(Material.BARRIER)
                    .name("&cGameRule Not Available")
                    .lore("&7This GameRule is not available in this version")
                    .build();
            pane.addItem(new GuiItem(errorItem), Slot.fromIndex(4));
            ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
            pane.addItem(new GuiItem(backItem, event -> getGui().show(player)), Slot.fromIndex(8));
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
            addBooleanOptions(pane, (GameRule<Boolean>) gameRule, player);
        } else if (gameRule.getType() == Integer.class) {
            addIntegerOptions(pane, (GameRule<Integer>) gameRule, player);
        }

        ItemStack backItem = new ItemBuilder(Material.DARK_OAK_DOOR).name("&cBack").build();
        pane.addItem(new GuiItem(backItem, event -> getGui().show(player)), Slot.fromIndex(8));

        editGui.addPane(pane);
        editGui.show(player);
    }

    private void addBooleanOptions(StaticPane pane, GameRule<Boolean> gameRule, Player player) {
        Boolean currentValue;
        try {
            currentValue = currentWorld.getGameRuleValue(gameRule);
        } catch (IllegalArgumentException e) {
            return;
        }

        ItemStack trueItem = new ItemBuilder(Material.LIME_DYE)
                .name("&aTrue")
                .lore(currentValue != null && currentValue ? "&7Current Value" : "")
                .build();
        pane.addItem(new GuiItem(trueItem, event -> {
            try {
                currentWorld.setGameRule(gameRule, true);
                openEditMenu(player, gameRule);
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
                openEditMenu(player, gameRule);
            } catch (IllegalArgumentException e) {
                // Silently fail
            }
        }), Slot.fromIndex(5));
    }

    private void addIntegerOptions(StaticPane pane, GameRule<Integer> gameRule, Player player) {
        try {
            currentWorld.getGameRuleValue(gameRule);
        } catch (IllegalArgumentException e) {
            return;
        }

        ItemStack increaseItem = new ItemBuilder(Material.ARROW)
                .name("&aIncrease")
                .build();
        pane.addItem(new GuiItem(increaseItem, event -> {
            try {
                Integer cur = currentWorld.getGameRuleValue(gameRule);
                int newVal = (cur == null ? 0 : cur) + 1;
                currentWorld.setGameRule(gameRule, newVal);
                openEditMenu(player, gameRule);
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
                openEditMenu(player, gameRule);
            } catch (IllegalArgumentException e) {
                // Silently fail
            }
        }), Slot.fromIndex(5));

        ItemStack setItem = new ItemBuilder(Material.ANVIL)
                .name("&eSet Value")
                .build();
        pane.addItem(new GuiItem(setItem, event -> {
            player.closeInventory();
            WorldManager.langConfig.sendWaiting(player, "gui.enter_game_rule_value", gameRule.key().asString());
            chatInputPlayers.put(player, new ChatEntry(currentWorld, gameRule));
        }), Slot.fromIndex(4));
    }

    private static void showGameRuleGui(Player player, World world) {
        new GameRuleEditorGUI(world).getGui().show(player);
    }

    private void saveGameRules() {
        String worldName = currentWorld.getName();
        String gameRulesPath = "worlds." + worldName + ".gameRules";

        @SuppressWarnings("removal")
        GameRule<?>[] allRules = GameRule.values();
        for (GameRule<?> gameRule : allRules) {
            try {
                Object value = currentWorld.getGameRuleValue(gameRule);
                WorldManager.worldsConfig.set(gameRulesPath + "." + gameRule.key().asString(), value);
            } catch (IllegalArgumentException e) {
                // GameRule is not available in this version, skip it
            }
        }

        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (java.io.IOException e) {
            System.err.println("Failed to save game rules: " + e.getMessage());
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
}

