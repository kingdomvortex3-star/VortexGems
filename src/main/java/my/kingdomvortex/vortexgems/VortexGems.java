
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
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VortexGems extends JavaPlugin {

    private static VortexGems instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private GemsManager gemsManager;
    private CategoryManager categoryManager;
    private RewardManager rewardManager;
    private TransactionManager transactionManager;
    private LeaderboardManager leaderboardManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResourceIfMissing("categories.yml");
        saveResourceIfMissing("rewards.yml");
        saveResourceIfMissing("messages.yml");

        configManager = new ConfigManager(this);
        configManager.load();

        databaseManager = new DatabaseManager(this);

        if (!databaseManager.connect()) {
            getLogger().severe("Database initialization failed!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        economyManager = new EconomyManager(this);
        gemsManager = new GemsManager(this);
        categoryManager = new CategoryManager(this);
        rewardManager = new RewardManager(this);
        transactionManager = new TransactionManager(this);
        leaderboardManager = new LeaderboardManager(this);
        menuManager = new MenuManager(this);

        registerCommands();
        registerListeners();
        registerPlaceholder();

        getLogger().info("VortexGems v" + getDescription().getVersion() + " enabled!");
    }

    private void saveResourceIfMissing(String resource) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        java.io.File file = new java.io.File(getDataFolder(), resource);

        if (!file.exists()) {
            saveResource(resource, false);
        }
    }

    private void registerCommands() {
        GemsCommand gemsCommand = new GemsCommand(this);
        GemsAdminCommand adminCommand = new GemsAdminCommand(this);

        getCommand("gems").setExecutor(gemsCommand);
        getCommand("gems").setTabCompleter(gemsCommand);

        getCommand("gemsadmin").setExecutor(adminCommand);
        getCommand("gemsadmin").setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerPlaceholder() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GemsExpansion(this).register();
            getLogger().info("PlaceholderAPI hooked!");
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.load();
        categoryManager.reload();
        rewardManager.reload();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VortexGems disabled.");
        instance = null;
    }

    public static VortexGems getInstance() {
        return instance;
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
}
