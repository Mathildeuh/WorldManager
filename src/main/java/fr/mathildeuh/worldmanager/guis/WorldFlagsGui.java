package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Per-world flags in the spirit of Multiverse-Core's world properties: PvP, mob/animal
 * spawning, a weather lock, and a custom spawn point (used by {@code /wm tp} instead of the
 * world's vanilla spawn once set).
 */
public final class WorldFlagsGui implements WMGui {

    private static final int PVP_SLOT = 11;
    private static final int MOB_SPAWNING_SLOT = 12;
    private static final int ANIMAL_SPAWNING_SLOT = 13;
    private static final int WEATHER_LOCKED_SLOT = 14;
    private static final int SET_SPAWN_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final Player player;
    private final String worldName;
    private final Inventory inventory;

    private WorldFlagsGui(Player player, String worldName) {
        this.player = player;
        this.worldName = worldName;
        this.inventory = Bukkit.createInventory(null, 27, GuiUtils.miniFromLang("gui.flags.title", worldName));
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.INFO);
        render();
    }

    public static void open(Player player, String worldName) {
        GuiManager.open(player, new WorldFlagsGui(player, worldName));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.setItem(PVP_SLOT, toggleItem(Material.IRON_SWORD, "gui.flags.pvp",
                WorldsConfig.getFlag(worldName, "pvp", true)));
        inventory.setItem(MOB_SPAWNING_SLOT, toggleItem(Material.ZOMBIE_HEAD, "gui.flags.mob_spawning",
                WorldsConfig.getFlag(worldName, "mobSpawning", true)));
        inventory.setItem(ANIMAL_SPAWNING_SLOT, toggleItem(Material.COW_SPAWN_EGG, "gui.flags.animal_spawning",
                WorldsConfig.getFlag(worldName, "animalSpawning", true)));
        inventory.setItem(WEATHER_LOCKED_SLOT, toggleItem(Material.SUNFLOWER, "gui.flags.weather_locked",
                WorldsConfig.getFlag(worldName, "weatherLocked", false)));
        inventory.setItem(SET_SPAWN_SLOT, GuiUtils.item(Material.COMPASS,
                GuiUtils.miniFromLang("gui.flags.set_spawn"), GuiUtils.miniFromLang("gui.flags.set_spawn_lore")));
        inventory.setItem(BACK_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.back")));
    }

    private ItemStack toggleItem(Material icon, String labelKey, boolean state) {
        Component label = GuiUtils.miniFromLang(labelKey);
        Component status = state ? GuiUtils.miniFromLang("gui.flags.enabled") : GuiUtils.miniFromLang("gui.flags.disabled");
        return GuiUtils.item(icon, label, status, GuiUtils.miniFromLang("gui.flags.click_to_toggle"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == PVP_SLOT) {
            toggle("pvp", true);
        } else if (slot == MOB_SPAWNING_SLOT) {
            toggle("mobSpawning", true);
        } else if (slot == ANIMAL_SPAWNING_SLOT) {
            toggle("animalSpawning", true);
        } else if (slot == WEATHER_LOCKED_SLOT) {
            toggle("weatherLocked", false);
        } else if (slot == SET_SPAWN_SLOT) {
            setSpawnHere();
        } else if (slot == BACK_SLOT) {
            WorldOptionsGui.open(player, worldName);
        }
    }

    private void toggle(String flag, boolean defaultValue) {
        boolean newValue = !WorldsConfig.getFlag(worldName, flag, defaultValue);
        WorldsConfig.setFlag(worldName, flag, newValue);
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            WorldsConfig.applyFlags(world);
        }
        render();
    }

    private void setSpawnHere() {
        if (!player.getWorld().getName().equals(worldName)) {
            WorldManager.langConfig.sendError(player, "gui.flags.must_be_in_world");
            return;
        }
        WorldsConfig.setSpawn(player.getLocation());
        WorldManager.langConfig.sendSuccess(player, "gui.flags.spawn_set");
        render();
    }
}
