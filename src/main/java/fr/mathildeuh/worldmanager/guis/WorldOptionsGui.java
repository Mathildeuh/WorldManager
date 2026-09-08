package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.commands.subcommands.Backup;
import fr.mathildeuh.worldmanager.commands.subcommands.Delete;
import fr.mathildeuh.worldmanager.commands.subcommands.Load;
import fr.mathildeuh.worldmanager.commands.subcommands.Restore;
import fr.mathildeuh.worldmanager.commands.subcommands.Teleport;
import fr.mathildeuh.worldmanager.commands.subcommands.Unload;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Per-world action menu: game rules / teleport / unload / backup / restore / delete. */
public final class WorldOptionsGui implements WMGui {

    // Loaded-world layout (36 slots)
    private static final int GAME_RULES_SLOT = 10;
    private static final int TELEPORT_SLOT = 11;
    private static final int UNLOAD_SLOT = 12;
    private static final int BACKUP_SLOT = 13;
    private static final int RESTORE_SLOT = 14;
    private static final int DELETE_SLOT = 15;
    private static final int FLAGS_SLOT = 16;
    private static final int DEFAULT_WORLD_NOTICE_SLOT = 22;
    private static final int BACK_SLOT = 31;

    // Unloaded-world layout (27 slots)
    private static final int LOAD_SLOT = 11;
    private static final int NOT_LOADED_NOTICE_SLOT = 13;
    private static final int SMALL_BACK_SLOT = 15;

    private final Player player;
    private final String worldName;
    private final Inventory inventory;
    private final boolean worldLoaded;
    private final boolean defaultWorld;

    private WorldOptionsGui(Player player, String worldName) {
        this.player = player;
        this.worldName = worldName;
        World world = Bukkit.getWorld(worldName);
        this.worldLoaded = world != null;
        this.defaultWorld = worldLoaded && world.equals(Bukkit.getWorlds().get(0));

        Component title = GuiUtils.miniFromLang("gui.options.title", worldName);

        if (!worldLoaded) {
            this.inventory = Bukkit.createInventory(null, 27, title);
            GuiUtils.fillBorder(inventory, GuiUtils.Theme.NEUTRAL);
            inventory.setItem(LOAD_SLOT, GuiUtils.item(Material.LIME_CONCRETE, GuiUtils.miniFromLang("gui.options.load")));
            inventory.setItem(NOT_LOADED_NOTICE_SLOT, GuiUtils.item(Material.PAPER, GuiUtils.miniFromLang("gui.options.not_loaded")));
            inventory.setItem(SMALL_BACK_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.back")));
            return;
        }

        this.inventory = Bukkit.createInventory(null, 36, title);
        GuiUtils.fillBorder(inventory, GuiUtils.Theme.NEUTRAL);
        inventory.setItem(GAME_RULES_SLOT, GuiUtils.item(Material.COMMAND_BLOCK, GuiUtils.miniFromLang("gui.options.game_rules")));
        inventory.setItem(TELEPORT_SLOT, GuiUtils.item(Material.ENDER_PEARL, GuiUtils.miniFromLang("gui.options.teleport")));
        inventory.setItem(FLAGS_SLOT, GuiUtils.item(Material.REDSTONE_TORCH, GuiUtils.miniFromLang("gui.options.flags")));

        if (defaultWorld) {
            inventory.setItem(DEFAULT_WORLD_NOTICE_SLOT, GuiUtils.item(Material.PAPER, GuiUtils.miniFromLang("gui.options.default_world_notice")));
        } else {
            inventory.setItem(UNLOAD_SLOT, GuiUtils.item(Material.HOPPER, GuiUtils.miniFromLang("gui.options.unload")));
            inventory.setItem(BACKUP_SLOT, GuiUtils.item(Material.CHEST, GuiUtils.miniFromLang("gui.options.backup")));
            inventory.setItem(RESTORE_SLOT, GuiUtils.item(Material.CLOCK, GuiUtils.miniFromLang("gui.options.restore")));
            inventory.setItem(DELETE_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.miniFromLang("gui.options.delete")));
        }

        inventory.setItem(BACK_SLOT, GuiUtils.item(Material.OAK_DOOR, GuiUtils.miniFromLang("gui.back")));
    }

    public static void open(Player player, String worldName) {
        GuiManager.open(player, new WorldOptionsGui(player, worldName));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!worldLoaded) {
            onUnloadedClick(event.getSlot());
            return;
        }
        onLoadedClick(event.getSlot());
    }

    private void onUnloadedClick(int slot) {
        if (slot == LOAD_SLOT) {
            new Load(player).execute(worldName, "normal", null);
            WorldOptionsGui.open(player, worldName);
        } else if (slot == SMALL_BACK_SLOT) {
            EditorListGui.open(player, 0);
        }
    }

    private void onLoadedClick(int slot) {
        if (slot == GAME_RULES_SLOT) {
            GameRuleListGui.open(player, worldName, 0);
        } else if (slot == FLAGS_SLOT) {
            WorldFlagsGui.open(player, worldName);
        } else if (slot == TELEPORT_SLOT) {
            new Teleport(player).execute("teleport", worldName);
        } else if (slot == BACK_SLOT) {
            EditorListGui.open(player, 0);
        } else if (!defaultWorld && slot == UNLOAD_SLOT) {
            confirmUnload();
        } else if (!defaultWorld && slot == BACKUP_SLOT) {
            new Backup(player).execute(worldName);
        } else if (!defaultWorld && slot == RESTORE_SLOT) {
            confirmRestore();
        } else if (!defaultWorld && slot == DELETE_SLOT) {
            confirmDelete();
        }
    }

    private void confirmUnload() {
        ConfirmGui.open(player,
                GuiUtils.miniFromLang("gui.options.title", worldName),
                GuiUtils.miniFromLang("gui.options.confirm_unload", worldName),
                () -> {
                    new Unload(player).execute(worldName);
                    EditorListGui.open(player, 0);
                },
                () -> WorldOptionsGui.open(player, worldName));
    }

    private void confirmDelete() {
        ConfirmGui.open(player,
                GuiUtils.miniFromLang("gui.options.title", worldName),
                GuiUtils.miniFromLang("gui.options.confirm_delete", worldName),
                () -> {
                    new Delete(player).execute(worldName);
                    EditorListGui.open(player, 0);
                },
                () -> WorldOptionsGui.open(player, worldName));
    }

    private void confirmRestore() {
        ConfirmGui.open(player,
                GuiUtils.miniFromLang("gui.options.title", worldName),
                GuiUtils.miniFromLang("gui.options.confirm_restore", worldName),
                () -> {
                    new Restore(player).execute(worldName);
                    WorldOptionsGui.open(player, worldName);
                },
                () -> WorldOptionsGui.open(player, worldName));
    }
}
