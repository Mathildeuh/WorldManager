package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

public class GUIGameRuleSettings implements Listener {
    private final WorldManager plugin;
    private final World world;

    public GUIGameRuleSettings(WorldManager plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("deprecation")
    public void open(Player player) {
        List<String> gameRules = Arrays.stream(world.getGameRules())
                .sorted(String::compareToIgnoreCase) // Trier les game rules par ordre alphabétique
                .toList();

        int menuSize = ((gameRules.size() + 8) / 9) * 9;
        if (menuSize > 54) menuSize = 54;

        SGMenu gameRuleMenu = WorldManager.getSpiGUI().create("&aGame Rules for " + world.getName(), menuSize / 9);

        for (int i = 0; i < gameRules.size(); i++) {
            String gameRule = gameRules.get(i);
            boolean isEnabled = Boolean.parseBoolean(world.getGameRuleValue(gameRule));

            gameRuleMenu.setButton(i, new SGButton(new ItemBuilder(isEnabled ? Material.BLUE_WOOL : Material.RED_WOOL)
                    .name(isEnabled ? "§a" + gameRule : "§c" + gameRule)
                    .lore(isEnabled ? "§7Click to §cdisable" : "§7Click to §aenable")
                    .build()
            ).withListener(event -> {
                world.setGameRuleValue(gameRule, String.valueOf(!isEnabled));
                open(player);
            }));


        }

        if (gameRuleMenu.getButton(menuSize - 1) == null) {
            gameRuleMenu.setButton(menuSize - 1, new SGButton(new ItemBuilder(Material.BARRIER)
                    .name("§cBack")
                    .lore("§7Click to open main menu")
                    .build()
            ).withListener(event -> {
                new GUIGameRules(plugin).open(player); // Open the main menu
            }));
        }

        player.openInventory(gameRuleMenu.getInventory());
    }
}
