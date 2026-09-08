package fr.mathildeuh.worldmanager.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional Vault economy hook backing custom portal prices (see
 * {@code configs/CustomPortalsManager}, {@code events/CustomPortalListener}). Vault is a soft
 * dependency: absent Vault or a registered economy plugin, {@link #isEnabled()} is {@code false}
 * and every portal is effectively free, regardless of its configured price - paid portals are a
 * bonus on top of Vault, never a hard requirement to run the plugin.
 */
public final class EconomyHook {

    private static Economy economy;

    private EconomyHook() { }

    /** Looks up a registered Vault economy provider. Safe to call even if Vault isn't installed. */
    public static boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        return economy != null;
    }

    public static boolean isEnabled() {
        return economy != null;
    }

    public static boolean has(OfflinePlayer player, double amount) {
        return economy == null || economy.has(player, amount);
    }

    /** Withdraws {@code amount} from {@code player}. Returns whether it succeeded (always {@code true} if Vault is absent). */
    public static boolean withdraw(OfflinePlayer player, double amount) {
        return economy == null || economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        return economy != null ? economy.format(amount) : String.valueOf(amount);
    }
}
