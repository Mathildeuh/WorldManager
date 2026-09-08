package fr.mathildeuh.worldmanager.guis;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for building chest-GUI screens: lang-file-backed text (mirroring
 * {@link MessageUtils}'s role for chat messages), item construction, themed/animated border
 * filling, sound feedback and pagination math, so every screen looks and behaves consistently.
 */
public final class GuiUtils {

    /** Marks a pane as an animated border slot; value is the accent {@link Material} name for {@link GuiAnimator}. */
    static final NamespacedKey BORDER_ACCENT_KEY = new NamespacedKey(WorldManager.getInstance(), "gui_border_accent");

    private GuiUtils() { }

    /**
     * A screen's color language: a neutral base pane plus an accent that "chases" around the
     * border. Deliberately avoids red/lime - those are reserved for actual cancel/confirm
     * buttons (RED_CONCRETE / LIME_CONCRETE), and an accent sharing that hue made the border
     * animation compete with the real action buttons for attention instead of framing them.
     */
    public enum Theme {
        /** Navigation / neutral screens (main menu, lists, options). */
        NEUTRAL(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE),
        /** Creation / positive-action screens. */
        CREATE(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE),
        /** Destructive-action confirmation screens. */
        DESTRUCTIVE(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE),
        /** Info/settings screens (game rules, flags). */
        INFO(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE);

        final Material base;
        final Material accent;

        Theme(Material base, Material accent) {
            this.base = base;
            this.accent = accent;
        }
    }

    /** Splits camelCase into readable "Title Case" text, e.g. "doFireTick" -> "Do Fire Tick". */
    public static String readableName(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static Component miniFromLang(String path, Object... args) {
        String mini = WorldManager.langConfig.formatMessage(path, args);
        if (mini == null) {
            Bukkit.getLogger().warning("[WorldManager] Missing GUI message key: " + path);
            return Component.text(path);
        }
        return MessageUtils.parseMini(mini);
    }

    public static Component mini(String rawMiniMessage) {
        return MessageUtils.parseMini(rawMiniMessage);
    }

    /** Un-italicizes a component - Minecraft defaults item names/lore to italic otherwise. */
    private static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack borderPane(Material material, Material accent) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(Component.text(" ")));
        meta.getPersistentDataContainer().set(BORDER_ACCENT_KEY, PersistentDataType.STRING, accent.name());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack item(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        if (lore.length > 0) {
            List<Component> loreLines = new ArrayList<>();
            for (Component line : lore) {
                loreLines.add(plain(line));
            }
            meta.lore(loreLines);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack item(Material material, String nameMini, String... loreMini) {
        Component[] lore = new Component[loreMini.length];
        for (int i = 0; i < loreMini.length; i++) {
            lore[i] = mini(loreMini[i]);
        }
        return item(material, mini(nameMini), lore);
    }

    /**
     * Gives an item a subtle enchantment-glint shimmer with no visible enchantment line, for the
     * one or two "primary" buttons on a screen (confirm/create/etc.) - a cheap, well-established
     * way to make the most important action visually pop in a plain chest inventory.
     */
    public static ItemStack glinted(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /** Fills every slot with this theme's base pane, tagged for {@link GuiAnimator} to chase the accent color through. */
    public static void fillBorder(Inventory inventory, Theme theme) {
        ItemStack pane = borderPane(theme.base, theme.accent);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane);
        }
    }

    public static void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    public static void playOpen(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.3f);
    }

    public static void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.1f);
    }

    public static void playError(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 0.7f);
    }

    /** Pagination math shared by every paginated list screen. */
    public record Page(int index, int count, int fromInclusive, int toExclusive) {
    }

    public static Page paginate(int requestedPage, int totalItems, int pageSize) {
        int pageCount = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        int clamped = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int from = clamped * pageSize;
        int to = Math.min(from + pageSize, totalItems);
        return new Page(clamped, pageCount, from, to);
    }
}
