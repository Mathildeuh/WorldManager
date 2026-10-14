package fr.mathildeuh.worldmanager.guis;

import org.bukkit.entity.Player;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Bukkit;
import java.util.function.BiConsumer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which {@link WMGui} (if any) each player currently has open. */
public final class GuiManager {

    private static final Map<UUID, WMGui> open = new ConcurrentHashMap<>();

    private GuiManager() { }

    public static void open(Player player, WMGui gui) {
        if (!Bukkit.isOwnedByCurrentRegion(player)) {
            SchedulerUtil.runEntity(player, () -> open(player, gui));
            return;
        }
        if (!player.isOnline()) return;
        open.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
        GuiUtils.playOpen(player);
    }

    public static void clear() {
        open.clear();
    }

    public static WMGui get(UUID playerId) {
        return open.get(playerId);
    }

    public static void remove(UUID playerId) {
        open.remove(playerId);
    }

    /** Dispatch each animation frame to its viewer, ignoring screens closed before execution. */
    public static void forEachOpen(BiConsumer<Player, WMGui> action) {
        open.forEach((id, gui) -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                SchedulerUtil.runEntity(player, () -> {
                    if (open.get(id) == gui) action.accept(player, gui);
                });
            }
        });
    }
}
