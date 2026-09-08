package fr.mathildeuh.worldmanager.guis;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which {@link WMGui} (if any) each player currently has open. */
public final class GuiManager {

    private static final Map<UUID, WMGui> open = new ConcurrentHashMap<>();

    private GuiManager() { }

    public static void open(Player player, WMGui gui) {
        open.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
        GuiUtils.playOpen(player);
    }

    public static WMGui get(UUID playerId) {
        return open.get(playerId);
    }

    public static void remove(UUID playerId) {
        open.remove(playerId);
    }

    /** Snapshot of every currently-open screen, for {@link GuiAnimator} to redraw each frame. */
    public static Collection<WMGui> openGuis() {
        return open.values();
    }
}
