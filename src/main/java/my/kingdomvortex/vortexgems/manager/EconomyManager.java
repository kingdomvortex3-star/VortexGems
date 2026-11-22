
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyManager {

    private final VortexGems plugin;
    private Economy economy;

    public EconomyManager(VortexGems plugin) {
        this.plugin = plugin;
        setup();
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault was not found.");
            economy = null;
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            plugin.getLogger().warning("No economy provider was found.");
            economy = null;
            return false;
        }

        economy = provider.getProvider();
        plugin.getLogger().info("Vault economy connected successfully.");
        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public Economy getProvider() {
        return economy;
    }

    public double getPricePerGem() {
        return plugin.getConfig().getDouble(
                "settings.economy.price-per-gem", 1000.0
        );
    }

    public boolean isBuyingEnabled() {
        return plugin.getConfig().getBoolean(
                "settings.economy.buying-enabled", true
        );
    }

    public double calculatePrice(long amount) {
        if (amount <= 0) return -1;

        double price = getPricePerGem() * amount;

        if (!Double.isFinite(price) || price <= 0) {
            return -1;
        }

        return price;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) return 0;
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return isAvailable()
                && Double.isFinite(amount)
                && amount >= 0
                && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || !Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        if (!economy.has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || !Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean buyGems(OfflinePlayer player, long amount) {
        if (player == null || !player.isOnline()) return false;
        if (!isAvailable() || !isBuyingEnabled()) return false;

        long minimum = plugin.getConfig().getLong(
                "settings.economy.minimum-purchase", 1
        );

        long maximum = plugin.getConfig().getLong(
                "settings.economy.maximum-purchase", 100000
        );

        if (amount < minimum || amount > maximum) {
            return false;
        }

        double price = calculatePrice(amount);

        if (price <= 0 || !economy.has(player, price)) {
            return false;
        }

        if (!withdraw(player, price)) {
            return false;
        }

        String playerName = player.getName() == null
                ? player.getUniqueId().toString()
                : player.getName();

        boolean credited = plugin.getGemsManager().addBalance(
                player.getUniqueId(),
                playerName,
                amount
        );

        if (!credited) {
            boolean refunded = deposit(player, price);

            if (!refunded) {
                plugin.getLogger().severe(
                        "CRITICAL: Failed to refund purchase for "
                                + player.getUniqueId()
                );
            }

            return false;
        }

        plugin.getTransactionManager().record(
                player.getUniqueId(),
                "BUY",
                amount,
                "Purchased " + amount + " Gems"
        );

        return true;
    }
}
