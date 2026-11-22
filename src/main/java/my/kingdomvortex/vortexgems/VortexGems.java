
package my.kingdomvortex.vortexgems;

import my.kingdomvortex.vortexgems.command.GemsAdminCommand;
import my.kingdomvortex.vortexgems.command.GemsCommand;
import my.kingdomvortex.vortexgems.config.ConfigManager;
import my.kingdomvortex.vortexgems.database.DatabaseManager;
import my.kingdomvortex.vortexgems.gui.MenuManager;
import my.kingdomvortex.vortexgems.listener.MenuListener;
import my.kingdomvortex.vortexgems.listener.PlayerListener;
import my.kingdomvortex.vortexgems.manager.CategoryManager;
import my.kingdomvortex.vortexgems.manager.EconomyManager;
import my.kingdomvortex.vortexgems.manager.GemsManager;
import my.kingdomvortex.vortexgems.manager.LeaderboardManager;
import my.kingdomvortex.vortexgems.manager.RewardManager;
import my.kingdomvortex.vortexgems.manager.TransactionManager;
import my.kingdomvortex.vortexgems.placeholder.GemsExpansion;
import my.kingdomvortex.vortexgems.util.InputManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VortexGems extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private GemsManager gemsManager;
    private CategoryManager categoryManager;
    private RewardManager rewardManager;
    private TransactionManager transactionManager;
    private LeaderboardManager leaderboardManager;
    private MenuManager menuManager;
    private InputManager inputManager;

    @Override
    public void onEnable() {
        getLogger().info("Starting VortexGems...");

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        saveResourceIfMissing("categories.yml");
        saveResourceIfMissing("rewards.yml");
        saveResourceIfMissing("messages.yml");

        configManager = new ConfigManager(this);
        configManager.load();

        databaseManager = new DatabaseManager(this);

        if (!databaseManager.connect()) {
            getLogger().severe("VortexGems could not connect to SQLite.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economyManager = new EconomyManager(this);
        economyManager.setup();

        gemsManager = new GemsManager(this);
        categoryManager = new CategoryManager(this);
        rewardManager = new RewardManager(this);
        transactionManager = new TransactionManager(this);
        leaderboardManager = new LeaderboardManager(this);
        menuManager = new MenuManager(this);
        inputManager = new InputManager(this);

        registerCommands();
        registerListeners();
        registerPlaceholder();

        getLogger().info("VortexGems enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VortexGems disabled.");
    }

    private void saveResourceIfMissing(String fileName) {
        java.io.File file = new java.io.File(getDataFolder(), fileName);

        if (!file.exists()) {
            saveResource(fileName, false);
        }
    }

    private void registerCommands() {
        PluginCommand gemsCommand = getCommand("gems");

        if (gemsCommand != null) {
            GemsCommand executor = new GemsCommand(this);
            gemsCommand.setExecutor(executor);
            gemsCommand.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'gems' is missing from plugin.yml.");
        }

        PluginCommand adminCommand = getCommand("gemsadmin");

        if (adminCommand != null) {
            GemsAdminCommand executor = new GemsAdminCommand(this);
            adminCommand.setExecutor(executor);
            adminCommand.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'gemsadmin' is missing from plugin.yml.");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new MenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this),
                this
        );
    }

    private void registerPlaceholder() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found. Expansion skipped.");
            return;
        }

        if (new GemsExpansion(this).register()) {
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().warning("PlaceholderAPI expansion registration failed.");
        }
    }

    public void reloadPlugin() {
        reloadConfig();

        if (configManager != null) {
            configManager.load();
        }

        if (categoryManager != null) {
            categoryManager.reload();
        }

        if (rewardManager != null) {
            rewardManager.reload();
        }

        getLogger().info("VortexGems configuration reloaded.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GemsManager getGemsManager() {
        return gemsManager;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }
}
