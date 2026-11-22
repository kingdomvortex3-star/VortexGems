
package my.kingdomvortex.vortexgems;

import net.milkbowl.vault.economy.Economy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class VortexGems extends JavaPlugin implements Listener, CommandExecutor {

    private Economy economy;
    private Connection database;

    private File categoriesFile;
    private File rewardsFile;

    private YamlConfiguration categories;
    private YamlConfiguration rewards;

    private final Map<UUID, Long> transactions = new HashMap<>();

    private static final String PREFIX = "&8[&bVortexGems&8] &7";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("categories.yml");
        saveResourceIfMissing("rewards.yml");

        categoriesFile = new File(getDataFolder(), "categories.yml");
        rewardsFile = new File(getDataFolder(), "rewards.yml");

        loadShopFiles();

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy tidak ditemukan! Plugin dinonaktifkan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupDatabase()) {
            getLogger().severe("Database gagal dibuka!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("gems")).setExecutor(this);
        Objects.requireNonNull(getCommand("gemsadmin")).setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GemsExpansion(this).register();
            getLogger().info("PlaceholderAPI berhasil diaktifkan!");
        }

        getLogger().info("VortexGems v1.0.0 enabled!");
    }

    @Override
    public void onDisable() {
        try {
            if (database != null && !database.isClosed()) {
                database.close();
            }
        } catch (SQLException exception) {
            getLogger().severe("Database close error: " + exception.getMessage());
        }

        getLogger().info("VortexGems disabled.");
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (provider == null) return false;

        economy = provider.getProvider();
        return economy != null;
    }

    private boolean setupDatabase() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");

            File file = new File(getDataFolder(), "data.db");

            database = DriverManager.getConnection(
                    "jdbc:sqlite:" + file.getAbsolutePath()
            );

            try (Statement statement = database.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gems (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0
                    )
                """);

                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }

            return true;

        } catch (Exception exception) {
            getLogger().severe("SQLite error: " + exception.getMessage());
            return false;
        }
    }

    private synchronized void loadShopFiles() {
        categories = YamlConfiguration.loadConfiguration(categoriesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    private synchronized void saveCategories() {
        try {
            categories.save(categoriesFile);
        } catch (Exception exception) {
            getLogger().severe("Categories save error: " + exception.getMessage());
        }
    }

    private synchronized void saveRewards() {
        try {
            rewards.save(rewardsFile);
        } catch (Exception exception) {
            getLogger().severe("Rewards save error: " + exception.getMessage());
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String message(String key) {
        String value = getConfig().getString("messages." + key);

        if (value == null) {
            YamlConfiguration messages = YamlConfiguration.loadConfiguration(
                    new File(getDataFolder(), "messages.yml")
            );
            value = messages.getString(key, key);
        }

        return color(PREFIX + value);
    }

    private void send(Player player, String key) {
        player.sendMessage(message(key));
    }

    private synchronized long getBalance(UUID uuid) {
        try (PreparedStatement statement = database.prepareStatement(
                "SELECT balance FROM gems WHERE uuid = ?"
        )) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong("balance");
            }

        } catch (SQLException exception) {
            getLogger().severe(exception.getMessage());
        }

        return 0;
    }

    private synchronized boolean setBalance(
            UUID uuid, String name, long amount
    ) {
        if (amount < 0 || amount > getConfig().getLong("settings.max-balance")) {
            return false;
        }

        try (PreparedStatement statement = database.prepareStatement("""
                INSERT INTO gems(uuid, username, balance)
                VALUES(?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    username = excluded.username,
                    balance = excluded.balance
                """)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3, amount);

            statement.executeUpdate();
            return true;

        } catch (SQLException exception) {
            getLogger().severe(exception.getMessage());
            return false;
        }
    }

    private synchronized boolean addBalance(
            UUID uuid, String name, long amount, String type
    ) {
        if (amount <= 0) return false;

        long current = getBalance(uuid);

        if (current > getConfig().getLong("settings.max-balance") - amount) {
            return false;
        }

        if (!setBalance(uuid, name, current + amount)) return false;

        logTransaction(uuid, type, amount, type);
        return true;
    }

    private synchronized boolean removeBalance(
            UUID uuid, String name, long amount, String type
    ) {
        if (amount <= 0) return false;

        long current = getBalance(uuid);

        if (current < amount) return false;

        if (!setBalance(uuid, name, current - amount)) return false;

        logTransaction(uuid, type, amount, type);
        return true;
    }

    private void logTransaction(
            UUID uuid, String type, long amount, String description
    ) {
        if (!getConfig().getBoolean("security.log-transactions", true)) return;

        try (PreparedStatement statement = database.prepareStatement("""
                INSERT INTO transactions(uuid, type, amount, description)
                VALUES(?, ?, ?, ?)
                """)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, type);
            statement.setLong(3, amount);
            statement.setString(4, description);
            statement.executeUpdate();

        } catch (SQLException exception) {
            getLogger().warning("Transaction log error: " + exception.getMessage());
        }
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            if (lore != null) {
                meta.setLore(lore.stream().map(this::color).toList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack filler() {
        return createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                "&8 ",
                Collections.emptyList()
        );
    }

    private Inventory createMenu(String title, int size) {
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler());
        }

        return inventory;
    }

    public void openMain(Player player) {
        Inventory inventory = createMenu("&8✦ &bVortexGems", 27);

        inventory.setItem(10, createItem(
                Material.EMERALD,
                "&aBuy Gems",
                List.of(
                        "&7Purchase Gems using Money.",
                        "",
                        "&eClick to open!"
                )
        ));

        inventory.setItem(12, createItem(
                Material.CHEST,
                "&6Exchange Shop",
                List.of(
                        "&7Exchange your Gems for rewards.",
                        "",
                        "&eClick to open!"
                )
        ));

        inventory.setItem(14, createItem(
                Material.PLAYER_HEAD,
                "&bMy Balance",
                List.of(
                        "&7Your Gems: &f" + getBalance(player.getUniqueId()),
                        "",
                        "&eClick to view!"
                )
        ));

        inventory.setItem(16, createItem(
                Material.GOLD_BLOCK,
                "&6Leaderboard",
                List.of(
                        "&7View the richest players.",
                        "",
                        "&eClick to open!"
                )
        ));

        player.openInventory(inventory);
    }

    private void openBuy(Player player) {
        Inventory inventory = createMenu("&8Buy Gems", 27);

        List<Integer> options = getConfig().getIntegerList(
                "economy.purchase-options"
        );

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        long price = getConfig().getLong("economy.price-per-gem");

        for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
            int amount = options.get(i);

            if (amount <= 0) continue;

            long total = price * amount;

            inventory.setItem(slots[i], createItem(
                    Material.EMERALD,
                    "&a" + amount + " Gems",
                    List.of(
                            "&7Price: &e$" + total,
                            "",
                            "&eClick to purchase!"
                    )
            ));
        }

        inventory.setItem(22, createItem(
                Material.PAPER,
                "&bCustom Purchase",
                List.of(
                        "&7Use command:",
                        "&f/gems buy <amount>"
                )
        ));

        player.openInventory(inventory);
    }

    private void openCategories(Player player) {
        if (!getConfig().getBoolean("shop.enabled", true)) {
            player.sendMessage(color("&cExchange Shop is disabled."));
            return;
        }

        Inventory inventory = createMenu("&8Exchange Categories", 27);

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "categories." + id;

                if (!categories.getBoolean(path + ".enabled", true)) continue;

                Material material = Material.matchMaterial(
                        categories.getString(path + ".material", "CHEST")
                );

                if (material == null) material = Material.CHEST;

                int slot = categories.getInt(path + ".slot", 11);

                if (slot < 0 || slot >= 27) continue;

                List<String> lore = categories.getStringList(path + ".lore");

                inventory.setItem(slot, createItem(
                        material,
                        categories.getString(path + ".name", id),
                        lore
                ));
            }
        }

        player.openInventory(inventory);
    }

    private void openRewards(Player player, String category) {
        Inventory inventory = createMenu("&8Exchange: " + category, 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(category)) continue;

                if (!rewards.getBoolean(path + ".enabled", true)) continue;

                Material material = Material.matchMaterial(
                        rewards.getString(path + ".material", "STONE")
                );

                if (material == null) material = Material.STONE;

                int slot = rewards.getInt(path + ".slot", 10);

                if (slot < 0 || slot >= 54) continue;

                List<String> lore = new ArrayList<>(
                        rewards.getStringList(path + ".lore")
                );

                lore.add("");
                lore.add("&7Price: &b" + rewards.getLong(path + ".price") + " Gems");
                lore.add("&eClick to exchange!");

                ItemStack item = createItem(
                        material,
                        rewards.getString(path + ".name", id),
                        lore
                );

                item.setAmount(Math.max(1, Math.min(
                        64, rewards.getInt(path + ".amount", 1)
                )));

                inventory.setItem(slot, item);
            }
        }

        player.openInventory(inventory);
    }

    private void openAdmin(Player player) {
        if (!isAdmin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return;
        }

        Inventory inventory = createMenu("&8VortexGems Admin", 27);

        inventory.setItem(10, createItem(
                Material.CHEST,
                "&bManage Categories",
                List.of(
                        "&7Enable or disable categories.",
                        "",
                        "&eClick to manage!"
                )
        ));

        inventory.setItem(13, createItem(
                Material.EMERALD,
                "&aManage Rewards",
                List.of(
                        "&7Enable or disable rewards.",
                        "",
                        "&eClick to manage!"
                )
        ));

        inventory.setItem(16, createItem(
                Material.REDSTONE,
                "&cReload Configuration",
                List.of(
                        "&7Reload all YAML files.",
                        "",
                        "&eClick to reload!"
                )
        ));

        player.openInventory(inventory);
    }

    private void openCategoryEditor(Player player) {
        Inventory inventory = createMenu("&8Category Editor", 54);

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "categories." + id;

                Material material = Material.matchMaterial(
                        categories.getString(path + ".material", "CHEST")
                );

                if (material == null) material = Material.CHEST;

                boolean enabled = categories.getBoolean(path + ".enabled", true);

                inventory.setItem(slot++, createItem(
                        material,
                        categories.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to toggle!"
                        )
                ));
            }
        }

        player.openInventory(inventory);
    }

    private void openRewardEditor(Player player) {
        Inventory inventory = createMenu("&8Reward Editor", 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "rewards." + id;

                Material material = Material.matchMaterial(
                        rewards.getString(path + ".material", "STONE")
                );

                if (material == null) material = Material.STONE;

                boolean enabled = rewards.getBoolean(path + ".enabled", true);

                inventory.setItem(slot++, createItem(
                        material,
                        rewards.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Category: &f" + rewards.getString(path + ".category"),
                                "&7Price: &b" + rewards.getLong(path + ".price") + " Gems",
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to toggle!"
                        )
                ));
            }
        }

        player.openInventory(inventory);
    }

    private void exchange(Player player, String id) {
        String path = "rewards." + id;

        if (!rewards.getBoolean(path + ".enabled", false)) return;

        long price = rewards.getLong(path + ".price");
        if (price <= 0) return;

        long balance = getBalance(player.getUniqueId());

        if (balance < price) {
            send(player, "not-enough-gems");
            return;
        }

        String type = rewards.getString(path + ".reward-type", "COMMAND");

        List<String> commands = rewards.getStringList(path + ".commands");

        Material material = Material.matchMaterial(
                rewards.getString(path + ".material", "STONE")
        );

        int amount = rewards.getInt(path + ".amount", 1);

        if (type.equalsIgnoreCase("COMMAND") && commands.isEmpty()) {
            player.sendMessage(color("&cReward command is empty."));
            return;
        }

        if (type.equalsIgnoreCase("ITEM")
                && (material == null || material.isAir()
                || amount < 1 || amount > 64)) {
            player.sendMessage(color("&cInvalid reward item."));
            return;
        }

        if (type.equalsIgnoreCase("ITEM")
                && !canFit(player, material, amount)) {
            player.sendMessage(color("&cYour inventory is full!"));
            return;
        }

        if (!removeBalance(
                player.getUniqueId(),
                player.getName(),
                price,
                "EXCHANGE"
        )) {
            send(player, "not-enough-gems");
            return;
        }

        try {
            if (type.equalsIgnoreCase("COMMAND")) {
                for (String command : commands) {
                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            command.replace("%player%", player.getName())
                    );
                }
            } else if (type.equalsIgnoreCase("ITEM")) {
                ItemStack item = new ItemStack(material, amount);
                player.getInventory().addItem(item);
            } else {
                addBalance(
                        player.getUniqueId(),
                        player.getName(),
                        price,
                        "REFUND"
                );
                player.sendMessage(color("&cUnknown reward type."));
                return;
            }

            player.sendMessage(color(
                    PREFIX + "&aExchange successful: &e"
                            + rewards.getString(path + ".name", id)
            ));

            logTransaction(
                    player.getUniqueId(),
                    "REWARD",
                    price,
                    id
            );

            if (getConfig().getBoolean("shop.close-after-purchase", false)) {
                player.closeInventory();
            }

        } catch (Exception exception) {
            addBalance(
                    player.getUniqueId(),
                    player.getName(),
                    price,
                    "REFUND"
            );

            getLogger().severe("Reward error: " + exception.getMessage());

            player.sendMessage(color("&cReward failed. Gems refunded."));
        }
    }

    private boolean canFit(Player player, Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        return player.getInventory().firstEmpty() != -1
                || player.getInventory().containsAtLeast(item, 1);
    }

    private void buy(Player player, int amount) {
        if (!getConfig().getBoolean("economy.buy-enabled", true)) {
            player.sendMessage(color("&cGems purchase is disabled."));
            return;
        }

        int min = getConfig().getInt("economy.min-purchase", 1);
        int max = getConfig().getInt("economy.max-purchase", 10000);

        if (amount < min || amount > max) {
            send(player, "invalid-amount");
            return;
        }

        long pricePerGem = getConfig().getLong("economy.price-per-gem");

        if (pricePerGem <= 0 || amount > Long.MAX_VALUE / pricePerGem) {
            send(player, "invalid-amount");
            return;
        }

        long total = pricePerGem * amount;

        if (!economy.has(player, total)) {
            send(player, "not-enough-money");
            return;
        }

        long now = System.currentTimeMillis();

        if (transactions.getOrDefault(player.getUniqueId(), 0L) > now - 1000) {
            return;
        }

        transactions.put(player.getUniqueId(), now);

        if (!economy.withdrawPlayer(player, total).transactionSuccess()) {
            send(player, "not-enough-money");
            return;
        }

        if (!addBalance(
                player.getUniqueId(),
                player.getName(),
                amount,
                "PURCHASE"
        )) {
            economy.depositPlayer(player, total);
            player.sendMessage(color("&cTransaction failed. Money refunded."));
            return;
        }

        player.sendMessage(color(
                PREFIX + "&aYou purchased &b" + amount + " Gems &afor &e$" + total
        ));
    }

    private boolean isAdmin(Player player, String permission) {
        return (getConfig().getBoolean("admin.op-access", true)
                && player.isOp()) || player.hasPermission(permission);
    }

    private void toggleCategory(String id) {
        String path = "categories." + id;

        if (!categories.contains(path)) return;

        boolean enabled = categories.getBoolean(path + ".enabled", true);
        categories.set(path + ".enabled", !enabled);
        saveCategories();
    }

    private void toggleReward(String id) {
        String path = "rewards." + id;

        if (!rewards.contains(path)) return;

        boolean enabled = rewards.getBoolean(path + ".enabled", true);
        rewards.set(path + ".enabled", !enabled);
        saveRewards();
    }

    private void createCategory(
            String id, String name, Material material
    ) {
        String path = "categories." + id;

        categories.set(path + ".name", name);
        categories.set(path + ".material", material.name());
        categories.set(path + ".slot", 11);
        categories.set(path + ".enabled", true);
        categories.set(path + ".lore", List.of("&7Custom category"));

        saveCategories();
    }

    private void deleteCategory(String id) {
        categories.set("categories." + id, null);
        saveCategories();
    }

    private void addHandReward(
            Player player,
            String id,
            String category,
            long price,
            int amount
    ) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir() || amount < 1 || amount > 64 || price <= 0) {
            player.sendMessage(color("&cInvalid item, amount or price."));
            return;
        }

        if (!categories.contains("categories." + category)) {
            player.sendMessage(color("&cCategory does not exist."));
            return;
        }

        String path = "rewards." + id;

        rewards.set(path + ".category", category);
        rewards.set(path + ".name",
                hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()
                        ? ChatColor.stripColor(hand.getItemMeta().getDisplayName())
                        : id
        );
        rewards.set(path + ".material", hand.getType().name());
        rewards.set(path + ".slot", 11);
        rewards.set(path + ".amount", amount);
        rewards.set(path + ".price", price);
        rewards.set(path + ".enabled", true);
        rewards.set(path + ".reward-type", "ITEM");
        rewards.set(path + ".lore", List.of("&7Custom reward"));

        saveRewards();

        player.sendMessage(color(PREFIX + "&aReward created!"));
    }

    private void showTop(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&l       GEMS LEADERBOARD"));
        player.sendMessage(color("&8&m------------------------------"));

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username, balance FROM gems ORDER BY balance DESC LIMIT ?"
        )) {
            statement.setInt(1,
                    getConfig().getInt("leaderboard.max-entries", 10));

            try (ResultSet result = statement.executeQuery()) {
                int rank = 1;

                while (result.next()) {
                    player.sendMessage(color(
                            "&e#" + rank + " &f" + result.getString("username")
                                    + " &8- &b" + result.getLong("balance") + " Gems"
                    ));
                    rank++;
                }
            }

        } catch (SQLException exception) {
            player.sendMessage(color("&cLeaderboard error."));
        }

        player.sendMessage(color("&8&m------------------------------"));
    }

    private void adminHelp(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&lVortexGems Admin"));
        player.sendMessage(color("&f/gemsadmin"));
        player.sendMessage(color("&f/gemsadmin category create <id> <name> <material>"));
        player.sendMessage(color("&f/gemsadmin category delete <id>"));
        player.sendMessage(color("&f/gemsadmin category toggle <id>"));
        player.sendMessage(color("&f/gemsadmin reward addhand <id> <category> <price> <amount>"));
        player.sendMessage(color("&f/gemsadmin reward delete <id>"));
        player.sendMessage(color("&f/gemsadmin reward price <id> <price>"));
        player.sendMessage(color("&f/gemsadmin reward toggle <id>"));
        player.sendMessage(color("&8&m------------------------------"));
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (command.getName().equalsIgnoreCase("gemsadmin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Player only.");
                return true;
            }

            if (!isAdmin(player, "vortexgems.editor")) {
                send(player, "no-permission");
                return true;
            }

            if (args.length == 0) {
                openAdmin(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("category")) {
                if (args.length >= 2) {
                    String action = args[1].toLowerCase();

                    if (action.equals("create") && args.length >= 5) {
                        Material material = Material.matchMaterial(args[4]);

                        if (material == null) {
                            player.sendMessage(color("&cInvalid material."));
                            return true;
                        }

                        createCategory(args[2], args[3], material);
                        player.sendMessage(color(PREFIX + "&aCategory created!"));

                    } else if (action.equals("delete") && args.length >= 3) {
                        deleteCategory(args[2]);
                        player.sendMessage(color(PREFIX + "&aCategory deleted!"));

                    } else if (action.equals("toggle") && args.length >= 3) {
                        toggleCategory(args[2]);
                        player.sendMessage(color(PREFIX + "&aCategory updated!"));
                    }
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("reward")) {
                if (args.length >= 2) {
                    String action = args[1].toLowerCase();

                    if (action.equals("addhand") && args.length >= 6) {
                        try {
                            addHandReward(
                                    player,
                                    args[2],
                                    args[3],
                                    Long.parseLong(args[4]),
                                    Integer.parseInt(args[5])
                            );
                        } catch (NumberFormatException exception) {
                            send(player, "invalid-amount");
                        }

                    } else if (action.equals("delete") && args.length >= 3) {
                        rewards.set("rewards." + args[2], null);
                        saveRewards();
                        player.sendMessage(color(PREFIX + "&aReward deleted!"));

                    } else if (action.equals("toggle") && args.length >= 3) {
                        toggleReward(args[2]);
                        player.sendMessage(color(PREFIX + "&aReward updated!"));

                    } else if (action.equals("price") && args.length >= 4) {
                        try {
                            long price = Long.parseLong(args[3]);

                            if (price <= 0 || !rewards.contains("rewards." + args[2])) {
                                send(player, "invalid-amount");
                                return true;
                            }

                            rewards.set("rewards." + args[2] + ".price", price);
                            saveRewards();

                            player.sendMessage(color(PREFIX + "&aReward price updated!"));

                        } catch (NumberFormatException exception) {
                            send(player, "invalid-amount");
                        }
                    }
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                adminHelp(player);
                return true;
            }

            adminHelp(player);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player only.");
            return true;
        }

        if (args.length == 0) {
            openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "balance" -> {
                player.sendMessage(color(
                        PREFIX + "&7Your Gems: &b"
                                + getBalance(player.getUniqueId()) + " ✦"
                ));
            }

            case "shop" -> openCategories(player);

            case "top" -> showTop(player);

            case "buy" -> {
                if (args.length < 2) {
                    openBuy(player);
                    return true;
                }

                try {
                    buy(player, Integer.parseInt(args[1]));
                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "give", "take", "set", "check" -> {
                if (args.length < 3 || !isAdmin(player, "vortexgems.admin")) {
                    send(player, "no-permission");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

                if (!target.isOnline() && !target.hasPlayedBefore()) {
                    send(player, "player-not-found");
                    return true;
                }

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount < 0) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    UUID uuid = target.getUniqueId();
                    String name = target.getName() == null ? args[1] : target.getName();

                    switch (sub) {
                        case "give" -> {
                            if (addBalance(uuid, name, amount, "ADMIN_GIVE")) {
                                player.sendMessage(color(
                                        PREFIX + "&aGave &b" + amount + " Gems &ato " + name
                                ));
                            } else send(player, "invalid-amount");
                        }

                        case "take" -> {
                            if (removeBalance(uuid, name, amount, "ADMIN_TAKE")) {
                                player.sendMessage(color(
                                        PREFIX + "&aTook &b" + amount + " Gems &afrom " + name
                                ));
                            } else send(player, "not-enough-gems");
                        }

                        case "set" -> {
                            if (setBalance(uuid, name, amount)) {
                                player.sendMessage(color(
                                        PREFIX + "&aSet " + name + "'s balance to &b" + amount
                                ));
                                logTransaction(uuid, "ADMIN_SET", amount, "ADMIN_SET");
                            } else send(player, "invalid-amount");
                        }

                        case "check" -> player.sendMessage(color(
                                PREFIX + "&e" + name + " &7has &b"
                                        + getBalance(uuid) + " Gems"
                        ));
                    }

                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "reload" -> {
                if (!isAdmin(player, "vortexgems.reload")) {
                    send(player, "no-permission");
                    return true;
                }

                reloadConfig();
                loadShopFiles();
                send(player, "reload-success");
            }

            case "pay" -> {
                if (args.length < 3) {
                    player.sendMessage(color("&cUsage: /gems pay <player> <amount>"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null || target.equals(player)) {
                    send(player, "player-not-found");
                    return true;
                }

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount <= 0
                            || !removeBalance(player.getUniqueId(), player.getName(),
                            amount, "PLAYER_PAY")) {
                        send(player, "not-enough-gems");
                        return true;
                    }

                    if (!addBalance(target.getUniqueId(), target.getName(),
                            amount, "PLAYER_RECEIVE")) {
                        addBalance(player.getUniqueId(), player.getName(),
                                amount, "PAY_REFUND");
                        send(player, "invalid-amount");
                        return true;
                    }

                    player.sendMessage(color(PREFIX + "&aSent &b" + amount
                            + " Gems &ato " + target.getName()));

                    target.sendMessage(color(PREFIX + "&aReceived &b" + amount
                            + " Gems &afrom " + player.getName()));

                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "help" -> {
                player.sendMessage(color("&b/gems"));
                player.sendMessage(color("&b/gems balance"));
                player.sendMessage(color("&b/gems shop"));
                player.sendMessage(color("&b/gems buy <amount>"));
                player.sendMessage(color("&b/gems pay <player> <amount>"));
                player.sendMessage(color("&b/gems top"));
            }

            default -> openMain(player);
        }

        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        if (!title.contains("VortexGems")
                && !title.contains("Buy Gems")
                && !title.contains("Exchange")
                && !title.contains("Category Editor")
                && !title.contains("Reward Editor")) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != inventory) return;

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) return;

        if (title.contains("Admin")) {
            if (!isAdmin(player, "vortexgems.editor")) return;

            if (slot == 10) {
                openCategoryEditor(player);
            } else if (slot == 13) {
                openRewardEditor(player);
            } else if (slot == 16) {
                reloadConfig();
                loadShopFiles();
                send(player, "reload-success");
            }

            return;
        }

        if (title.contains("Category Editor")) {
            ConfigurationSection section =
                    categories.getConfigurationSection("categories");

            if (section == null) return;

            List<String> ids = new ArrayList<>(section.getKeys(false));

            if (slot < ids.size()) {
                toggleCategory(ids.get(slot));
                openCategoryEditor(player);
            }

            return;
        }

        if (title.contains("Reward Editor")) {
            ConfigurationSection section =
                    rewards.getConfigurationSection("rewards");

            if (section == null) return;

            List<String> ids = new ArrayList<>(section.getKeys(false));

            if (slot < ids.size()) {
                toggleReward(ids.get(slot));
                openRewardEditor(player);
            }

            return;
        }

        if (title.contains("Buy Gems")) {
            List<Integer> options = getConfig().getIntegerList(
                    "economy.purchase-options"
            );

            int[] slots = {10, 11, 12, 13, 14, 15, 16};

            for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
                if (slot == slots[i]) {
                    buy(player, options.get(i));
                    openBuy(player);
                    return;
                }
            }

            return;
        }

        if (title.contains("Exchange:")) {
            String category = title.substring("§8Exchange: ".length());

            ConfigurationSection section =
                    rewards.getConfigurationSection("rewards");

            if (section == null) return;

            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(category)) continue;

                if (rewards.getInt(path + ".slot", 10) == slot) {
                    exchange(player, id);
                    return;
                }
            }

            return;
        }

        if (title.contains("Exchange Categories")) {
            ConfigurationSection section =
                    categories.getConfigurationSection("categories");

            if (section == null) return;

            for (String id : section.getKeys(false)) {
                String path = "categories." + id;

                if (categories.getInt(path + ".slot", 11) == slot
                        && categories.getBoolean(path + ".enabled", true)) {
                    openRewards(player, id);
                    return;
                }
            }

            return;
        }

        if (title.contains("VortexGems")) {
            switch (slot) {
                case 10 -> openBuy(player);
                case 12 -> openCategories(player);
                case 14 -> player.sendMessage(color(
                        PREFIX + "&7Your balance: &b"
                                + getBalance(player.getUniqueId()) + " Gems"
                ));
                case 16 -> showTop(player);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        if (title.contains("VortexGems")
                || title.contains("Buy Gems")
                || title.contains("Exchange")
                || title.contains("Category Editor")
                || title.contains("Reward Editor")) {
            event.setCancelled(true);
        }
    }

    public long getPlayerGems(UUID uuid) {
        return getBalance(uuid);
    }

    public List<String> getLeaderboardNames() {
        List<String> names = new ArrayList<>();

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username FROM gems ORDER BY balance DESC LIMIT 10"
        ); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                names.add(result.getString("username"));
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return names;
    }

    public long getLeaderboardBalance(int position) {
        try (PreparedStatement statement = database.prepareStatement(
                "SELECT balance FROM gems ORDER BY balance DESC LIMIT 1 OFFSET ?"
        )) {
            statement.setInt(1, position - 1);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong("balance");
            }

        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    public int getLeaderboardRank(UUID uuid) {
        try (PreparedStatement statement = database.prepareStatement("""
                SELECT COUNT(*) + 1 AS rank
                FROM gems
                WHERE balance > ?
                """)) {

            statement.setLong(1, getBalance(uuid));

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt("rank");
            }

        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    private static final class GemsExpansion extends PlaceholderExpansion {

        private final VortexGems plugin;

        private GemsExpansion(VortexGems plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "vortexgems";
        }

        @Override
        public String getAuthor() {
            return "KingdomVortex";
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            if (params.equalsIgnoreCase("total")) {
                return String.valueOf(plugin.getLeaderboardNames().size());
            }

            if (params.equalsIgnoreCase("rank")) {
                if (player == null) return "0";
                return String.valueOf(
                        plugin.getLeaderboardRank(player.getUniqueId())
                );
            }

            if (params.equalsIgnoreCase("balance")
                    || params.equalsIgnoreCase("formatted")) {
                if (player == null) return "0";

                return String.valueOf(
                        plugin.getPlayerGems(player.getUniqueId())
                );
            }

            if (params.matches("top_[1-9][0-9]*_name")) {
                int rank = Integer.parseInt(
                        params.replace("top_", "").replace("_name", "")
                );

                List<String> names = plugin.getLeaderboardNames();

                return rank > 0 && rank <= names.size()
                        ? names.get(rank - 1) : "None";
            }

            if (params.matches("top_[1-9][0-9]*_balance")) {
                int rank = Integer.parseInt(
                        params.replace("top_", "").replace("_balance", "")
                );

                return String.valueOf(
                        plugin.getLeaderboardBalance(rank)
                );
            }

            return null;
        }
    }
}
