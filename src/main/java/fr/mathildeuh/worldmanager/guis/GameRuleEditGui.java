package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.util.GameRuleUpdates;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.UnaryOperator;
import java.util.logging.Level;

/** Editor for a single game rule: boolean toggle or integer +/- stepper, applied immediately. */
public final class GameRuleEditGui implements WMGui {

    private static final int DISPLAY_SLOT = 13;
    private static final int MINUS_TEN_SLOT = 10;
    private static final int MINUS_ONE_SLOT = 11;
    private static final int PLUS_ONE_SLOT = 15;
    private static final int PLUS_TEN_SLOT = 16;
    private static final int SET_EXACT_SLOT = 20;
    private static final int DONE_SLOT = 24;

    private final Player player;
    private final String worldName;
    private final GameRule<?> rule;
    private final int returnPage;
    private final Inventory inventory;
    private final World editingWorld;

    private GameRuleEditGui(Player player, String worldName, GameRule<?> rule, int returnPage) {
        this.player = player;
        this.worldName = worldName;
        this.rule = rule;
        this.returnPage = returnPage;

        String readable = GuiUtils.readableName(rule.getName());
        this.inventory = Bukkit.createInventory(null, 27, GuiUtils.mini("<yellow><b>" + readable + "</b></yellow>"));
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.INFO);

        World world = Bukkit.getWorld(worldName);
        this.editingWorld = world;
        if (world == null) {
            inventory.setItem(DISPLAY_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.miniFromLang("gui.options.not_loaded")));
        } else if (rule.getType() == Boolean.class) {
            renderBoolean(world);
        } else if (rule.getType() == Integer.class) {
            renderInteger(world);
        } else {
            inventory.setItem(DISPLAY_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.miniFromLang("gui.gamerules.not_available")));
        }

        inventory.setItem(DONE_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.gamerules.done")));
    }

    public static void open(Player player, String worldName, GameRule<?> rule, int returnPage) {
        GuiManager.open(player, new GameRuleEditGui(player, worldName, rule, returnPage));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @SuppressWarnings("unchecked")
    private void renderBoolean(World world) {
        GameRule<Boolean> boolRule = (GameRule<Boolean>) rule;
        boolean value = Boolean.TRUE.equals(world.getGameRuleValue(boolRule));
        inventory.setItem(DISPLAY_SLOT, GuiUtils.item(
                value ? Material.LIME_DYE : Material.GRAY_DYE,
                value ? GuiUtils.miniFromLang("gui.gamerules.true") : GuiUtils.miniFromLang("gui.gamerules.false"),
                GuiUtils.mini("<gray>" + rule.getName() + "</gray>"),
                GuiUtils.miniFromLang("gui.gamerules.click_to_toggle")));
    }

    @SuppressWarnings("unchecked")
    private void renderInteger(World world) {
        GameRule<Integer> intRule = (GameRule<Integer>) rule;
        Integer value = world.getGameRuleValue(intRule);
        inventory.setItem(DISPLAY_SLOT, GuiUtils.item(Material.REPEATER,
                GuiUtils.miniFromLang("gui.gamerules.current_value", value),
                GuiUtils.mini("<gray>" + rule.getName() + "</gray>")));
        inventory.setItem(MINUS_TEN_SLOT, GuiUtils.item(Material.RED_CONCRETE, GuiUtils.miniFromLang("gui.gamerules.decrease_10")));
        inventory.setItem(MINUS_ONE_SLOT, GuiUtils.item(Material.RED_CONCRETE, GuiUtils.miniFromLang("gui.gamerules.decrease_1")));
        inventory.setItem(PLUS_ONE_SLOT, GuiUtils.item(Material.LIME_CONCRETE, GuiUtils.miniFromLang("gui.gamerules.increase_1")));
        inventory.setItem(PLUS_TEN_SLOT, GuiUtils.item(Material.LIME_CONCRETE, GuiUtils.miniFromLang("gui.gamerules.increase_10")));
        inventory.setItem(SET_EXACT_SLOT, GuiUtils.item(Material.ANVIL, GuiUtils.miniFromLang("gui.gamerules.set_exact")));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == DONE_SLOT) {
            GameRuleListGui.open(player, worldName, returnPage);
            return;
        }

        if (editingWorld == null) return;

        if (rule.getType() == Boolean.class && slot == DISPLAY_SLOT) {
            updateRule((GameRule<Boolean>) rule, current -> !current, this);
        } else if (rule.getType() == Integer.class) {
            GameRule<Integer> intRule = (GameRule<Integer>) rule;
            if (slot == SET_EXACT_SLOT) {
                promptExactValue(intRule);
                return;
            }
            Integer delta = switch (slot) {
                case MINUS_TEN_SLOT -> -10;
                case MINUS_ONE_SLOT -> -1;
                case PLUS_ONE_SLOT -> 1;
                case PLUS_TEN_SLOT -> 10;
                default -> null;
            };
            if (delta != null) updateRule(intRule, current -> GameRuleUpdates.adjustInteger(current, delta), this);
        }
    }

    private <T> void updateRule(GameRule<T> typedRule, UnaryOperator<T> change, WMGui expectedScreen) {
        GameRuleUpdates.update(editingWorld, typedRule, change).whenComplete((changed, error) -> {
            if (error != null) {
                WorldManager.getInstance().getLogger().log(Level.WARNING,
                        "Could not update game rule " + rule.getName() + " in world " + worldName, error);
            }
            SchedulerUtil.runEntity(player, () -> {
                if (error != null || !Boolean.TRUE.equals(changed)) {
                    WorldManager.langConfig.sendError(player, "gui.gamerules.update_failed");
                    GuiUtils.playError(player);
                }
                if (GuiManager.get(player.getUniqueId()) == expectedScreen) {
                    if (expectedScreen == this && Bukkit.getWorld(worldName) == editingWorld) {
                        // Keep the same screen for rapid clicks; an earlier refresh must not hide a later result.
                        if (rule.getType() == Boolean.class) renderBoolean(editingWorld);
                        else if (rule.getType() == Integer.class) renderInteger(editingWorld);
                    } else {
                        GameRuleEditGui.open(player, worldName, rule, returnPage);
                    }
                }
            });
        });
    }

    private void promptExactValue(GameRule<Integer> intRule) {
        ChatInput.open(player, text -> {
            if (text != null) {
                try {
                    int value = Math.max(0, Integer.parseInt(text));
                    updateRule(intRule, current -> value, GuiManager.get(player.getUniqueId()));
                    return;
                } catch (NumberFormatException e) {
                    WorldManager.langConfig.sendError(player, "gui.gamerules.invalid_value");
                    GuiUtils.playError(player);
                }
            }
            GameRuleEditGui.open(player, worldName, rule, returnPage);
        }, GuiUtils.miniFromLang("gui.chat_input.gamerule_prompt"), GuiUtils.mini("<gray>" + rule.getName() + "</gray>"));
    }
}
