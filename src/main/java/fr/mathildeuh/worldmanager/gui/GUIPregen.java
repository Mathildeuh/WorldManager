package fr.mathildeuh.worldmanager.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.subcommands.pregen.Pregen;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GUIPregen implements Listener {

    private final Plugin plugin;
    private final Map<Player, String> playerWorldSelections;
    private final Map<Player, String> playerRadiusInput;
    private final Map<Player, String> playerCenterInput;
    SGMenu optionsMenu = WorldManager.getSpiGUI().create(" az ", 1);
    private MessageManager message;

    public GUIPregen(Plugin plugin) {
        this.plugin = plugin;
        this.playerWorldSelections = new HashMap<>();
        this.playerRadiusInput = new HashMap<>();
        this.playerCenterInput = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        this.message = new MessageManager(player);

        List<String> worldNames = Bukkit.getWorlds().stream().map(org.bukkit.World::getName).collect(Collectors.toList());
        int menuSize = Math.min((worldNames.size() + 8) / 9 * 9, 54); // Taille dynamique en fonction du nombre de mondes, avec une limite de 54
        if (menuSize <= 9) {
            menuSize = 18;
        }

        SGMenu worldMenu = WorldManager.getSpiGUI().create("&aSelect a World", menuSize / 9);

        for (String worldName : worldNames) {
            SGButton worldButton = new SGButton(new ItemBuilder(Material.GRASS_BLOCK)
                    .name(worldName)
                    .build());
            worldButton.withListener(event -> openOptionsMenu(player, worldName));
            worldMenu.addButton(worldButton);
        }

        player.openInventory(worldMenu.getInventory());
    }

    private void openOptionsMenu(Player player, String worldName) {

        optionsMenu.setName("&aOptions for " + worldName);


        SGButton startButton = new SGButton(new ItemBuilder(Material.GREEN_CONCRETE)
                .name("&aConfirm")
                .lore("&7Start pregenerating the world", "&7Click for enter parameters and start pregenerating.")
                .build());
        startButton.withListener(event -> {
            playerWorldSelections.put(player, worldName);
            message.parse(MessageManager.MessageType.CUSTOM, "<color:#7471b0>➥</color> <yellow>Enter the radius in chat (e.g., 20) <color:#7471b0>⬇ </color></yellow> \n" +
                    "<dark_red>⚠ Too high number can cause crashes ⚠</dark_red>");
            player.closeInventory();
        });
        optionsMenu.setButton(3, startButton);

        SGButton cancelButton = new SGButton(new ItemBuilder(Material.RED_CONCRETE)
                .name("&cCancel")
                .lore("&7Cancel pregeneration")
                .build());
        cancelButton.withListener(event -> player.closeInventory());
        optionsMenu.setButton(5, cancelButton);

        player.openInventory(optionsMenu.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getCurrentItem() != null && event.getClickedInventory().getType() == InventoryType.CHEST) {
            Player player = (Player) event.getWhoClicked();
            if (event.getView().getTitle().equals("&aSelect a World")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> openOptionsMenu(player, event.getCurrentItem().getItemMeta().getDisplayName()));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("&aOptions for")) {
            playerWorldSelections.remove((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (playerWorldSelections.containsKey(player)) {
            String message = event.getMessage().trim();
            if (message.matches("-?\\d+(,\\d+)?")) {
                if (!playerRadiusInput.containsKey(player)) {
                    playerRadiusInput.put(player, message);
                    this.message.parse(MessageManager.MessageType.CUSTOM, "<color:#7471b0>➥</color> <yellow>Enter the center in chat (e.g., 100,100)<color:#7471b0>⬇ </color></yellow>");
                } else if (!playerCenterInput.containsKey(player)) {
                    playerCenterInput.put(player, message);
                    String worldName = playerWorldSelections.get(player);
                    String radius = playerRadiusInput.get(player);
                    String center = playerCenterInput.get(player);
                    new Pregen(player).execute(new String[]{"pregen", "start", worldName, radius, center});
                    playerRadiusInput.remove(player);
                    playerCenterInput.remove(player);
                }
                event.setCancelled(true);
            } else {
                this.message.parse(MessageManager.MessageType.ERROR, "Invalid input. Please enter a number or a number followed by a comma and another number.");
            }
        }
    }


}
