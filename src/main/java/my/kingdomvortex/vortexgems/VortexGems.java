
package my.kingdomvortex.vortexgems;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private File messagesFile;

    private YamlConfiguration categories;
    private YamlConfiguration rewards;
    private YamlConfiguration messages;

    private final Map<UUID, String> pendingInput = new HashMap<>();
    private final Map<UUID, Long> lastTransaction = new HashMap<>();
    private final Map<UUID, String> confirmations = new HashMap<>();

    private static final class Menu implements InventoryHolder {
        final String type;
        final String context;
        final Map<Integer, String> entries = new HashMap<>();
        final Map<Integer, Integer> amounts = new HashMap<>();
        Inventory inventory;

        Menu(String type, String context) {
            this.type = type;
            this.context = context;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveIfMissing("categories.yml");
        saveIfMissing("rewards.yml");
        saveIfMissing("messages.yml");

        categoriesFile = new File(getDataFolder(), "categories.yml");
        rewardsFile = new File(getDataFolder(), "rewards.yml");
        messagesFile = new File(getDataFolder(), "messages.yml");

        loadFiles();

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy provider not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupDatabase()) {
            getLogger().severe("SQLite database initialization failed!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("gems")).setExecutor(this);
        Objects.requireNonNull(getCommand("gemsadmin")).setExecutor(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GemsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ensurePlayer(player.getUniqueId(), player.getName());
        }

        getLogger().info("VortexGems v1.1.0 enabled.");
    }

    @Override
    public void onDisable() {
        pendingInput.clear();
        confirmations.clear();

        try {
            if (database != null && !database.isClosed()) {
                database.close();
            }
        } catch (SQLException exception) {
            getLogger().severe("Database close error: " + exception.getMessage());
        }

        getLogger().info("VortexGems disabled.");
    }

    private void saveIfMissing(String name) {
        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        return economy != null;
    }

    private boolean setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");

            String filename = getConfig().getString(
                    "storage.sqlite.file", "data.db"
            );

            File file = new File(getDataFolder(), filename);

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

                statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_gems_balance
                    ON gems(balance DESC)
                """);
            }

            return true;
        } catch (Exception exception) {
            getLogger().severe("SQLite error: " + exception.getMessage());
            return false;
        }
    }

    private synchronized void loadFiles() {
        reloadConfig();

        categories = YamlConfiguration.loadConfiguration(categoriesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private synchronized void saveCategories() {
        try {
            categories.save(categoriesFile);
        } catch (Exception exception) {
            getLogger().severe("Could not save categories.yml: "
                    + exception.getMessage());
        }
    }

    private synchronized void saveRewards() {
        try {
            rewards.save(rewardsFile);
        } catch (Exception exception) {
            getLogger().severe("Could not save rewards.yml: "
                    + exception.getMessage());
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String prefix() {
        return color(getConfig().getString(
                "settings.prefix", "&8[&bVortexGems&8] &7"
        ));
    }

    private String message(String key) {
        return prefix() + color(messages.getString(key, key));
    }

    private void send(Player player, String key) {
        player.sendMessage(message(key));
    }

    private void send(Player player, String key, String... replacements) {
        String text = message(key);

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }

        player.sendMessage(text);
    }

    // =========================================================
    // DATABASE
    // =========================================================

    private synchronized void ensurePlayer(UUID uuid, String name) {
        if (database == null) return;

        try (PreparedStatement statement = database.prepareStatement("""
            INSERT OR IGNORE INTO gems(uuid, username, balance)
            VALUES(?, ?, ?)
        """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3,
                    Math.max(0, getConfig().getLong("settings.starting-balance", 0)));
            statement.executeUpdate();

            try (PreparedStatement update = database.prepareStatement(
                    "UPDATE gems SET username = ? WHERE uuid = ?"
            )) {
                update.setString(1, name);
                update.setString(2, uuid.toString());
                update.executeUpdate();
            }

        } catch (SQLException exception) {
            getLogger().severe(exception.getMessage());
        }
    }

    private synchronized long balance(UUID uuid) {
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
        long maximum = getConfig().getLong(
                "settings.max-balance", 1000000000L
        );

        if (amount < 0 || amount > maximum) return false;

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

        long current = balance(uuid);
        long maximum = getConfig().getLong(
                "settings.max-balance", 1000000000L
        );

        if (current > maximum - amount) return false;

        if (!setBalance(uuid, name, current + amount)) return false;

        logTransaction(uuid, type, amount, type);
        return true;
    }

    private synchronized boolean removeBalance(
            UUID uuid, String name, long amount, String type
    ) {
        if (amount <= 0) return false;

        long current = balance(uuid);
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
            getLogger().warning("Transaction log failed: "
                    + exception.getMessage());
        }
    }

    // =========================================================
    // INVENTORY / GUI HELPERS
    // =========================================================

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            if (lore != null) {
                meta.setLore(lore.stream().map(this::color).toList());
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    private Material material(String value, Material fallback) {
        Material result = Material.matchMaterial(value == null ? "" : value);
        return result == null ? fallback : result;
    }

    private Menu menu(String type, String context, String title, int size) {
        Menu holder = new Menu(type, context);

        Inventory inventory = Bukkit.createInventory(
                holder, size, color(title)
        );

        holder.inventory = inventory;

        ItemStack filler = item(
                Material.GRAY_STAINED_GLASS_PANE,
                "&8 ",
                Collections.emptyList()
        );

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        return holder;
    }

    private void put(Menu menu, int slot, String id, ItemStack stack) {
        if (slot < 0 || slot >= menu.inventory.getSize()) return;

        menu.inventory.setItem(slot, stack);
        menu.entries.put(slot, id);
    }

    private boolean admin(Player player, String permission) {
        return (getConfig().getBoolean("admin.op-access", true)
                && player.isOp()) || player.hasPermission(permission);
    }

    // =========================================================
    // PLAYER MENUS
    // =========================================================

    public void openMain(Player player) {
        Menu menu = menu(
                "MAIN", "",
                getConfig().getString("menus.main.title", "&8VortexGems"),
                27
        );

        put(menu, 10, "buy", item(
                Material.EMERALD, "&aBuy Gems",
                List.of("&7Purchase Gems using Money.", "", "&eClick to open!")
        ));

        put(menu, 12, "shop", item(
                Material.CHEST, "&6Exchange Shop",
                List.of("&7Exchange Gems for rewards.", "", "&eClick to open!")
        ));

        put(menu, 14, "balance", item(
                Material.PLAYER_HEAD, "&bMy Balance",
                List.of(
                        "&7Your Gems: &b" + balance(player.getUniqueId()),
                        "", "&eClick to view!"
                )
        ));

        put(menu, 16, "top", item(
                Material.GOLD_BLOCK, "&6Leaderboard",
                List.of("&7View the richest players.", "", "&eClick to view!")
        ));

        player.openInventory(menu.inventory);
    }

    private void openBuy(Player player) {
        Menu menu = menu(
                "BUY", "",
                getConfig().getString("menus.buy.title", "&8Buy Gems"),
                27
        );

        List<Integer> options = getConfig().getIntegerList(
                "economy.purchase-options"
        );

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        long price = getConfig().getLong("economy.price-per-gem", 1500);

        for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
            int amount = options.get(i);

            if (amount <= 0 || price <= 0
                    || amount > Long.MAX_VALUE / price) continue;

            long total = amount * price;

            put(menu, slots[i], "purchase", item(
                    Material.EMERALD,
                    "&a" + amount + " Gems",
                    List.of(
                            "&7Price: &e$" + total,
                            "",
                            "&eClick to purchase!"
                    )
            ));

            menu.amounts.put(slots[i], amount);
        }

        put(menu, 22, "custom", item(
                Material.PAPER,
                "&bCustom Purchase",
                List.of(
                        "&7Use command:",
                        "&f/gems buy <amount>"
                )
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategories(Player player) {
        if (!getConfig().getBoolean("shop.enabled", true)) {
            player.sendMessage(color("&cExchange Shop is disabled."));
            return;
        }

        Menu menu = menu(
                "CATEGORIES", "",
                getConfig().getString(
                        "menus.categories.title", "&8Exchange Categories"
                ),
                54
        );

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "categories." + id;

                if (!categories.getBoolean(path + ".enabled", true)) continue;

                Material icon = material(
                        categories.getString(path + ".material"),
                        Material.CHEST
                );

                int slot = categories.getInt(path + ".slot", 11);

                put(menu, slot, id, item(
                        icon,
                        categories.getString(path + ".name", id),
                        categories.getStringList(path + ".lore")
                ));
            }
        }

        player.openInventory(menu.inventory);
    }

    private void openRewards(Player player, String categoryId) {
        String title = getConfig().getString(
                "menus.rewards.title", "&8Exchange: %category%"
        ).replace("%category%", categoryId);

        Menu menu = menu("REWARDS", categoryId, title, 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(categoryId)) continue;

                if (!rewards.getBoolean(path + ".enabled", true)) continue;

                Material icon = material(
                        rewards.getString(path + ".material"),
                        Material.STONE
                );

                int slot = rewards.getInt(path + ".slot", 10);
                int amount = rewards.getInt(path + ".amount", 1);
                long price = rewards.getLong(path + ".price", 1);

                List<String> lore = new ArrayList<>(
                        rewards.getStringList(path + ".lore")
                );

                lore.add("");
                lore.add("&7Price: &b" + price + " Gems");
                lore.add("&eClick to exchange!");

                ItemStack display = item(
                        icon,
                        rewards.getString(path + ".name", id),
                        lore
                );

                display.setAmount(Math.max(1, Math.min(64, amount)));

                put(menu, slot, id, display);
            }
        }

        player.openInventory(menu.inventory);
    }

    // =========================================================
    // ADMIN MENUS
    // =========================================================

    private void openAdmin(Player player) {
        if (!admin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return;
        }

        Menu menu = menu(
                "ADMIN", "",
                "&8VortexGems Admin", 27
        );

        put(menu, 10, "categories", item(
                Material.CHEST, "&bManage Categories",
                List.of("&7Create, edit, delete and toggle.", "", "&eClick!")
        ));

        put(menu, 12, "rewards", item(
                Material.EMERALD, "&aManage Rewards",
                List.of("&7Create, edit, delete and toggle.", "", "&eClick!")
        ));

        put(menu, 14, "create-category", item(
                Material.NAME_TAG, "&eCreate Category",
                List.of("&7Create category using chat.", "", "&eClick!")
        ));

        put(menu, 16, "create-reward", item(
                Material.ITEM_FRAME, "&eCreate Reward",
                List.of(
                        "&7Hold an item in your main hand.",
                        "&7Then create reward using chat.",
                        "", "&eClick!"
                )
        ));

        put(menu, 22, "reload", item(
                Material.REDSTONE, "&cReload Configuration",
                List.of("&7Reload YAML files.", "", "&eClick!")
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategoryEditor(Player player) {
        Menu menu = menu("CATEGORY_EDITOR", "", "&8Category Editor", 54);

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "categories." + id;

                Material icon = material(
                        categories.getString(path + ".material"),
                        Material.CHEST
                );

                boolean enabled = categories.getBoolean(path + ".enabled", true);

                put(menu, slot++, id, item(
                        icon,
                        categories.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to manage!"
                        )
                ));
            }
        }

        put(menu, 49, "back", item(
                Material.ARROW, "&cBack", List.of("&7Return to admin menu.")
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategoryActions(Player player, String id) {
        String path = "categories." + id;

        if (!categories.contains(path)) return;

        Menu menu = menu(
                "CATEGORY_ACTIONS", id,
                "&8Category: " + id, 27
        );

        put(menu, 10, "toggle", item(
                Material.LEVER, "&eToggle Category",
                List.of(
                        "&7Current: " +
                                (categories.getBoolean(path + ".enabled", true)
                                        ? "&aON" : "&cOFF"),
                        "", "&eClick to toggle!"
                )
        ));

        put(menu, 12, "rename", item(
                Material.NAME_TAG, "&bRename Category",
                List.of("&7Change category name using chat.")
        ));

        put(menu, 14, "icon", item(
                Material.ITEM_FRAME, "&bSet Icon From Hand",
                List.of("&7Hold the desired icon in your hand.")
        ));

        put(menu, 16, "delete", item(
                Material.BARRIER, "&cDelete Category",
                List.of("&7Delete category and its rewards.")
        ));

        put(menu, 22, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    private void openRewardEditor(Player player) {
        Menu menu = menu("REWARD_EDITOR", "", "&8Reward Editor", 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "rewards." + id;

                Material icon = material(
                        rewards.getString(path + ".material"),
                        Material.STONE
                );

                boolean enabled = rewards.getBoolean(path + ".enabled", true);

                put(menu, slot++, id, item(
                        icon,
                        rewards.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Category: &f" +
                                        rewards.getString(path + ".category", ""),
                                "&7Price: &b" +
                                        rewards.getLong(path + ".price") + " Gems",
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to manage!"
                        )
                ));
            }
        }

        put(menu, 49, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    private void openRewardActions(Player player, String id) {
        String path = "rewards." + id;

        if (!rewards.contains(path)) return;

        Menu menu = menu(
                "REWARD_ACTIONS", id,
                "&8Reward: " + id, 27
        );

        put(menu, 10, "toggle", item(
                Material.LEVER, "&eToggle Reward",
                List.of(
                        "&7Current: " +
                                (rewards.getBoolean(path + ".enabled", true)
                                        ? "&aON" : "&cOFF"),
                        "", "&eClick to toggle!"
                )
        ));

        put(menu, 12, "price", item(
                Material.EMERALD, "&bChange Price",
                List.of("&7Current: &b" +
                        rewards.getLong(path + ".price") + " Gems",
                        "", "&eClick and type price in chat.")
        ));

        put(menu, 14, "rename", item(
                Material.NAME_TAG, "&bChange Name",
                List.of("&7Click and type new name in chat.")
        ));

        put(menu, 16, "delete", item(
                Material.BARRIER, "&cDelete Reward",
                List.of("&7Permanently delete this reward.")
        ));

        put(menu, 22, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    // =========================================================
    // CHAT EDITOR
    // =========================================================

    private void requestInput(Player player, String type) {
        pendingInput.put(player.getUniqueId(), type);
        player.closeInventory();

        player.sendMessage(color(
                prefix() + "&eType your input in chat. Type &ccancel &eto cancel."
        ));
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String type = pendingInput.get(player.getUniqueId());

        if (type == null) return;

        event.setCancelled(true);

        String input = event.getMessage();

        Bukkit.getScheduler().runTask(this, () -> {
            if (!pendingInput.containsKey(player.getUniqueId())) return;

            pendingInput.remove(player.getUniqueId());

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(color(prefix() + "&cInput cancelled."));
                return;
            }

            String[] parts = input.split("\\|", -1);

            switch (type) {
                case "create-category" -> {
                    if (parts.length < 3) {
                        player.sendMessage(color(
                                "&cFormat: id|name|material"
                        ));
                        return;
                    }

                    String id = safeId(parts[0]);

                    Material icon = Material.matchMaterial(parts[2]);

                    if (id.isEmpty() || icon == null
                            || categories.contains("categories." + id)) {
                        player.sendMessage(color("&cInvalid category."));
                        return;
                    }

                    String path = "categories." + id;

                    categories.set(path + ".name", parts[1]);
                    categories.set(path + ".material", icon.name());
                    categories.set(path + ".slot", firstFreeCategorySlot());
                    categories.set(path + ".enabled", true);
                    categories.set(path + ".lore",
                            List.of("&7Custom category", "", "&eClick to open!"));

                    saveCategories();

                    player.sendMessage(color(prefix() + "&aCategory created!"));
                    openCategoryEditor(player);
                }

                case "create-reward" -> {
                    if (parts.length < 4) {
                        player.sendMessage(color(
                                "&cFormat: id|category|price|amount"
                        ));
                        return;
                    }

                    String id = safeId(parts[0]);
                    String category = safeId(parts[1]);

                    long price;
                    int amount;

                    try {
                        price = Long.parseLong(parts[2]);
                        amount = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException exception) {
                        player.sendMessage(color("&cInvalid price or amount."));
                        return;
                    }

                    ItemStack hand = player.getInventory().getItemInMainHand();

                    if (id.isEmpty() || rewards.contains("rewards." + id)
                            || !categories.contains("categories." + category)
                            || hand.getType().isAir()
                            || price <= 0 || amount < 1 || amount > 64) {
                        player.sendMessage(color("&cInvalid reward data."));
                        return;
                    }

                    String path = "rewards." + id;

                    rewards.set(path + ".category", category);
                    rewards.set(path + ".name",
                            hand.hasItemMeta()
                                    && hand.getItemMeta().hasDisplayName()
                                    ? hand.getItemMeta().getDisplayName()
                                    : id
                    );
                    rewards.set(path + ".material", hand.getType().name());
                    rewards.set(path + ".slot", firstFreeRewardSlot(category));
                    rewards.set(path + ".amount", amount);
                    rewards.set(path + ".price", price);
                    rewards.set(path + ".enabled", true);
                    rewards.set(path + ".reward-type", "ITEM");
                    rewards.set(path + ".lore", List.of(
                            "&7Custom reward",
                            "",
                            "&7Price: &b" + price + " Gems"
                    ));
                    rewards.set(path + ".commands", Collections.emptyList());

                    saveRewards();

                    player.sendMessage(color(prefix() + "&aReward created!"));
                    openRewardEditor(player);
                }

                case "rename-category" -> {
                    String path = "categories." + typeValue(type);

                    player.sendMessage(color("&cInvalid editor state."));
                }

                case "category-name" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !categories.contains("categories." + id)) return;

                    categories.set("categories." + id + ".name", input);
                    saveCategories();
                    pendingContext.remove(player.getUniqueId());

                    player.sendMessage(color(prefix() + "&aCategory renamed!"));
                    openCategoryActions(player, id);
                }

                case "reward-name" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !rewards.contains("rewards." + id)) return;

                    rewards.set("rewards." + id + ".name", input);
                    saveRewards();
                    pendingContext.remove(player.getUniqueId());

                    player.sendMessage(color(prefix() + "&aReward renamed!"));
                    openRewardActions(player, id);
                }

                case "reward-price" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !rewards.contains("rewards." + id)) return;

                    try {
                        long price = Long.parseLong(input);

                        if (price <= 0) {
                            player.sendMessage(color("&cPrice must be positive."));
                            return;
                        }

                        rewards.set("rewards." + id + ".price", price);
                        saveRewards();
                        pendingContext.remove(player.getUniqueId());

                        player.sendMessage(color(prefix() + "&aPrice updated!"));
                        openRewardActions(player, id);

                    } catch (NumberFormatException exception) {
                        player.sendMessage(color("&cInvalid price."));
                    }
                }
            }
        });
    }

    private final Map<UUID, String> pendingContext = new HashMap<>();

    private String typeValue(String value) {
        return value;
    }

    private String safeId(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
    }

    private int firstFreeCategorySlot() {
        boolean[] used = new boolean[54];

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                int slot = categories.getInt("categories." + id + ".slot", -1);
                if (slot >= 0 && slot < 54) used[slot] = true;
            }
        }

        for (int i = 0; i < 54; i++) {
            if (!used[i]) return i;
        }

        return 0;
    }

    private int firstFreeRewardSlot(String category) {
        boolean[] used = new boolean[54];

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(category)) continue;

                int slot = rewards.getInt(path + ".slot", -1);
                if (slot >= 0 && slot < 54) used[slot] = true;
            }
        }

        for (int i = 0; i < 54; i++) {
            if (!used[i]) return i;
        }

        return 0;
    }

    // =========================================================
    // CATEGORY / REWARD MANAGEMENT
    // =========================================================

    private void deleteCategory(String id) {
        categories.set("categories." + id, null);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String rewardId : new ArrayList<>(section.getKeys(false))) {
                if (rewards.getString(
                        "rewards." + rewardId + ".category", ""
                ).equalsIgnoreCase(id)) {
                    rewards.set("rewards." + rewardId, null);
                }
            }
        }

        saveCategories();
        saveRewards();
    }

    private void toggleCategory(String id) {
        String path = "categories." + id;
        if (!categories.contains(path)) return;

        categories.set(path + ".enabled",
                !categories.getBoolean(path + ".enabled", true));

        saveCategories();
    }

    private void toggleReward(String id) {
        String path = "rewards." + id;
        if (!rewards.contains(path)) return;

        rewards.set(path + ".enabled",
                !rewards.getBoolean(path + ".enabled", true));

        saveRewards();
    }

    private void setCategoryIcon(Player player, String id) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage(color("&cHold an item first."));
            return;
        }

        categories.set(
                "categories." + id + ".material",
                hand.getType().name()
        );

        saveCategories();
        player.sendMessage(color(prefix() + "&aCategory icon updated!"));
    }

    // =========================================================
    // ECONOMY
    // =========================================================

    private void buy(Player player, int amount) {
        if (!getConfig().getBoolean("economy.enabled", true)
                || !getConfig().getBoolean("economy.buy-enabled", true)) {
            player.sendMessage(color("&cGems purchase is disabled."));
            return;
        }

        int minimum = getConfig().getInt("economy.min-purchase", 1);
        int maximum = getConfig().getInt("economy.max-purchase", 10000);
        long price = getConfig().getLong("economy.price-per-gem", 1500);

        if (amount < minimum || amount > maximum || price <= 0
                || amount > Long.MAX_VALUE / price) {
            send(player, "invalid-amount");
            return;
        }

        long total = amount * price;

        long now = System.currentTimeMillis();

        if (getConfig().getBoolean(
                "security.prevent-duplicate-purchase", true
        )) {
            if (now - lastTransaction.getOrDefault(
                    player.getUniqueId(), 0L
            ) < 1000) return;
        }

        if (!economy.has(player, total)) {
            send(player, "not-enough-money");
            return;
        }

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
            player.sendMessage(color(prefix()
                    + "&cTransaction failed. Money refunded."));
            return;
        }

        lastTransaction.put(player.getUniqueId(), now);

        send(player, "purchase-success",
                "%amount%", String.valueOf(amount));
    }

    private boolean canFit(Player player, ItemStack item) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = item.getAmount();

        for (ItemStack content : contents) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(
                        0, content.getMaxStackSize() - content.getAmount()
                );
            }

            if (remaining <= 0) return true;
        }

        return false;
    }

    private void exchange(Player player, String id) {
        String path = "rewards." + id;

        if (!rewards.contains(path)
                || !rewards.getBoolean(path + ".enabled", true)) return;

        long price = rewards.getLong(path + ".price");

        if (price <= 0 || balance(player.getUniqueId()) < price) {
            send(player, "not-enough-gems");
            return;
        }

        String type = rewards.getString(path + ".reward-type", "ITEM");

        Material material = Material.matchMaterial(
                rewards.getString(path + ".material", "STONE")
        );

        int amount = rewards.getInt(path + ".amount", 1);

        ItemStack rewardItem = null;

        if (type.equalsIgnoreCase("ITEM")) {
            if (material == null || material.isAir()
                    || amount < 1 || amount > 64) {
                player.sendMessage(color("&cInvalid reward item."));
                return;
            }

            rewardItem = new ItemStack(material, amount);

            if (!canFit(player, rewardItem)) {
                send(player, "inventory-full");
                return;
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            List<String> commands = rewards.getStringList(path + ".commands");

            if (commands.isEmpty()) {
                player.sendMessage(color("&cReward commands are empty."));
                return;
            }
        } else {
            player.sendMessage(color("&cUnknown reward type."));
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

        boolean success = true;

        if (type.equalsIgnoreCase("ITEM")) {
            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(rewardItem);

            if (!leftovers.isEmpty()) {
                success = false;

                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(
                            player.getLocation(), leftover
                    );
                }
            }
        } else {
            for (String command : rewards.getStringList(path + ".commands")) {
                String parsed = command.replace(
                        "%player%", player.getName()
                );

                boolean dispatched = Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(), parsed
                );

                if (!dispatched) {
                    getLogger().warning(
                            "Reward command was not handled: " + parsed
                    );
                    success = false;
                    break;
                }
            }
        }

        if (!success) {
            addBalance(
                    player.getUniqueId(),
                    player.getName(),
                    price,
                    "REFUND"
            );

            player.sendMessage(color(prefix()
                    + "&cReward execution failed. Gems refunded."));
            return;
        }

        send(player, "exchange-success",
                "%reward%", rewards.getString(path + ".name", id));

        logTransaction(
                player.getUniqueId(), "REWARD", price, id
        );

        if (getConfig().getBoolean("shop.close-after-purchase", false)) {
            player.closeInventory();
        }
    }

    // =========================================================
    // LEADERBOARD
    // =========================================================

    private void showTop(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&l       GEMS LEADERBOARD"));
        player.sendMessage(color("&8&m------------------------------"));

        int limit = Math.max(1, getConfig().getInt(
                "leaderboard.max-entries", 10
        ));

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username, balance FROM gems ORDER BY balance DESC LIMIT ?"
        )) {
            statement.setInt(1, limit);

            try (ResultSet result = statement.executeQuery()) {
                int rank = 1;

                while (result.next()) {
                    player.sendMessage(color(
                            "&e#" + rank + " &f" +
                                    result.getString("username") +
                                    " &8- &b" +
                                    result.getLong("balance") + " Gems"
                    ));

                    rank++;
                }
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        player.sendMessage(color("&8&m------------------------------"));
    }

    private long topBalance(int position) {
        if (position < 1) return 0;

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

    private String topName(int position) {
        if (position < 1) return "None";

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username FROM gems ORDER BY balance DESC LIMIT 1 OFFSET ?"
        )) {
            statement.setInt(1, position - 1);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getString("username");
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return "None";
    }

    private int rank(UUID uuid) {
        try (PreparedStatement statement = database.prepareStatement("""
            SELECT COUNT(*) + 1 AS rank
            FROM gems
            WHERE balance > (SELECT balance FROM gems WHERE uuid = ?)
        """)) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt("rank");
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    private long totalPlayers() {
        try (Statement statement = database.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM gems"
             )) {
            if (result.next()) return result.getLong(1);
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    private boolean hasPermission(Player player, String permission) {
        return admin(player, permission);
    }

    private void help(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&lVortexGems Commands"));
        player.sendMessage(color("&f/gems"));
        player.sendMessage(color("&f/gems balance"));
        player.sendMessage(color("&f/gems shop"));
        player.sendMessage(color("&f/gems buy <amount>"));
        player.sendMessage(color("&f/gems pay <player> <amount>"));
        player.sendMessage(color("&f/gems top"));
        player.sendMessage(color("&f/gems give <player> <amount>"));
        player.sendMessage(color("&f/gems take <player> <amount>"));
        player.sendMessage(color("&f/gems set <player> <amount>"));
        player.sendMessage(color("&f/gems check <player>"));
        player.sendMessage(color("&f/gems reload"));
        player.sendMessage(color("&8&m------------------------------"));
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            if (command.getName().equalsIgnoreCase("gems")
                    && args.length >= 3) {
                return consoleAdmin(sender, args);
            }

            sender.sendMessage("This command requires a player.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("gemsadmin")) {
            return adminCommand(player, args);
        }

        if (!player.hasPermission("vortexgems.use")) {
            send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "balance" -> player.sendMessage(color(
                    prefix() + "&7Your balance: &b" +
                            balance(player.getUniqueId()) + " Gems"
            ));

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

            case "pay" -> {
                if (!player.hasPermission("vortexgems.pay")
                        || args.length < 3) {
                    send(player, "no-permission");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null || target.equals(player)) {
                    send(player, "player-not-found");
                    return true;
                }

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount <= 0 || amount > getConfig().getLong(
                            "settings.max-balance", 1000000000L
                    )) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    synchronized (database) {
                        if (!removeBalance(
                                player.getUniqueId(),
                                player.getName(),
                                amount,
                                "PLAYER_PAY"
                        )) {
                            send(player, "not-enough-gems");
                            return true;
                        }

                        if (!addBalance(
                                target.getUniqueId(),
                                target.getName(),
                                amount,
                                "PLAYER_RECEIVE"
                        )) {
                            addBalance(
                                    player.getUniqueId(),
                                    player.getName(),
                                    amount,
                                    "PAY_REFUND"
                            );
                            send(player, "invalid-amount");
                            return true;
                        }
                    }

                    player.sendMessage(color(prefix()
                            + "&aSent &b" + amount + " Gems &ato "
                            + target.getName()));

                    target.sendMessage(color(prefix()
                            + "&aReceived &b" + amount + " Gems &afrom "
                            + player.getName()));

                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "give", "take", "set", "check" -> {
                if (args.length < 3) {
                    help(player);
                    return true;
                }

                String permission = switch (sub) {
                    case "give" -> "vortexgems.give";
                    case "take" -> "vortexgems.take";
                    case "set" -> "vortexgems.set";
                    default -> "vortexgems.check";
                };

                if (!hasPermission(player, permission)) {
                    send(player, "no-permission");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount < 0) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    String name = target.getName() == null
                            ? args[1] : target.getName();

                    switch (sub) {
                        case "give" -> {
                            if (addBalance(
                                    target.getUniqueId(), name,
                                    amount, "ADMIN_GIVE"
                            )) {
                                send(player, "give-success",
                                        "%amount%", String.valueOf(amount),
                                        "%player%", name);
                            } else send(player, "invalid-amount");
                        }

                        case "take" -> {
                            if (removeBalance(
                                    target.getUniqueId(), name,
                                    amount, "ADMIN_TAKE"
                            )) {
                                send(player, "take-success",
                                        "%amount%", String.valueOf(amount),
                                        "%player%", name);
                            } else send(player, "not-enough-gems");
                        }

                        case "set" -> {
                            if (setBalance(target.getUniqueId(), name, amount)) {
                                logTransaction(
                                        target.getUniqueId(),
                                        "ADMIN_SET", amount, "ADMIN_SET"
                                );
                                send(player, "set-success",
                                        "%player%", name);
                            } else send(player, "invalid-amount");
                        }

                        case "check" -> player.sendMessage(color(
                                prefix() + "&e" + name + " &7has &b" +
                                        balance(target.getUniqueId()) + " Gems"
                        ));
                    }
                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "reload" -> {
                if (!hasPermission(player, "vortexgems.reload")) {
                    send(player, "no-permission");
                    return true;
                }

                loadFiles();
                send(player, "reload-success");
            }

            case "help" -> help(player);

            default -> help(player);
        }

        return true;
    }

    private boolean consoleAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /gems <give|take|set|check> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        try {
            long amount = Long.parseLong(args[2]);

            if (amount < 0) {
                sender.sendMessage("Invalid amount.");
                return true;
            }

            String name = target.getName() == null ? args[1] : target.getName();

            boolean success = switch (action) {
                case "give" -> addBalance(
                        target.getUniqueId(), name, amount, "CONSOLE_GIVE"
                );
                case "take" -> removeBalance(
                        target.getUniqueId(), name, amount, "CONSOLE_TAKE"
                );
                case "set" -> setBalance(
                        target.getUniqueId(), name, amount
                );
                case "check" -> {
                    sender.sendMessage(name + " has "
                            + balance(target.getUniqueId()) + " Gems.");
                    yield true;
                }
                default -> false;
            };

            sender.sendMessage(success
                    ? "VortexGems: Operation successful."
                    : "VortexGems: Operation failed.");

        } catch (NumberFormatException exception) {
            sender.sendMessage("Invalid amount.");
        }

        return true;
    }

    private boolean adminCommand(Player player, String[] args) {
        if (!admin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            openAdmin(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(color("&b/gemsadmin"));
            player.sendMessage(color("&b/gemsadmin category create"));
            player.sendMessage(color("&b/gemsadmin category delete <id>"));
            player.sendMessage(color("&b/gemsadmin category toggle <id>"));
            player.sendMessage(color("&b/gemsadmin reward addhand"));
            player.sendMessage(color("&b/gemsadmin reward delete <id>"));
            player.sendMessage(color("&b/gemsadmin reward toggle <id>"));
            player.sendMessage(color("&b/gemsadmin reward price <id> <price>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("category") && args.length >= 3) {
            String action = args[1].toLowerCase(Locale.ROOT);
            String id = safeId(args[2]);

            switch (action) {
                case "create" -> requestInput(player, "create-category");

                case "delete" -> {
                    if (!categories.contains("categories." + id)) {
                        player.sendMessage(color("&cCategory not found."));
                        return true;
                    }

                    deleteCategory(id);
                    player.sendMessage(color(prefix()
                            + "&aCategory and its rewards deleted."));
                }

                case "toggle" -> {
                    toggleCategory(id);
                    player.sendMessage(color(prefix() + "&aCategory updated."));
                }
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reward")) {
            if (args.length >= 2) {
                String action = args[1].toLowerCase(Locale.ROOT);

                switch (action) {
                    case "addhand" -> requestInput(player, "create-reward");

                    case "delete" -> {
                        if (args.length < 3) return true;

                        rewards.set("rewards." + safeId(args[2]), null);
                        saveRewards();

                        player.sendMessage(color(prefix() + "&aReward deleted."));
                    }

                    case "toggle" -> {
                        if (args.length < 3) return true;

                        toggleReward(safeId(args[2]));
                        player.sendMessage(color(prefix() + "&aReward updated."));
                    }

                    case "price" -> {
                        if (args.length < 4) return true;

                        String id = safeId(args[2]);

                        try {
                            long price = Long.parseLong(args[3]);

                            if (price <= 0 || !rewards.contains("rewards." + id)) {
                                send(player, "invalid-amount");
                                return true;
                            }

                            rewards.set("rewards." + id + ".price", price);
                            saveRewards();

                            player.sendMessage(color(prefix() + "&aPrice updated."));
                        } catch (NumberFormatException exception) {
                            send(player, "invalid-amount");
                        }
                    }
                }
            }

            return true;
        }

        openAdmin(player);
        return true;
    }

    // =========================================================
    // INVENTORY EVENTS
    // =========================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof Menu menu)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != top) return;

        int slot = event.getRawSlot();

        String id = menu.entries.get(slot);

        if (id == null) return;

        switch (menu.type) {
            case "MAIN" -> {
                switch (id) {
                    case "buy" -> openBuy(player);
                    case "shop" -> openCategories(player);
                    case "balance" -> player.sendMessage(color(
                            prefix() + "&7Your balance: &b" +
                                    balance(player.getUniqueId()) + " Gems"
                    ));
                    case "top" -> showTop(player);
                }
            }

            case "BUY" -> {
                if (id.equals("purchase")) {
                    int amount = menu.amounts.getOrDefault(slot, 0);

                    if (amount > 0) {
                        buy(player, amount);
                        openBuy(player);
                    }
                }
            }

            case "CATEGORIES" -> openRewards(player, id);

            case "REWARDS" -> exchange(player, id);

            case "ADMIN" -> {
                switch (id) {
                    case "categories" -> openCategoryEditor(player);
                    case "rewards" -> openRewardEditor(player);
                    case "create-category" -> requestInput(
                            player, "create-category"
                    );
                    case "create-reward" -> requestInput(
                            player, "create-reward"
                    );
                    case "reload" -> {
                        loadFiles();
                        send(player, "reload-success");
                        openAdmin(player);
                    }
                }
            }

            case "CATEGORY_EDITOR" -> {
                if (id.equals("back")) {
                    openAdmin(player);
                } else {
                    openCategoryActions(player, id);
                }
            }

            case "CATEGORY_ACTIONS" -> {
                String categoryId = menu.context;

                switch (id) {
                    case "toggle" -> {
                        toggleCategory(categoryId);
                        openCategoryActions(player, categoryId);
                    }

                    case "rename" -> {
                        pendingContext.put(player.getUniqueId(), categoryId);
                        requestInput(player, "category-name");
                    }

                    case "icon" -> {
                        setCategoryIcon(player, categoryId);
                        openCategoryActions(player, categoryId);
                    }

                    case "delete" -> {
                        deleteCategory(categoryId);
                        player.sendMessage(color(prefix()
                                + "&aCategory deleted."));
                        openCategoryEditor(player);
                    }

                    case "back" -> openCategoryEditor(player);
                }
            }

            case "REWARD_EDITOR" -> {
                if (id.equals("back")) {
                    openAdmin(player);
                } else {
                    openRewardActions(player, id);
                }
            }

            case "REWARD_ACTIONS" -> {
                String rewardId = menu.context;

                switch (id) {
                    case "toggle" -> {
                        toggleReward(rewardId);
                        openRewardActions(player, rewardId);
                    }

                    case "price" -> {
                        pendingContext.put(player.getUniqueId(), rewardId);
                        requestInput(player, "reward-price");
                    }

                    case "rename" -> {
                        pendingContext.put(player.getUniqueId(), rewardId);
                        requestInput(player, "reward-name");
                    }

                    case "delete" -> {
                        rewards.set("rewards." + rewardId, null);
                        saveRewards();

                        player.sendMessage(color(prefix()
                                + "&aReward deleted."));
                        openRewardEditor(player);
                    }

                    case "back" -> openRewardEditor(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ensurePlayer(player.getUniqueId(), player.getName());
    }

    // =========================================================
    // PLACEHOLDER API
    // =========================================================

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
                return String.valueOf(plugin.totalPlayers());
            }

            if (params.equalsIgnoreCase("top_1_name")) {
                return plugin.topName(1);
            }

            if (params.matches("top_[1-9][0-9]*_name")) {
                int rank = Integer.parseInt(
                        params.substring(4, params.length() - 5)
                );
                return plugin.topName(rank);
            }

            if (params.matches("top_[1-9][0-9]*_balance")) {
                int rank = Integer.parseInt(
                        params.substring(4, params.length() - 8)
                );
                return String.valueOf(plugin.topBalance(rank));
            }

            if (player == null) return "0";

            if (params.equalsIgnoreCase("balance")
                    || params.equalsIgnoreCase("formatted")) {
                return String.valueOf(
                        plugin.balance(player.getUniqueId())
                );
            }

            if (params.equalsIgnoreCase("rank")) {
                return String.valueOf(
                        plugin.rank(player.getUniqueId())
                );
            }

            return null;
        }
    }
}

package my.kingdomvortex.vortexgems;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private File messagesFile;

    private YamlConfiguration categories;
    private YamlConfiguration rewards;
    private YamlConfiguration messages;

    private final Map<UUID, String> pendingInput = new HashMap<>();
    private final Map<UUID, Long> lastTransaction = new HashMap<>();
    private final Map<UUID, String> confirmations = new HashMap<>();

    private static final class Menu implements InventoryHolder {
        final String type;
        final String context;
        final Map<Integer, String> entries = new HashMap<>();
        final Map<Integer, Integer> amounts = new HashMap<>();
        Inventory inventory;

        Menu(String type, String context) {
            this.type = type;
            this.context = context;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveIfMissing("categories.yml");
        saveIfMissing("rewards.yml");
        saveIfMissing("messages.yml");

        categoriesFile = new File(getDataFolder(), "categories.yml");
        rewardsFile = new File(getDataFolder(), "rewards.yml");
        messagesFile = new File(getDataFolder(), "messages.yml");

        loadFiles();

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy provider not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupDatabase()) {
            getLogger().severe("SQLite database initialization failed!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("gems")).setExecutor(this);
        Objects.requireNonNull(getCommand("gemsadmin")).setExecutor(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GemsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ensurePlayer(player.getUniqueId(), player.getName());
        }

        getLogger().info("VortexGems v1.1.0 enabled.");
    }

    @Override
    public void onDisable() {
        pendingInput.clear();
        confirmations.clear();

        try {
            if (database != null && !database.isClosed()) {
                database.close();
            }
        } catch (SQLException exception) {
            getLogger().severe("Database close error: " + exception.getMessage());
        }

        getLogger().info("VortexGems disabled.");
    }

    private void saveIfMissing(String name) {
        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        return economy != null;
    }

    private boolean setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");

            String filename = getConfig().getString(
                    "storage.sqlite.file", "data.db"
            );

            File file = new File(getDataFolder(), filename);

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

                statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_gems_balance
                    ON gems(balance DESC)
                """);
            }

            return true;
        } catch (Exception exception) {
            getLogger().severe("SQLite error: " + exception.getMessage());
            return false;
        }
    }

    private synchronized void loadFiles() {
        reloadConfig();

        categories = YamlConfiguration.loadConfiguration(categoriesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private synchronized void saveCategories() {
        try {
            categories.save(categoriesFile);
        } catch (Exception exception) {
            getLogger().severe("Could not save categories.yml: "
                    + exception.getMessage());
        }
    }

    private synchronized void saveRewards() {
        try {
            rewards.save(rewardsFile);
        } catch (Exception exception) {
            getLogger().severe("Could not save rewards.yml: "
                    + exception.getMessage());
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String prefix() {
        return color(getConfig().getString(
                "settings.prefix", "&8[&bVortexGems&8] &7"
        ));
    }

    private String message(String key) {
        return prefix() + color(messages.getString(key, key));
    }

    private void send(Player player, String key) {
        player.sendMessage(message(key));
    }

    private void send(Player player, String key, String... replacements) {
        String text = message(key);

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }

        player.sendMessage(text);
    }

    // =========================================================
    // DATABASE
    // =========================================================

    private synchronized void ensurePlayer(UUID uuid, String name) {
        if (database == null) return;

        try (PreparedStatement statement = database.prepareStatement("""
            INSERT OR IGNORE INTO gems(uuid, username, balance)
            VALUES(?, ?, ?)
        """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3,
                    Math.max(0, getConfig().getLong("settings.starting-balance", 0)));
            statement.executeUpdate();

            try (PreparedStatement update = database.prepareStatement(
                    "UPDATE gems SET username = ? WHERE uuid = ?"
            )) {
                update.setString(1, name);
                update.setString(2, uuid.toString());
                update.executeUpdate();
            }

        } catch (SQLException exception) {
            getLogger().severe(exception.getMessage());
        }
    }

    private synchronized long balance(UUID uuid) {
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
        long maximum = getConfig().getLong(
                "settings.max-balance", 1000000000L
        );

        if (amount < 0 || amount > maximum) return false;

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

        long current = balance(uuid);
        long maximum = getConfig().getLong(
                "settings.max-balance", 1000000000L
        );

        if (current > maximum - amount) return false;

        if (!setBalance(uuid, name, current + amount)) return false;

        logTransaction(uuid, type, amount, type);
        return true;
    }

    private synchronized boolean removeBalance(
            UUID uuid, String name, long amount, String type
    ) {
        if (amount <= 0) return false;

        long current = balance(uuid);
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
            getLogger().warning("Transaction log failed: "
                    + exception.getMessage());
        }
    }

    // =========================================================
    // INVENTORY / GUI HELPERS
    // =========================================================

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            if (lore != null) {
                meta.setLore(lore.stream().map(this::color).toList());
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    private Material material(String value, Material fallback) {
        Material result = Material.matchMaterial(value == null ? "" : value);
        return result == null ? fallback : result;
    }

    private Menu menu(String type, String context, String title, int size) {
        Menu holder = new Menu(type, context);

        Inventory inventory = Bukkit.createInventory(
                holder, size, color(title)
        );

        holder.inventory = inventory;

        ItemStack filler = item(
                Material.GRAY_STAINED_GLASS_PANE,
                "&8 ",
                Collections.emptyList()
        );

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        return holder;
    }

    private void put(Menu menu, int slot, String id, ItemStack stack) {
        if (slot < 0 || slot >= menu.inventory.getSize()) return;

        menu.inventory.setItem(slot, stack);
        menu.entries.put(slot, id);
    }

    private boolean admin(Player player, String permission) {
        return (getConfig().getBoolean("admin.op-access", true)
                && player.isOp()) || player.hasPermission(permission);
    }

    // =========================================================
    // PLAYER MENUS
    // =========================================================

    public void openMain(Player player) {
        Menu menu = menu(
                "MAIN", "",
                getConfig().getString("menus.main.title", "&8VortexGems"),
                27
        );

        put(menu, 10, "buy", item(
                Material.EMERALD, "&aBuy Gems",
                List.of("&7Purchase Gems using Money.", "", "&eClick to open!")
        ));

        put(menu, 12, "shop", item(
                Material.CHEST, "&6Exchange Shop",
                List.of("&7Exchange Gems for rewards.", "", "&eClick to open!")
        ));

        put(menu, 14, "balance", item(
                Material.PLAYER_HEAD, "&bMy Balance",
                List.of(
                        "&7Your Gems: &b" + balance(player.getUniqueId()),
                        "", "&eClick to view!"
                )
        ));

        put(menu, 16, "top", item(
                Material.GOLD_BLOCK, "&6Leaderboard",
                List.of("&7View the richest players.", "", "&eClick to view!")
        ));

        player.openInventory(menu.inventory);
    }

    private void openBuy(Player player) {
        Menu menu = menu(
                "BUY", "",
                getConfig().getString("menus.buy.title", "&8Buy Gems"),
                27
        );

        List<Integer> options = getConfig().getIntegerList(
                "economy.purchase-options"
        );

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        long price = getConfig().getLong("economy.price-per-gem", 1500);

        for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
            int amount = options.get(i);

            if (amount <= 0 || price <= 0
                    || amount > Long.MAX_VALUE / price) continue;

            long total = amount * price;

            put(menu, slots[i], "purchase", item(
                    Material.EMERALD,
                    "&a" + amount + " Gems",
                    List.of(
                            "&7Price: &e$" + total,
                            "",
                            "&eClick to purchase!"
                    )
            ));

            menu.amounts.put(slots[i], amount);
        }

        put(menu, 22, "custom", item(
                Material.PAPER,
                "&bCustom Purchase",
                List.of(
                        "&7Use command:",
                        "&f/gems buy <amount>"
                )
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategories(Player player) {
        if (!getConfig().getBoolean("shop.enabled", true)) {
            player.sendMessage(color("&cExchange Shop is disabled."));
            return;
        }

        Menu menu = menu(
                "CATEGORIES", "",
                getConfig().getString(
                        "menus.categories.title", "&8Exchange Categories"
                ),
                54
        );

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "categories." + id;

                if (!categories.getBoolean(path + ".enabled", true)) continue;

                Material icon = material(
                        categories.getString(path + ".material"),
                        Material.CHEST
                );

                int slot = categories.getInt(path + ".slot", 11);

                put(menu, slot, id, item(
                        icon,
                        categories.getString(path + ".name", id),
                        categories.getStringList(path + ".lore")
                ));
            }
        }

        player.openInventory(menu.inventory);
    }

    private void openRewards(Player player, String categoryId) {
        String title = getConfig().getString(
                "menus.rewards.title", "&8Exchange: %category%"
        ).replace("%category%", categoryId);

        Menu menu = menu("REWARDS", categoryId, title, 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(categoryId)) continue;

                if (!rewards.getBoolean(path + ".enabled", true)) continue;

                Material icon = material(
                        rewards.getString(path + ".material"),
                        Material.STONE
                );

                int slot = rewards.getInt(path + ".slot", 10);
                int amount = rewards.getInt(path + ".amount", 1);
                long price = rewards.getLong(path + ".price", 1);

                List<String> lore = new ArrayList<>(
                        rewards.getStringList(path + ".lore")
                );

                lore.add("");
                lore.add("&7Price: &b" + price + " Gems");
                lore.add("&eClick to exchange!");

                ItemStack display = item(
                        icon,
                        rewards.getString(path + ".name", id),
                        lore
                );

                display.setAmount(Math.max(1, Math.min(64, amount)));

                put(menu, slot, id, display);
            }
        }

        player.openInventory(menu.inventory);
    }

    // =========================================================
    // ADMIN MENUS
    // =========================================================

    private void openAdmin(Player player) {
        if (!admin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return;
        }

        Menu menu = menu(
                "ADMIN", "",
                "&8VortexGems Admin", 27
        );

        put(menu, 10, "categories", item(
                Material.CHEST, "&bManage Categories",
                List.of("&7Create, edit, delete and toggle.", "", "&eClick!")
        ));

        put(menu, 12, "rewards", item(
                Material.EMERALD, "&aManage Rewards",
                List.of("&7Create, edit, delete and toggle.", "", "&eClick!")
        ));

        put(menu, 14, "create-category", item(
                Material.NAME_TAG, "&eCreate Category",
                List.of("&7Create category using chat.", "", "&eClick!")
        ));

        put(menu, 16, "create-reward", item(
                Material.ITEM_FRAME, "&eCreate Reward",
                List.of(
                        "&7Hold an item in your main hand.",
                        "&7Then create reward using chat.",
                        "", "&eClick!"
                )
        ));

        put(menu, 22, "reload", item(
                Material.REDSTONE, "&cReload Configuration",
                List.of("&7Reload YAML files.", "", "&eClick!")
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategoryEditor(Player player) {
        Menu menu = menu("CATEGORY_EDITOR", "", "&8Category Editor", 54);

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "categories." + id;

                Material icon = material(
                        categories.getString(path + ".material"),
                        Material.CHEST
                );

                boolean enabled = categories.getBoolean(path + ".enabled", true);

                put(menu, slot++, id, item(
                        icon,
                        categories.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to manage!"
                        )
                ));
            }
        }

        put(menu, 49, "back", item(
                Material.ARROW, "&cBack", List.of("&7Return to admin menu.")
        ));

        player.openInventory(menu.inventory);
    }

    private void openCategoryActions(Player player, String id) {
        String path = "categories." + id;

        if (!categories.contains(path)) return;

        Menu menu = menu(
                "CATEGORY_ACTIONS", id,
                "&8Category: " + id, 27
        );

        put(menu, 10, "toggle", item(
                Material.LEVER, "&eToggle Category",
                List.of(
                        "&7Current: " +
                                (categories.getBoolean(path + ".enabled", true)
                                        ? "&aON" : "&cOFF"),
                        "", "&eClick to toggle!"
                )
        ));

        put(menu, 12, "rename", item(
                Material.NAME_TAG, "&bRename Category",
                List.of("&7Change category name using chat.")
        ));

        put(menu, 14, "icon", item(
                Material.ITEM_FRAME, "&bSet Icon From Hand",
                List.of("&7Hold the desired icon in your hand.")
        ));

        put(menu, 16, "delete", item(
                Material.BARRIER, "&cDelete Category",
                List.of("&7Delete category and its rewards.")
        ));

        put(menu, 22, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    private void openRewardEditor(Player player) {
        Menu menu = menu("REWARD_EDITOR", "", "&8Reward Editor", 54);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            int slot = 0;

            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;

                String path = "rewards." + id;

                Material icon = material(
                        rewards.getString(path + ".material"),
                        Material.STONE
                );

                boolean enabled = rewards.getBoolean(path + ".enabled", true);

                put(menu, slot++, id, item(
                        icon,
                        rewards.getString(path + ".name", id),
                        List.of(
                                "&7ID: &f" + id,
                                "&7Category: &f" +
                                        rewards.getString(path + ".category", ""),
                                "&7Price: &b" +
                                        rewards.getLong(path + ".price") + " Gems",
                                "&7Status: " + (enabled ? "&aON" : "&cOFF"),
                                "",
                                "&eClick to manage!"
                        )
                ));
            }
        }

        put(menu, 49, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    private void openRewardActions(Player player, String id) {
        String path = "rewards." + id;

        if (!rewards.contains(path)) return;

        Menu menu = menu(
                "REWARD_ACTIONS", id,
                "&8Reward: " + id, 27
        );

        put(menu, 10, "toggle", item(
                Material.LEVER, "&eToggle Reward",
                List.of(
                        "&7Current: " +
                                (rewards.getBoolean(path + ".enabled", true)
                                        ? "&aON" : "&cOFF"),
                        "", "&eClick to toggle!"
                )
        ));

        put(menu, 12, "price", item(
                Material.EMERALD, "&bChange Price",
                List.of("&7Current: &b" +
                        rewards.getLong(path + ".price") + " Gems",
                        "", "&eClick and type price in chat.")
        ));

        put(menu, 14, "rename", item(
                Material.NAME_TAG, "&bChange Name",
                List.of("&7Click and type new name in chat.")
        ));

        put(menu, 16, "delete", item(
                Material.BARRIER, "&cDelete Reward",
                List.of("&7Permanently delete this reward.")
        ));

        put(menu, 22, "back", item(
                Material.ARROW, "&cBack", List.of()
        ));

        player.openInventory(menu.inventory);
    }

    // =========================================================
    // CHAT EDITOR
    // =========================================================

    private void requestInput(Player player, String type) {
        pendingInput.put(player.getUniqueId(), type);
        player.closeInventory();

        player.sendMessage(color(
                prefix() + "&eType your input in chat. Type &ccancel &eto cancel."
        ));
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String type = pendingInput.get(player.getUniqueId());

        if (type == null) return;

        event.setCancelled(true);

        String input = event.getMessage();

        Bukkit.getScheduler().runTask(this, () -> {
            if (!pendingInput.containsKey(player.getUniqueId())) return;

            pendingInput.remove(player.getUniqueId());

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(color(prefix() + "&cInput cancelled."));
                return;
            }

            String[] parts = input.split("\\|", -1);

            switch (type) {
                case "create-category" -> {
                    if (parts.length < 3) {
                        player.sendMessage(color(
                                "&cFormat: id|name|material"
                        ));
                        return;
                    }

                    String id = safeId(parts[0]);

                    Material icon = Material.matchMaterial(parts[2]);

                    if (id.isEmpty() || icon == null
                            || categories.contains("categories." + id)) {
                        player.sendMessage(color("&cInvalid category."));
                        return;
                    }

                    String path = "categories." + id;

                    categories.set(path + ".name", parts[1]);
                    categories.set(path + ".material", icon.name());
                    categories.set(path + ".slot", firstFreeCategorySlot());
                    categories.set(path + ".enabled", true);
                    categories.set(path + ".lore",
                            List.of("&7Custom category", "", "&eClick to open!"));

                    saveCategories();

                    player.sendMessage(color(prefix() + "&aCategory created!"));
                    openCategoryEditor(player);
                }

                case "create-reward" -> {
                    if (parts.length < 4) {
                        player.sendMessage(color(
                                "&cFormat: id|category|price|amount"
                        ));
                        return;
                    }

                    String id = safeId(parts[0]);
                    String category = safeId(parts[1]);

                    long price;
                    int amount;

                    try {
                        price = Long.parseLong(parts[2]);
                        amount = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException exception) {
                        player.sendMessage(color("&cInvalid price or amount."));
                        return;
                    }

                    ItemStack hand = player.getInventory().getItemInMainHand();

                    if (id.isEmpty() || rewards.contains("rewards." + id)
                            || !categories.contains("categories." + category)
                            || hand.getType().isAir()
                            || price <= 0 || amount < 1 || amount > 64) {
                        player.sendMessage(color("&cInvalid reward data."));
                        return;
                    }

                    String path = "rewards." + id;

                    rewards.set(path + ".category", category);
                    rewards.set(path + ".name",
                            hand.hasItemMeta()
                                    && hand.getItemMeta().hasDisplayName()
                                    ? hand.getItemMeta().getDisplayName()
                                    : id
                    );
                    rewards.set(path + ".material", hand.getType().name());
                    rewards.set(path + ".slot", firstFreeRewardSlot(category));
                    rewards.set(path + ".amount", amount);
                    rewards.set(path + ".price", price);
                    rewards.set(path + ".enabled", true);
                    rewards.set(path + ".reward-type", "ITEM");
                    rewards.set(path + ".lore", List.of(
                            "&7Custom reward",
                            "",
                            "&7Price: &b" + price + " Gems"
                    ));
                    rewards.set(path + ".commands", Collections.emptyList());

                    saveRewards();

                    player.sendMessage(color(prefix() + "&aReward created!"));
                    openRewardEditor(player);
                }

                case "rename-category" -> {
                    String path = "categories." + typeValue(type);

                    player.sendMessage(color("&cInvalid editor state."));
                }

                case "category-name" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !categories.contains("categories." + id)) return;

                    categories.set("categories." + id + ".name", input);
                    saveCategories();
                    pendingContext.remove(player.getUniqueId());

                    player.sendMessage(color(prefix() + "&aCategory renamed!"));
                    openCategoryActions(player, id);
                }

                case "reward-name" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !rewards.contains("rewards." + id)) return;

                    rewards.set("rewards." + id + ".name", input);
                    saveRewards();
                    pendingContext.remove(player.getUniqueId());

                    player.sendMessage(color(prefix() + "&aReward renamed!"));
                    openRewardActions(player, id);
                }

                case "reward-price" -> {
                    String id = pendingContext.get(player.getUniqueId());
                    if (id == null || !rewards.contains("rewards." + id)) return;

                    try {
                        long price = Long.parseLong(input);

                        if (price <= 0) {
                            player.sendMessage(color("&cPrice must be positive."));
                            return;
                        }

                        rewards.set("rewards." + id + ".price", price);
                        saveRewards();
                        pendingContext.remove(player.getUniqueId());

                        player.sendMessage(color(prefix() + "&aPrice updated!"));
                        openRewardActions(player, id);

                    } catch (NumberFormatException exception) {
                        player.sendMessage(color("&cInvalid price."));
                    }
                }
            }
        });
    }

    private final Map<UUID, String> pendingContext = new HashMap<>();

    private String typeValue(String value) {
        return value;
    }

    private String safeId(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
    }

    private int firstFreeCategorySlot() {
        boolean[] used = new boolean[54];

        ConfigurationSection section =
                categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                int slot = categories.getInt("categories." + id + ".slot", -1);
                if (slot >= 0 && slot < 54) used[slot] = true;
            }
        }

        for (int i = 0; i < 54; i++) {
            if (!used[i]) return i;
        }

        return 0;
    }

    private int firstFreeRewardSlot(String category) {
        boolean[] used = new boolean[54];

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;

                if (!rewards.getString(path + ".category", "")
                        .equalsIgnoreCase(category)) continue;

                int slot = rewards.getInt(path + ".slot", -1);
                if (slot >= 0 && slot < 54) used[slot] = true;
            }
        }

        for (int i = 0; i < 54; i++) {
            if (!used[i]) return i;
        }

        return 0;
    }

    // =========================================================
    // CATEGORY / REWARD MANAGEMENT
    // =========================================================

    private void deleteCategory(String id) {
        categories.set("categories." + id, null);

        ConfigurationSection section =
                rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String rewardId : new ArrayList<>(section.getKeys(false))) {
                if (rewards.getString(
                        "rewards." + rewardId + ".category", ""
                ).equalsIgnoreCase(id)) {
                    rewards.set("rewards." + rewardId, null);
                }
            }
        }

        saveCategories();
        saveRewards();
    }

    private void toggleCategory(String id) {
        String path = "categories." + id;
        if (!categories.contains(path)) return;

        categories.set(path + ".enabled",
                !categories.getBoolean(path + ".enabled", true));

        saveCategories();
    }

    private void toggleReward(String id) {
        String path = "rewards." + id;
        if (!rewards.contains(path)) return;

        rewards.set(path + ".enabled",
                !rewards.getBoolean(path + ".enabled", true));

        saveRewards();
    }

    private void setCategoryIcon(Player player, String id) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage(color("&cHold an item first."));
            return;
        }

        categories.set(
                "categories." + id + ".material",
                hand.getType().name()
        );

        saveCategories();
        player.sendMessage(color(prefix() + "&aCategory icon updated!"));
    }

    // =========================================================
    // ECONOMY
    // =========================================================

    private void buy(Player player, int amount) {
        if (!getConfig().getBoolean("economy.enabled", true)
                || !getConfig().getBoolean("economy.buy-enabled", true)) {
            player.sendMessage(color("&cGems purchase is disabled."));
            return;
        }

        int minimum = getConfig().getInt("economy.min-purchase", 1);
        int maximum = getConfig().getInt("economy.max-purchase", 10000);
        long price = getConfig().getLong("economy.price-per-gem", 1500);

        if (amount < minimum || amount > maximum || price <= 0
                || amount > Long.MAX_VALUE / price) {
            send(player, "invalid-amount");
            return;
        }

        long total = amount * price;

        long now = System.currentTimeMillis();

        if (getConfig().getBoolean(
                "security.prevent-duplicate-purchase", true
        )) {
            if (now - lastTransaction.getOrDefault(
                    player.getUniqueId(), 0L
            ) < 1000) return;
        }

        if (!economy.has(player, total)) {
            send(player, "not-enough-money");
            return;
        }

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
            player.sendMessage(color(prefix()
                    + "&cTransaction failed. Money refunded."));
            return;
        }

        lastTransaction.put(player.getUniqueId(), now);

        send(player, "purchase-success",
                "%amount%", String.valueOf(amount));
    }

    private boolean canFit(Player player, ItemStack item) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = item.getAmount();

        for (ItemStack content : contents) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(
                        0, content.getMaxStackSize() - content.getAmount()
                );
            }

            if (remaining <= 0) return true;
        }

        return false;
    }

    private void exchange(Player player, String id) {
        String path = "rewards." + id;

        if (!rewards.contains(path)
                || !rewards.getBoolean(path + ".enabled", true)) return;

        long price = rewards.getLong(path + ".price");

        if (price <= 0 || balance(player.getUniqueId()) < price) {
            send(player, "not-enough-gems");
            return;
        }

        String type = rewards.getString(path + ".reward-type", "ITEM");

        Material material = Material.matchMaterial(
                rewards.getString(path + ".material", "STONE")
        );

        int amount = rewards.getInt(path + ".amount", 1);

        ItemStack rewardItem = null;

        if (type.equalsIgnoreCase("ITEM")) {
            if (material == null || material.isAir()
                    || amount < 1 || amount > 64) {
                player.sendMessage(color("&cInvalid reward item."));
                return;
            }

            rewardItem = new ItemStack(material, amount);

            if (!canFit(player, rewardItem)) {
                send(player, "inventory-full");
                return;
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            List<String> commands = rewards.getStringList(path + ".commands");

            if (commands.isEmpty()) {
                player.sendMessage(color("&cReward commands are empty."));
                return;
            }
        } else {
            player.sendMessage(color("&cUnknown reward type."));
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

        boolean success = true;

        if (type.equalsIgnoreCase("ITEM")) {
            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(rewardItem);

            if (!leftovers.isEmpty()) {
                success = false;

                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(
                            player.getLocation(), leftover
                    );
                }
            }
        } else {
            for (String command : rewards.getStringList(path + ".commands")) {
                String parsed = command.replace(
                        "%player%", player.getName()
                );

                boolean dispatched = Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(), parsed
                );

                if (!dispatched) {
                    getLogger().warning(
                            "Reward command was not handled: " + parsed
                    );
                    success = false;
                    break;
                }
            }
        }

        if (!success) {
            addBalance(
                    player.getUniqueId(),
                    player.getName(),
                    price,
                    "REFUND"
            );

            player.sendMessage(color(prefix()
                    + "&cReward execution failed. Gems refunded."));
            return;
        }

        send(player, "exchange-success",
                "%reward%", rewards.getString(path + ".name", id));

        logTransaction(
                player.getUniqueId(), "REWARD", price, id
        );

        if (getConfig().getBoolean("shop.close-after-purchase", false)) {
            player.closeInventory();
        }
    }

    // =========================================================
    // LEADERBOARD
    // =========================================================

    private void showTop(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&l       GEMS LEADERBOARD"));
        player.sendMessage(color("&8&m------------------------------"));

        int limit = Math.max(1, getConfig().getInt(
                "leaderboard.max-entries", 10
        ));

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username, balance FROM gems ORDER BY balance DESC LIMIT ?"
        )) {
            statement.setInt(1, limit);

            try (ResultSet result = statement.executeQuery()) {
                int rank = 1;

                while (result.next()) {
                    player.sendMessage(color(
                            "&e#" + rank + " &f" +
                                    result.getString("username") +
                                    " &8- &b" +
                                    result.getLong("balance") + " Gems"
                    ));

                    rank++;
                }
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        player.sendMessage(color("&8&m------------------------------"));
    }

    private long topBalance(int position) {
        if (position < 1) return 0;

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

    private String topName(int position) {
        if (position < 1) return "None";

        try (PreparedStatement statement = database.prepareStatement(
                "SELECT username FROM gems ORDER BY balance DESC LIMIT 1 OFFSET ?"
        )) {
            statement.setInt(1, position - 1);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getString("username");
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return "None";
    }

    private int rank(UUID uuid) {
        try (PreparedStatement statement = database.prepareStatement("""
            SELECT COUNT(*) + 1 AS rank
            FROM gems
            WHERE balance > (SELECT balance FROM gems WHERE uuid = ?)
        """)) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt("rank");
            }
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    private long totalPlayers() {
        try (Statement statement = database.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM gems"
             )) {
            if (result.next()) return result.getLong(1);
        } catch (SQLException exception) {
            getLogger().warning(exception.getMessage());
        }

        return 0;
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    private boolean hasPermission(Player player, String permission) {
        return admin(player, permission);
    }

    private void help(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&lVortexGems Commands"));
        player.sendMessage(color("&f/gems"));
        player.sendMessage(color("&f/gems balance"));
        player.sendMessage(color("&f/gems shop"));
        player.sendMessage(color("&f/gems buy <amount>"));
        player.sendMessage(color("&f/gems pay <player> <amount>"));
        player.sendMessage(color("&f/gems top"));
        player.sendMessage(color("&f/gems give <player> <amount>"));
        player.sendMessage(color("&f/gems take <player> <amount>"));
        player.sendMessage(color("&f/gems set <player> <amount>"));
        player.sendMessage(color("&f/gems check <player>"));
        player.sendMessage(color("&f/gems reload"));
        player.sendMessage(color("&8&m------------------------------"));
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            if (command.getName().equalsIgnoreCase("gems")
                    && args.length >= 3) {
                return consoleAdmin(sender, args);
            }

            sender.sendMessage("This command requires a player.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("gemsadmin")) {
            return adminCommand(player, args);
        }

        if (!player.hasPermission("vortexgems.use")) {
            send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "balance" -> player.sendMessage(color(
                    prefix() + "&7Your balance: &b" +
                            balance(player.getUniqueId()) + " Gems"
            ));

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

            case "pay" -> {
                if (!player.hasPermission("vortexgems.pay")
                        || args.length < 3) {
                    send(player, "no-permission");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null || target.equals(player)) {
                    send(player, "player-not-found");
                    return true;
                }

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount <= 0 || amount > getConfig().getLong(
                            "settings.max-balance", 1000000000L
                    )) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    synchronized (database) {
                        if (!removeBalance(
                                player.getUniqueId(),
                                player.getName(),
                                amount,
                                "PLAYER_PAY"
                        )) {
                            send(player, "not-enough-gems");
                            return true;
                        }

                        if (!addBalance(
                                target.getUniqueId(),
                                target.getName(),
                                amount,
                                "PLAYER_RECEIVE"
                        )) {
                            addBalance(
                                    player.getUniqueId(),
                                    player.getName(),
                                    amount,
                                    "PAY_REFUND"
                            );
                            send(player, "invalid-amount");
                            return true;
                        }
                    }

                    player.sendMessage(color(prefix()
                            + "&aSent &b" + amount + " Gems &ato "
                            + target.getName()));

                    target.sendMessage(color(prefix()
                            + "&aReceived &b" + amount + " Gems &afrom "
                            + player.getName()));

                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "give", "take", "set", "check" -> {
                if (args.length < 3) {
                    help(player);
                    return true;
                }

                String permission = switch (sub) {
                    case "give" -> "vortexgems.give";
                    case "take" -> "vortexgems.take";
                    case "set" -> "vortexgems.set";
                    default -> "vortexgems.check";
                };

                if (!hasPermission(player, permission)) {
                    send(player, "no-permission");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

                try {
                    long amount = Long.parseLong(args[2]);

                    if (amount < 0) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    String name = target.getName() == null
                            ? args[1] : target.getName();

                    switch (sub) {
                        case "give" -> {
                            if (addBalance(
                                    target.getUniqueId(), name,
                                    amount, "ADMIN_GIVE"
                            )) {
                                send(player, "give-success",
                                        "%amount%", String.valueOf(amount),
                                        "%player%", name);
                            } else send(player, "invalid-amount");
                        }

                        case "take" -> {
                            if (removeBalance(
                                    target.getUniqueId(), name,
                                    amount, "ADMIN_TAKE"
                            )) {
                                send(player, "take-success",
                                        "%amount%", String.valueOf(amount),
                                        "%player%", name);
                            } else send(player, "not-enough-gems");
                        }

                        case "set" -> {
                            if (setBalance(target.getUniqueId(), name, amount)) {
                                logTransaction(
                                        target.getUniqueId(),
                                        "ADMIN_SET", amount, "ADMIN_SET"
                                );
                                send(player, "set-success",
                                        "%player%", name);
                            } else send(player, "invalid-amount");
                        }

                        case "check" -> player.sendMessage(color(
                                prefix() + "&e" + name + " &7has &b" +
                                        balance(target.getUniqueId()) + " Gems"
                        ));
                    }
                } catch (NumberFormatException exception) {
                    send(player, "invalid-amount");
                }
            }

            case "reload" -> {
                if (!hasPermission(player, "vortexgems.reload")) {
                    send(player, "no-permission");
                    return true;
                }

                loadFiles();
                send(player, "reload-success");
            }

            case "help" -> help(player);

            default -> help(player);
        }

        return true;
    }

    private boolean consoleAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /gems <give|take|set|check> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        try {
            long amount = Long.parseLong(args[2]);

            if (amount < 0) {
                sender.sendMessage("Invalid amount.");
                return true;
            }

            String name = target.getName() == null ? args[1] : target.getName();

            boolean success = switch (action) {
                case "give" -> addBalance(
                        target.getUniqueId(), name, amount, "CONSOLE_GIVE"
                );
                case "take" -> removeBalance(
                        target.getUniqueId(), name, amount, "CONSOLE_TAKE"
                );
                case "set" -> setBalance(
                        target.getUniqueId(), name, amount
                );
                case "check" -> {
                    sender.sendMessage(name + " has "
                            + balance(target.getUniqueId()) + " Gems.");
                    yield true;
                }
                default -> false;
            };

            sender.sendMessage(success
                    ? "VortexGems: Operation successful."
                    : "VortexGems: Operation failed.");

        } catch (NumberFormatException exception) {
            sender.sendMessage("Invalid amount.");
        }

        return true;
    }

    private boolean adminCommand(Player player, String[] args) {
        if (!admin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            openAdmin(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(color("&b/gemsadmin"));
            player.sendMessage(color("&b/gemsadmin category create"));
            player.sendMessage(color("&b/gemsadmin category delete <id>"));
            player.sendMessage(color("&b/gemsadmin category toggle <id>"));
            player.sendMessage(color("&b/gemsadmin reward addhand"));
            player.sendMessage(color("&b/gemsadmin reward delete <id>"));
            player.sendMessage(color("&b/gemsadmin reward toggle <id>"));
            player.sendMessage(color("&b/gemsadmin reward price <id> <price>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("category") && args.length >= 3) {
            String action = args[1].toLowerCase(Locale.ROOT);
            String id = safeId(args[2]);

            switch (action) {
                case "create" -> requestInput(player, "create-category");

                case "delete" -> {
                    if (!categories.contains("categories." + id)) {
                        player.sendMessage(color("&cCategory not found."));
                        return true;
                    }

                    deleteCategory(id);
                    player.sendMessage(color(prefix()
                            + "&aCategory and its rewards deleted."));
                }

                case "toggle" -> {
                    toggleCategory(id);
                    player.sendMessage(color(prefix() + "&aCategory updated."));
                }
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reward")) {
            if (args.length >= 2) {
                String action = args[1].toLowerCase(Locale.ROOT);

                switch (action) {
                    case "addhand" -> requestInput(player, "create-reward");

                    case "delete" -> {
                        if (args.length < 3) return true;

                        rewards.set("rewards." + safeId(args[2]), null);
                        saveRewards();

                        player.sendMessage(color(prefix() + "&aReward deleted."));
                    }

                    case "toggle" -> {
                        if (args.length < 3) return true;

                        toggleReward(safeId(args[2]));
                        player.sendMessage(color(prefix() + "&aReward updated."));
                    }

                    case "price" -> {
                        if (args.length < 4) return true;

                        String id = safeId(args[2]);

                        try {
                            long price = Long.parseLong(args[3]);

                            if (price <= 0 || !rewards.contains("rewards." + id)) {
                                send(player, "invalid-amount");
                                return true;
                            }

                            rewards.set("rewards." + id + ".price", price);
                            saveRewards();

                            player.sendMessage(color(prefix() + "&aPrice updated."));
                        } catch (NumberFormatException exception) {
                            send(player, "invalid-amount");
                        }
                    }
                }
            }

            return true;
        }

        openAdmin(player);
        return true;
    }

    // =========================================================
    // INVENTORY EVENTS
    // =========================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof Menu menu)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != top) return;

        int slot = event.getRawSlot();

        String id = menu.entries.get(slot);

        if (id == null) return;

        switch (menu.type) {
            case "MAIN" -> {
                switch (id) {
                    case "buy" -> openBuy(player);
                    case "shop" -> openCategories(player);
                    case "balance" -> player.sendMessage(color(
                            prefix() + "&7Your balance: &b" +
                                    balance(player.getUniqueId()) + " Gems"
                    ));
                    case "top" -> showTop(player);
                }
            }

            case "BUY" -> {
                if (id.equals("purchase")) {
                    int amount = menu.amounts.getOrDefault(slot, 0);

                    if (amount > 0) {
                        buy(player, amount);
                        openBuy(player);
                    }
                }
            }

            case "CATEGORIES" -> openRewards(player, id);

            case "REWARDS" -> exchange(player, id);

            case "ADMIN" -> {
                switch (id) {
                    case "categories" -> openCategoryEditor(player);
                    case "rewards" -> openRewardEditor(player);
                    case "create-category" -> requestInput(
                            player, "create-category"
                    );
                    case "create-reward" -> requestInput(
                            player, "create-reward"
                    );
                    case "reload" -> {
                        loadFiles();
                        send(player, "reload-success");
                        openAdmin(player);
                    }
                }
            }

            case "CATEGORY_EDITOR" -> {
                if (id.equals("back")) {
                    openAdmin(player);
                } else {
                    openCategoryActions(player, id);
                }
            }

            case "CATEGORY_ACTIONS" -> {
                String categoryId = menu.context;

                switch (id) {
                    case "toggle" -> {
                        toggleCategory(categoryId);
                        openCategoryActions(player, categoryId);
                    }

                    case "rename" -> {
                        pendingContext.put(player.getUniqueId(), categoryId);
                        requestInput(player, "category-name");
                    }

                    case "icon" -> {
                        setCategoryIcon(player, categoryId);
                        openCategoryActions(player, categoryId);
                    }

                    case "delete" -> {
                        deleteCategory(categoryId);
                        player.sendMessage(color(prefix()
                                + "&aCategory deleted."));
                        openCategoryEditor(player);
                    }

                    case "back" -> openCategoryEditor(player);
                }
            }

            case "REWARD_EDITOR" -> {
                if (id.equals("back")) {
                    openAdmin(player);
                } else {
                    openRewardActions(player, id);
                }
            }

            case "REWARD_ACTIONS" -> {
                String rewardId = menu.context;

                switch (id) {
                    case "toggle" -> {
                        toggleReward(rewardId);
                        openRewardActions(player, rewardId);
                    }

                    case "price" -> {
                        pendingContext.put(player.getUniqueId(), rewardId);
                        requestInput(player, "reward-price");
                    }

                    case "rename" -> {
                        pendingContext.put(player.getUniqueId(), rewardId);
                        requestInput(player, "reward-name");
                    }

                    case "delete" -> {
                        rewards.set("rewards." + rewardId, null);
                        saveRewards();

                        player.sendMessage(color(prefix()
                                + "&aReward deleted."));
                        openRewardEditor(player);
                    }

                    case "back" -> openRewardEditor(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ensurePlayer(player.getUniqueId(), player.getName());
    }

    // =========================================================
    // PLACEHOLDER API
    // =========================================================

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
                return String.valueOf(plugin.totalPlayers());
            }

            if (params.equalsIgnoreCase("top_1_name")) {
                return plugin.topName(1);
            }

            if (params.matches("top_[1-9][0-9]*_name")) {
                int rank = Integer.parseInt(
                        params.substring(4, params.length() - 5)
                );
                return plugin.topName(rank);
            }

            if (params.matches("top_[1-9][0-9]*_balance")) {
                int rank = Integer.parseInt(
                        params.substring(4, params.length() - 8)
                );
                return String.valueOf(plugin.topBalance(rank));
            }

            if (player == null) return "0";

            if (params.equalsIgnoreCase("balance")
                    || params.equalsIgnoreCase("formatted")) {
                return String.valueOf(
                        plugin.balance(player.getUniqueId())
                );
            }

            if (params.equalsIgnoreCase("rank")) {
                return String.valueOf(
                        plugin.rank(player.getUniqueId())
                );
            }

            return null;
        }
    }
}
