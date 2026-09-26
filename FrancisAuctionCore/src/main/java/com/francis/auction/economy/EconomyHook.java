package com.francis.auction.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Thin wrapper around the Vault economy service.
 *
 * <p>The plugin stays loadable without Vault; every call simply reports that
 * no economy is available, and the command layer tells the player so.
 */
public final class EconomyHook {

    private Economy economy;

    /** Looks the provider up in the services manager. */
    public boolean hook(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return false;
        }
        RegisteredServiceProvider<Economy> provider =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        return economy != null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        return economy == null ? 0D : economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    /** @return {@code true} when the full amount was taken. */
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** @return {@code true} when the full amount was paid out. */
    public boolean deposit(OfflinePlayer player, double amount) {
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }
}
