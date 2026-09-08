package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Drives the "chasing sparkle" border effect on every open {@link WMGui} - a single global
 * heartbeat (not one task per screen) that, every few ticks, re-paints whichever border slots
 * (tagged by {@link GuiUtils#fillBorder}) fall on a shifting diagonal pattern with that screen's
 * accent color, and every other border slot with the neutral base color.
 */
public final class GuiAnimator {

    private static final long PERIOD_TICKS = 6L;
    private static final int CYCLE_LENGTH = 9;

    private static ScheduledTask task;
    private static int frame = 0;

    private GuiAnimator() { }

    public static void start() {
        if (task != null) {
            return;
        }
        task = SchedulerUtil.runGlobalTimer(GuiAnimator::tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static void tick() {
        frame++;
        for (WMGui gui : GuiManager.openGuis()) {
            animate(gui.getInventory());
        }
    }

    private static void animate(Inventory inventory) {
        Material accent = null;
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = inventory.getItem(slot);
            String accentName = borderAccentOf(item);
            if (accentName == null) {
                continue;
            }
            if (accent == null) {
                try {
                    accent = Material.valueOf(accentName);
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }
            Material target = (slot + frame) % CYCLE_LENGTH == 0 ? accent : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            if (item.getType() != target) {
                ItemStack repainted = item.clone();
                repainted.setType(target);
                inventory.setItem(slot, repainted);
            }
        }
    }

    private static String borderAccentOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(GuiUtils.BORDER_ACCENT_KEY, PersistentDataType.STRING);
    }
}
