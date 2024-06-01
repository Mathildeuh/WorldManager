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

import java.util.List;

public class GUIGameRules implements Listener {
    private final WorldManager plugin;

    public GUIGameRules(WorldManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<World> worlds = Bukkit.getWorlds();
        worlds.sort((w1, w2) -> w1.getName().compareToIgnoreCase(w2.getName())); // Trier les mondes par ordre alphabétique

        int menuSize = ((worlds.size() + 8) / 9) * 9; // Calculer la taille du menu en arrondissant vers le haut au prochain multiple de 9
        if (menuSize > 54) menuSize = 54; // Limiter la taille maximale à 54 (6 lignes)

        SGMenu mainMenu = WorldManager.getSpiGUI().create("&aGame rules", menuSize / 9);

        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);
            mainMenu.setButton(i, new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a" + world.getName())
                    .lore("§7Click to manage game rules")
                    .build()
            ).withListener(event -> {
                new GUIGameRuleSettings(plugin, world).open(player);
            }));
        }

        player.openInventory(mainMenu.getInventory());
    }
}
