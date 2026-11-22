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
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VortexGems extends JavaPlugin implements Listener, CommandExecutor {

    private Economy economy;
    private Connection database;

    private File categoriesFile, rewardsFile, messagesFile;
    private YamlConfiguration categories, rewards, messages;

    private final Map<UUID, String> pendingInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingContext = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTransaction = new ConcurrentHashMap<>();

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

        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        saveIfMissing("categories.yml");
        saveIfMissing("rewards.yml");
        saveIfMissing("messages.yml");

        categoriesFile = new File(getDataFolder(), "categories.yml");
        rewardsFile = new File(getDataFolder(), "rewards.yml");
        messagesFile = new File(getDataFolder(), "messages.yml");

        loadFiles();

        if (!setupEconomy() || !setupDatabase()) {
            getLogger().severe("Vault atau SQLite gagal diinisialisasi.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("gems")).setExecutor(this);
        Objects.requireNonNull(getCommand("gemsadmin")).setExecutor(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GemsExpansion(this).register();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ensurePlayer(player.getUniqueId(), player.getName());
        }

        getLogger().info("VortexGems enabled.");
    }

    @Override
    public void onDisable() {
        pendingInput.clear();
        pendingContext.clear();

        try {
            if (database != null && !database.isClosed()) database.close();
        } catch (SQLException e) {
            getLogger().severe(e.getMessage());
        }
    }

    private void saveIfMissing(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) return false;
        economy = provider.getProvider();
        return economy != null;
    }

    private boolean setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String filename = getConfig().getString("storage.sqlite.file", "data.db");
            File file = new File(getDataFolder(), filename);

            database = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

            try (Statement st = database.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gems (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0
                    )
                """);
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_gems_balance ON gems(balance DESC)");
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("SQLite: " + e.getMessage());
            return false;
        }
    }

    private synchronized void loadFiles() {
        reloadConfig();
        categories = YamlConfiguration.loadConfiguration(categoriesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void saveCategories() {
        try {
            categories.save(categoriesFile);
        } catch (Exception e) {
            getLogger().severe("categories.yml: " + e.getMessage());
        }
    }

    private void saveRewards() {
        try {
            rewards.save(rewardsFile);
        } catch (Exception e) {
            getLogger().severe("rewards.yml: " + e.getMessage());
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String prefix() {
        return color(getConfig().getString("settings.prefix", "&8[&bVortexGems&8] &7"));
    }

    private String msg(String key) {
        return prefix() + color(messages.getString(key,
                getConfig().getString("messages." + key, key)));
    }

    private void send(Player player, String key) {
        player.sendMessage(msg(key));
    }

    private void send(Player player, String key, String... replacements) {
        String text = msg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(text);
    }

    private String format(long value) {
        return new DecimalFormat("#,###").format(value);
    }

    // DATABASE

    private synchronized void ensurePlayer(UUID uuid, String name) {
        try (PreparedStatement ps = database.prepareStatement("""
            INSERT OR IGNORE INTO gems(uuid, username, balance) VALUES(?,?,?)
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, Math.max(0, getConfig().getLong("settings.starting-balance", 0)));
            ps.executeUpdate();
            try (PreparedStatement update = database.prepareStatement(
                    "UPDATE gems SET username=? WHERE uuid=?")) {
                update.setString(1, name);
                update.setString(2, uuid.toString());
                update.executeUpdate();
            }
        } catch (SQLException e) {
            getLogger().severe(e.getMessage());
        }
    }

    private synchronized long balance(UUID uuid) {
        try (PreparedStatement ps = database.prepareStatement(
                "SELECT balance FROM gems WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            getLogger().severe(e.getMessage());
        }
        return 0;
    }

    private synchronized boolean setBalance(UUID uuid, String name, long amount) {
        long max = getConfig().getLong("settings.max-balance", 1_000_000_000L);
        if (amount < 0 || amount > max) return false;

        try (PreparedStatement ps = database.prepareStatement("""
            INSERT INTO gems(uuid,username,balance) VALUES(?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET username=excluded.username,balance=excluded.balance
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, amount);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            getLogger().severe(e.getMessage());
            return false;
        }
    }

    private void logTransaction(UUID uuid, String type, long amount, String description) {
        if (!getConfig().getBoolean("security.log-transactions", true)) return;
        try (PreparedStatement ps = database.prepareStatement("""
            INSERT INTO transactions(uuid,type,amount,description) VALUES(?,?,?,?)
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            ps.setLong(3, amount);
            ps.setString(4, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
    }

    private synchronized boolean addBalance(UUID uuid, String name, long amount, String type) {
        if (amount <= 0) return false;
        long current = balance(uuid);
        long max = getConfig().getLong("settings.max-balance", 1_000_000_000L);
        if (amount > max || current > max - amount) return false;
        if (!setBalance(uuid, name, current + amount)) return false;
        logTransaction(uuid, type, amount, type);
        return true;
    }

    private synchronized boolean removeBalance(UUID uuid, String name, long amount, String type) {
        if (amount <= 0) return false;
        long current = balance(uuid);
        if (current < amount) return false;
        if (!setBalance(uuid, name, current - amount)) return false;
        logTransaction(uuid, type, amount, type);
        return true;
    }

    // GUI HELPERS

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) meta.setLore(lore.stream().map(this::color).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material material(String name, Material fallback) {
        Material mat = Material.matchMaterial(name == null ? "" : name);
        return mat == null ? fallback : mat;
    }

    private Menu createMenu(String type, String context, String title, int size) {
        Menu menu = new Menu(type, context);
        menu.inventory = Bukkit.createInventory(menu, size, color(title));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, "&8 ", List.of());
        for (int i = 0; i < size; i++) menu.inventory.setItem(i, filler);
        return menu;
    }

    private void put(Menu menu, int slot, String id, ItemStack item) {
        if (slot < 0 || slot >= menu.inventory.getSize()) return;
        menu.inventory.setItem(slot, item);
        menu.entries.put(slot, id);
    }

    private boolean isAdmin(Player player, String permission) {
        return (getConfig().getBoolean("admin.op-access", true) && player.isOp())
                || player.hasPermission(permission);
    }

    // PLAYER MENUS

    private void openMain(Player player) {
        Menu menu = createMenu("MAIN", "", "&8✦ &bVortexGems", 27);

        put(menu, 10, "buy", item(Material.EMERALD, "&aBuy Gems",
                List.of("&7Purchase Gems using money.", "", "&eClick to open.")));

        put(menu, 12, "shop", item(Material.CHEST, "&6Exchange Shop",
                List.of("&7Exchange Gems for rewards.", "", "&eClick to open.")));

        put(menu, 14, "balance", item(Material.PLAYER_HEAD, "&bMy Balance",
                List.of("&7Balance: &b" + format(balance(player.getUniqueId())) + " Gems")));

        put(menu, 16, "top", item(Material.GOLD_BLOCK, "&6Leaderboard",
                List.of("&7View the richest players.", "", "&eClick to view.")));

        player.openInventory(menu.inventory);
    }

    private void openBuy(Player player) {
        Menu menu = createMenu("BUY", "", "&8✦ &aBuy Gems", 27);
        List<Integer> options = getConfig().getIntegerList("economy.purchase-options");
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        long price = getConfig().getLong("economy.price-per-gem", 1500);

        for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
            int amount = options.get(i);
            if (amount <= 0 || price <= 0 || amount > Long.MAX_VALUE / price) continue;
            long total = amount * price;
            put(menu, slots[i], "purchase", item(Material.EMERALD,
                    "&a" + format(amount) + " Gems",
                    List.of("&7Price: &e$" + format(total), "", "&eClick to purchase.")));
            menu.amounts.put(slots[i], amount);
        }

        put(menu, 22, "back", item(Material.ARROW, "&cBack", List.of("&7Return to menu.")));
        player.openInventory(menu.inventory);
    }

    private void openCategories(Player player) {
        if (!getConfig().getBoolean("shop.enabled", true)) {
            player.sendMessage(color("&cExchange Shop is disabled."));
            return;
        }

        Menu menu = createMenu("CATEGORIES", "", "&8✦ &bShop Categories", 54);
        ConfigurationSection section = categories.getConfigurationSection("categories");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "categories." + id;
                if (!categories.getBoolean(path + ".enabled", true)) continue;

                put(menu, categories.getInt(path + ".slot", 10), id,
                        item(material(categories.getString(path + ".material"), Material.CHEST),
                                categories.getString(path + ".name", id),
                                categories.getStringList(path + ".lore")));
            }
        }

        put(menu, 49, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
    }

    private void openRewards(Player player, String category) {
        String title = getConfig().getString("menus.rewards.title",
                "&8✦ &bExchange: %category%").replace("%category%", category);

        Menu menu = createMenu("REWARDS", category, title, 54);
        ConfigurationSection section = rewards.getConfigurationSection("rewards");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "rewards." + id;
                if (!rewards.getBoolean(path + ".enabled", true)) continue;
                if (!rewards.getString(path + ".category", "").equalsIgnoreCase(category)) continue;

                int amount = rewards.getInt(path + ".amount", 1);
                long price = rewards.getLong(path + ".price", 1);
                List<String> lore = new ArrayList<>(rewards.getStringList(path + ".lore"));
                lore.add("");
                lore.add("&7Price: &b" + format(price) + " Gems");
                lore.add("&eClick to exchange.");

                ItemStack display = item(
                        material(rewards.getString(path + ".material"), Material.STONE),
                        rewards.getString(path + ".name", id), lore);
                display.setAmount(Math.max(1, Math.min(64, amount)));

                put(menu, rewards.getInt(path + ".slot", 10), id, display);
            }
        }

        put(menu, 49, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
    }

    // ADMIN MENUS

    private void openAdmin(Player player) {
        if (!isAdmin(player, "vortexgems.editor")) {
            send(player, "no-permission");
            return;
        }

        Menu menu = createMenu("ADMIN", "", "&8✦ &cVortexGems Admin", 27);
        put(menu, 10, "categories", item(Material.CHEST, "&bManage Categories",
                List.of("&7Create, edit and delete categories.")));
        put(menu, 12, "rewards", item(Material.EMERALD, "&aManage Rewards",
                List.of("&7Create, edit and delete rewards.")));
        put(menu, 14, "create-category", item(Material.NAME_TAG, "&eCreate Category",
                List.of("&7Create a new category.")));
        put(menu, 16, "create-reward", item(Material.ITEM_FRAME, "&eCreate Item Reward",
                List.of("&7Hold the item in your hand first.")));
        put(menu, 22, "reload", item(Material.REDSTONE, "&cReload Files",
                List.of("&7Reload YAML configuration.")));
        player.openInventory(menu.inventory);
    }

    private void openCategoryEditor(Player player) {
        Menu menu = createMenu("CATEGORY_EDITOR", "", "&8Category Editor", 54);
        ConfigurationSection section = categories.getConfigurationSection("categories");

        if (section != null) {
            int slot = 0;
            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;
                String path = "categories." + id;
                boolean enabled = categories.getBoolean(path + ".enabled", true);
                put(menu, slot++, id, item(
                        material(categories.getString(path + ".material"), Material.CHEST),
                        categories.getString(path + ".name", id),
                        List.of("&7ID: &f" + id,
                                "&7Status: " + (enabled ? "&aEnabled" : "&cDisabled"),
                                "", "&eClick to manage.")));
            }
        }

        put(menu, 49, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
    }

    private void openCategoryActions(Player player, String id) {
        String path = "categories." + id;
        if (!categories.contains(path)) return;

        Menu menu = createMenu("CATEGORY_ACTIONS", id, "&8Category: " + id, 27);
        put(menu, 10, "toggle", item(Material.LEVER, "&eToggle Enabled",
                List.of("&7Current: " + (categories.getBoolean(path + ".enabled", true)
                        ? "&aON" : "&cOFF"))));
        put(menu, 11, "rename", item(Material.NAME_TAG, "&bChange Name",
                List.of("&7Enter the new name in chat.")));
        put(menu, 12, "icon", item(Material.ITEM_FRAME, "&bSet Icon From Hand",
                List.of("&7Hold an item in your hand.")));
        put(menu, 13, "slot", item(Material.COMPASS, "&bChange Slot",
                List.of("&7Enter a slot number from 0 to 53.")));
        put(menu, 15, "delete", item(Material.BARRIER, "&cDelete Category",
                List.of("&7Also deletes rewards in this category.")));
        put(menu, 22, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
    }

    private void openRewardEditor(Player player) {
        Menu menu = createMenu("REWARD_EDITOR", "", "&8Reward Editor", 54);
        ConfigurationSection section = rewards.getConfigurationSection("rewards");

        if (section != null) {
            int slot = 0;
            for (String id : section.getKeys(false)) {
                if (slot >= 45) break;
                String path = "rewards." + id;
                boolean enabled = rewards.getBoolean(path + ".enabled", true);
                put(menu, slot++, id, item(
                        material(rewards.getString(path + ".material"), Material.STONE),
                        rewards.getString(path + ".name", id),
                        List.of("&7ID: &f" + id,
                                "&7Category: &f" + rewards.getString(path + ".category", ""),
                                "&7Price: &b" + format(rewards.getLong(path + ".price")) + " Gems",
                                "&7Status: " + (enabled ? "&aEnabled" : "&cDisabled"),
                                "", "&eClick to manage.")));
            }
        }

        put(menu, 49, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
    }

    private void openRewardActions(Player player, String id) {
        String path = "rewards." + id;
        if (!rewards.contains(path)) return;

        Menu menu = createMenu("REWARD_ACTIONS", id, "&8Reward: " + id, 27);
        put(menu, 10, "toggle", item(Material.LEVER, "&eToggle Enabled",
                List.of("&7Current: " + (rewards.getBoolean(path + ".enabled", true)
                        ? "&aON" : "&cOFF"))));
        put(menu, 11, "name", item(Material.NAME_TAG, "&bChange Name",
                List.of("&7Enter a new name in chat.")));
        put(menu, 12, "price", item(Material.EMERALD, "&bChange Price",
                List.of("&7Current: &b" + rewards.getLong(path + ".price"),
                        "&7Enter the new price in chat.")));
        put(menu, 13, "amount", item(Material.CHEST, "&bChange Amount",
                List.of("&7Current: &f" + rewards.getInt(path + ".amount"),
                        "&7Enter an amount from 1 to 64.")));
        put(menu, 14, "category", item(Material.BOOK, "&bChange Category",
                List.of("&7Enter an existing category ID in chat.")));
        put(menu, 15, "slot", item(Material.COMPASS, "&bChange Slot",
                List.of("&7Enter a slot from 0 to 53.")));
        put(menu, 16, "item", item(Material.ITEM_FRAME, "&bUpdate Item From Hand",
                List.of("&7Hold the new reward item in your hand.")));
        put(menu, 20, "delete", item(Material.BARRIER, "&cDelete Reward", List.of()));
        put(menu, 22, "back", item(Material.ARROW, "&cBack", List.of()));
        player.openInventory(menu.inventory);
                }
        // CHAT INPUT

    private void requestInput(Player player, String type, String context) {
        pendingInput.put(player.getUniqueId(), type);
        if (context != null) pendingContext.put(player.getUniqueId(), context);
        player.closeInventory();
        player.sendMessage(color(prefix()
                + "&eKetik nilai di chat. Ketik &ccancel &euntuk membatalkan."));
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String type = pendingInput.get(uuid);
        if (type == null) return;

        event.setCancelled(true);
        String input = event.getMessage();

        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline() || !pendingInput.containsKey(uuid)) return;

            pendingInput.remove(uuid);

            if (input.equalsIgnoreCase("cancel")) {
                pendingContext.remove(uuid);
                player.sendMessage(color(prefix() + "&cInput dibatalkan."));
                return;
            }

            String context = pendingContext.get(uuid);

            switch (type) {
                case "create-category" -> createCategoryFromInput(player, input);
                case "create-reward" -> createRewardFromInput(player, input);
                case "category-name" -> {
                    if (context == null || !categories.contains("categories." + context)) return;
                    categories.set("categories." + context + ".name", input);
                    saveCategories();
                    pendingContext.remove(uuid);
                    player.sendMessage(color(prefix() + "&aNama kategori diperbarui."));
                    openCategoryActions(player, context);
                }
                case "category-slot" -> updateNumber(player, input, context,
                        "categories", "slot", 0, 53);
                case "reward-name" -> {
                    if (context == null || !rewards.contains("rewards." + context)) return;
                    rewards.set("rewards." + context + ".name", input);
                    saveRewards();
                    pendingContext.remove(uuid);
                    player.sendMessage(color(prefix() + "&aNama reward diperbarui."));
                    openRewardActions(player, context);
                }
                case "reward-price" -> updateNumber(player, input, context,
                        "rewards", "price", 1, Long.MAX_VALUE);
                case "reward-amount" -> updateNumber(player, input, context,
                        "rewards", "amount", 1, 64);
                case "reward-slot" -> updateNumber(player, input, context,
                        "rewards", "slot", 0, 53);
                case "reward-category" -> {
                    if (context == null || !rewards.contains("rewards." + context)) return;
                    String category = safeId(input);
                    if (!categories.contains("categories." + category)) {
                        player.sendMessage(color("&cKategori tidak ditemukan."));
                        return;
                    }
                    rewards.set("rewards." + context + ".category", category);
                    saveRewards();
                    pendingContext.remove(uuid);
                    player.sendMessage(color(prefix() + "&aKategori reward diperbarui."));
                    openRewardActions(player, context);
                }
            }
        });
    }

    private void updateNumber(Player player, String input, String id,
                              String root, String key, long min, long max) {
        if (id == null || !getYaml(root).contains(root + "." + id)) return;

        try {
            long value = Long.parseLong(input);
            if (value < min || value > max) {
                player.sendMessage(color("&cNilai harus antara " + min + " dan " + max + "."));
                return;
            }

            YamlConfiguration yaml = getYaml(root);
            yaml.set(root + "." + id + "." + key, value);

            if (root.equals("categories")) saveCategories();
            else saveRewards();

            pendingContext.remove(player.getUniqueId());
            player.sendMessage(color(prefix() + "&aPengaturan berhasil diperbarui."));

            if (root.equals("categories")) openCategoryActions(player, id);
            else openRewardActions(player, id);

        } catch (NumberFormatException e) {
            player.sendMessage(color("&cMasukkan angka yang valid."));
        }
    }

    private YamlConfiguration getYaml(String root) {
        return root.equals("categories") ? categories : rewards;
    }

    private String safeId(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
    }

    private int firstFreeSlot(String root, String category) {
        boolean[] used = new boolean[54];
        ConfigurationSection section = getYaml(root).getConfigurationSection(root);

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = root + "." + id;
                if (root.equals("rewards")
                        && !getYaml(root).getString(path + ".category", "")
                        .equalsIgnoreCase(category)) continue;

                int slot = getYaml(root).getInt(path + ".slot", -1);
                if (slot >= 0 && slot < 54) used[slot] = true;
            }
        }

        for (int i = 0; i < 54; i++) if (!used[i]) return i;
        return -1;
    }

    private void createCategoryFromInput(Player player, String input) {
        String[] parts = input.split("\\|", 3);
        if (parts.length < 3) {
            player.sendMessage(color("&cFormat: id|nama|MATERIAL"));
            return;
        }

        String id = safeId(parts[0]);
        Material icon = Material.matchMaterial(parts[2]);

        if (id.isEmpty() || icon == null || icon.isAir()
                || categories.contains("categories." + id)) {
            player.sendMessage(color("&cID atau material kategori tidak valid."));
            return;
        }

        int slot = firstFreeSlot("categories", "");
        if (slot < 0) {
            player.sendMessage(color("&cTidak ada slot kategori kosong."));
            return;
        }

        String path = "categories." + id;
        categories.set(path + ".name", parts[1]);
        categories.set(path + ".material", icon.name());
        categories.set(path + ".slot", slot);
        categories.set(path + ".enabled", true);
        categories.set(path + ".lore",
                List.of("&7Kategori " + parts[1], "", "&eKlik untuk membuka."));

        saveCategories();
        player.sendMessage(color(prefix() + "&aKategori berhasil dibuat: &b" + id));
        openCategoryEditor(player);
    }

    private void createRewardFromInput(Player player, String input) {
        String[] parts = input.split("\\|", 4);
        if (parts.length < 4) {
            player.sendMessage(color("&cFormat: id|category|harga|jumlah"));
            return;
        }

        String id = safeId(parts[0]);
        String category = safeId(parts[1]);

        long price;
        int amount;

        try {
            price = Long.parseLong(parts[2]);
            amount = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cHarga atau jumlah tidak valid."));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (id.isEmpty() || rewards.contains("rewards." + id)
                || !categories.contains("categories." + category)
                || hand.getType().isAir() || price <= 0
                || amount < 1 || amount > 64) {
            player.sendMessage(color("&cData reward tidak valid."));
            return;
        }

        int slot = firstFreeSlot("rewards", category);
        if (slot < 0) {
            player.sendMessage(color("&cTidak ada slot reward kosong."));
            return;
        }

        String path = "rewards." + id;
        ItemStack stored = hand.clone();
        stored.setAmount(amount);

        rewards.set(path + ".category", category);
        rewards.set(path + ".name",
                stored.hasItemMeta() && stored.getItemMeta().hasDisplayName()
                        ? stored.getItemMeta().getDisplayName() : id);
        rewards.set(path + ".material", stored.getType().name());
        rewards.set(path + ".item", stored);
        rewards.set(path + ".slot", slot);
        rewards.set(path + ".amount", amount);
        rewards.set(path + ".price", price);
        rewards.set(path + ".enabled", true);
        rewards.set(path + ".reward-type", "ITEM");
        rewards.set(path + ".lore", List.of("&7Custom reward", "",
                "&7Harga: &b" + price + " Gems"));
        rewards.set(path + ".commands", Collections.emptyList());

        saveRewards();
        player.sendMessage(color(prefix() + "&aReward berhasil dibuat: &b" + id));
        openRewardEditor(player);
    }

    // CATEGORY / REWARD MANAGEMENT

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

    private void deleteCategory(String id) {
        categories.set("categories." + id, null);

        ConfigurationSection section = rewards.getConfigurationSection("rewards");
        if (section != null) {
            for (String rewardId : new ArrayList<>(section.getKeys(false))) {
                if (rewards.getString("rewards." + rewardId + ".category", "")
                        .equalsIgnoreCase(id)) {
                    rewards.set("rewards." + rewardId, null);
                }
            }
        }

        saveCategories();
        saveRewards();
    }

    private void deleteReward(String id) {
        rewards.set("rewards." + id, null);
        saveRewards();
    }

    private void setCategoryIcon(Player player, String id) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(color("&cPegang item terlebih dahulu."));
            return;
        }
        categories.set("categories." + id + ".material", hand.getType().name());
        saveCategories();
        player.sendMessage(color(prefix() + "&aIcon kategori diperbarui."));
    }

    private void setRewardItem(Player player, String id) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(color("&cPegang item terlebih dahulu."));
            return;
        }

        String path = "rewards." + id;
        ItemStack stored = hand.clone();
        stored.setAmount(rewards.getInt(path + ".amount", 1));

        rewards.set(path + ".item", stored);
        rewards.set(path + ".material", stored.getType().name());
        saveRewards();

        player.sendMessage(color(prefix() + "&aItem reward diperbarui."));
    }

    // ECONOMY

    private boolean cooldown(Player player) {
        long now = System.currentTimeMillis();
        long wait = getConfig().getLong("security.transaction-cooldown-ms", 1000);
        long last = lastTransaction.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < wait) {
            player.sendMessage(color(prefix() + "&cTunggu sebentar sebelum transaksi berikutnya."));
            return true;
        }

        lastTransaction.put(player.getUniqueId(), now);
        return false;
    }

    private void buy(Player player, int amount) {
        if (!getConfig().getBoolean("economy.enabled", true)
                || !getConfig().getBoolean("economy.buy-enabled", true)) {
            player.sendMessage(color("&cPembelian Gems sedang dinonaktifkan."));
            return;
        }

        int min = getConfig().getInt("economy.min-purchase", 1);
        int max = getConfig().getInt("economy.max-purchase", 10000);
        long price = getConfig().getLong("economy.price-per-gem", 1500);

        if (amount < min || amount > max || price <= 0
                || amount > Long.MAX_VALUE / price) {
            send(player, "invalid-amount");
            return;
        }

        if (cooldown(player)) return;

        long total = amount * price;
        if (!economy.has(player, total)) {
            send(player, "not-enough-money");
            return;
        }

        if (!economy.withdrawPlayer(player, total).transactionSuccess()) {
            send(player, "not-enough-money");
            return;
        }

        if (!addBalance(player.getUniqueId(), player.getName(), amount, "PURCHASE")) {
            economy.depositPlayer(player, total);
            player.sendMessage(color(prefix() + "&cTransaksi gagal. Uang dikembalikan."));
            return;
        }

        send(player, "purchase-success", "%amount%", format(amount));
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(0,
                        content.getMaxStackSize() - content.getAmount());
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
        ItemStack rewardItem = null;

        if (type.equalsIgnoreCase("ITEM")) {
            rewardItem = rewards.getItemStack(path + ".item");

            if (rewardItem == null) {
                Material mat = Material.matchMaterial(
                        rewards.getString(path + ".material", "STONE"));
                int amount = rewards.getInt(path + ".amount", 1);

                if (mat == null || mat.isAir() || amount < 1 || amount > 64) {
                    player.sendMessage(color("&cItem reward tidak valid."));
                    return;
                }

                rewardItem = new ItemStack(mat, amount);
            } else {
                rewardItem = rewardItem.clone();
                rewardItem.setAmount(rewards.getInt(path + ".amount",
                        rewardItem.getAmount()));
            }

            if (!canFit(player, rewardItem)) {
                player.sendMessage(color(prefix() + "&cInventory kamu penuh."));
                return;
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            if (rewards.getStringList(path + ".commands").isEmpty()) {
                player.sendMessage(color("&cCommand reward belum diatur."));
                return;
            }
        } else {
            player.sendMessage(color("&cTipe reward tidak dikenal."));
            return;
        }

        synchronized (database) {
            if (!removeBalance(player.getUniqueId(), player.getName(), price, "EXCHANGE")) {
                send(player, "not-enough-gems");
                return;
            }
        }

        boolean success = true;

        if (type.equalsIgnoreCase("ITEM")) {
            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(rewardItem);

            if (!leftovers.isEmpty()) {
                // Jangan menghilangkan item bila inventory berubah saat transaksi.
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        } else {
            for (String command : rewards.getStringList(path + ".commands")) {
                String parsed = command.replace("%player%", player.getName());
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                } catch (Exception e) {
                    success = false;
                    getLogger().warning("Reward command gagal: " + parsed);
                    break;
                }
            }
        }

        if (!success) {
            addBalance(player.getUniqueId(), player.getName(), price, "REFUND");
            player.sendMessage(color(prefix() + "&cReward gagal. Gems dikembalikan."));
            return;
        }

        logTransaction(player.getUniqueId(), "REWARD", price, id);
        send(player, "exchange-success", "%reward%",
                rewards.getString(path + ".name", id));

        if (getConfig().getBoolean("shop.close-after-purchase", false)) {
            player.closeInventory();
        }
    }

    // LEADERBOARD

    private void showTop(Player player) {
        player.sendMessage(color("&8&m------------------------------"));
        player.sendMessage(color("&b&l       GEMS LEADERBOARD"));
        player.sendMessage(color("&8&m------------------------------"));

        int limit = Math.max(1, getConfig().getInt("leaderboard.max-entries", 10));

        try (PreparedStatement ps = database.prepareStatement(
                "SELECT username,balance FROM gems ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    player.sendMessage(color("&e#" + rank + " &f"
                            + rs.getString("username") + " &8- &b"
                            + format(rs.getLong("balance")) + " Gems"));
                    rank++;
                }
            }
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }

        player.sendMessage(color("&8&m------------------------------"));
    }

    private long topBalance(int position) {
        if (position < 1) return 0;
        try (PreparedStatement ps = database.prepareStatement(
                "SELECT balance FROM gems ORDER BY balance DESC LIMIT 1 OFFSET ?")) {
            ps.setInt(1, position - 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
        return 0;
    }

    private String topName(int position) {
        if (position < 1) return "None";
        try (PreparedStatement ps = database.prepareStatement(
                "SELECT username FROM gems ORDER BY balance DESC LIMIT 1 OFFSET ?")) {
            ps.setInt(1, position - 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
        return "None";
    }

    private int rank(UUID uuid) {
        try (PreparedStatement ps = database.prepareStatement("""
            SELECT COUNT(*) + 1 FROM gems
            WHERE balance > (SELECT balance FROM gems WHERE uuid=?)
        """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
        return 0;
    }

    private long totalPlayers() {
        try (Statement st = database.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM gems")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
        return 0;
    }

    // COMMANDS

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
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return consoleCommand(sender, args);
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
            case "balance" -> player.sendMessage(color(prefix()
                    + "&7Saldo kamu: &b" + format(balance(player.getUniqueId())) + " Gems"));
            case "shop" -> openCategories(player);
            case "top" -> showTop(player);
            case "buy" -> {
                if (args.length < 2) {
                    openBuy(player);
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (getConfig().getBoolean("economy.confirm-purchase", true)) {
                        openConfirmation(player, "BUY", String.valueOf(amount),
                                "&aConfirm purchase", "&7Buy &b" + amount + " Gems?");
                    } else buy(player, amount);
                } catch (NumberFormatException e) {
                    send(player, "invalid-amount");
                }
            }
            case "pay" -> {
                if (!player.hasPermission("vortexgems.pay") || args.length < 3) {
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
                    if (amount <= 0) {
                        send(player, "invalid-amount");
                        return true;
                    }
                    synchronized (database) {
                        if (balance(player.getUniqueId()) < amount) {
                            send(player, "not-enough-gems");
                            return true;
                        }
                        if (balance(target.getUniqueId()) + amount
                                > getConfig().getLong("settings.max-balance", 1_000_000_000L)) {
                            player.sendMessage(color("&cSaldo penerima sudah mencapai batas."));
                            return true;
                        }
                        if (!removeBalance(player.getUniqueId(), player.getName(),
                                amount, "PLAYER_PAY")) return true;
                        if (!addBalance(target.getUniqueId(), target.getName(),
                                amount, "PLAYER_RECEIVE")) {
                            addBalance(player.getUniqueId(), player.getName(),
                                    amount, "PAY_REFUND");
                            return true;
                        }
                    }
                    player.sendMessage(color(prefix() + "&aMengirim &b"
                            + format(amount) + " Gems &ake " + target.getName()));
                    target.sendMessage(color(prefix() + "&aMenerima &b"
                            + format(amount) + " Gems &adari " + player.getName()));
                } catch (NumberFormatException e) {
                    send(player, "invalid-amount");
                }
            }
            case "give", "take", "set", "check" -> {
                if (args.length < 3) {
                    help(player);
                    return true;
                }

                String permission = "vortexgems." + sub;
                if (!isAdmin(player, permission)) {
                    send(player, "no-permission");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String name = target.getName() == null ? args[1] : target.getName();

                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount < 0) {
                        send(player, "invalid-amount");
                        return true;
                    }

                    switch (sub) {
                        case "give" -> {
                            if (addBalance(target.getUniqueId(), name, amount, "ADMIN_GIVE"))
                                send(player, "give-success", "%amount%", format(amount),
                                        "%player%", name);
                            else send(player, "invalid-amount");
                        }
                        case "take" -> {
                            if (removeBalance(target.getUniqueId(), name, amount, "ADMIN_TAKE"))
                                send(player, "take-success", "%amount%", format(amount),
                                        "%player%", name);
                            else send(player, "not-enough-gems");
                        }
                        case "set" -> {
                            if (setBalance(target.getUniqueId(), name, amount)) {
                                logTransaction(target.getUniqueId(), "ADMIN_SET",
                                        amount, "ADMIN_SET");
                                send(player, "set-success", "%player%", name);
                            } else send(player, "invalid-amount");
                        }
                        case "check" -> player.sendMessage(color(prefix() + "&e" + name
                                + " &7memiliki &b" + format(balance(target.getUniqueId()))
                                + " Gems"));
                    }
                } catch (NumberFormatException e) {
                    send(player, "invalid-amount");
                }
            }
            case "reload" -> {
                if (!isAdmin(player, "vortexgems.reload")) {
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

    private boolean consoleCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: gems <give|take|set|check> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = target.getName() == null ? args[1] : target.getName();

        try {
            long amount = Long.parseLong(args[2]);
            if (amount < 0) {
                sender.sendMessage("Invalid amount.");
                return true;
            }

            boolean success = switch (action) {
                case "give" -> addBalance(target.getUniqueId(), name, amount, "CONSOLE_GIVE");
                case "take" -> removeBalance(target.getUniqueId(), name, amount, "CONSOLE_TAKE");
                case "set" -> setBalance(target.getUniqueId(), name, amount);
                case "check" -> {
                    sender.sendMessage(name + " has " + balance(target.getUniqueId()) + " Gems.");
                    yield true;
                }
                default -> false;
            };

            sender.sendMessage(success ? "VortexGems: Success." : "VortexGems: Failed.");
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid amount.");
        }
        return true;
    }

    // ADMIN COMMAND

    private boolean adminCommand(Player player, String[] args) {
        if (!isAdmin(player, "vortexgems.editor")) {
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

        if (args[0].equalsIgnoreCase("category") && args.length >= 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("create")) {
                requestInput(player, "create-category", null);
                return true;
            }

            if (args.length < 3) return true;
            String id = safeId(args[2]);

            switch (action) {
                case "delete" -> {
                    if (!categories.contains("categories." + id)) {
                        player.sendMessage(color("&cKategori tidak ditemukan."));
                    } else {
                        deleteCategory(id);
                        player.sendMessage(color(prefix() + "&aKategori dan reward terkait dihapus."));
                    }
                }
                case "toggle" -> {
                    toggleCategory(id);
                    player.sendMessage(color(prefix() + "&aStatus kategori diperbarui."));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reward") && args.length >= 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("addhand")) {
                requestInput(player, "create-reward", null);
                return true;
            }

            if (args.length < 3) return true;
            String id = safeId(args[2]);

            switch (action) {
                case "delete" -> {
                    deleteReward(id);
                    player.sendMessage(color(prefix() + "&aReward dihapus."));
                }
                case "toggle" -> {
                    toggleReward(id);
                    player.sendMessage(color(prefix() + "&aStatus reward diperbarui."));
                }
                case "price" -> {
                    if (args.length < 4) return true;
                    try {
                        long price = Long.parseLong(args[3]);
                        if (price <= 0 || !rewards.contains("rewards." + id)) {
                            send(player, "invalid-amount");
                            return true;
                        }
                        rewards.set("rewards." + id + ".price", price);
                        saveRewards();
                        player.sendMessage(color(prefix() + "&aHarga reward diperbarui."));
                    } catch (NumberFormatException e) {
                        send(player, "invalid-amount");
                    }
                }
            }
            return true;
        }

        openAdmin(player);
        return true;
    }

    // CONFIRMATION MENU

    private void openConfirmation(Player player, String action, String context,
                                  String title, String description) {
        Menu menu = createMenu("CONFIRM", action + "|" + context, title, 27);
        put(menu, 11, "yes", item(Material.LIME_WOOL, "&aConfirm",
                List.of(description, "", "&eClick to confirm.")));
        put(menu, 15, "no", item(Material.RED_WOOL, "&cCancel",
                List.of("&7Cancel this transaction.")));
        player.openInventory(menu.inventory);
    }

    // INVENTORY EVENTS

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
                    case "balance" -> player.sendMessage(color(prefix()
                            + "&7Saldo: &b" + format(balance(player.getUniqueId())) + " Gems"));
                    case "top" -> showTop(player);
                }
            }
            case "BUY" -> {
                if (id.equals("purchase")) {
                    int amount = menu.amounts.getOrDefault(slot, 0);
                    if (amount > 0) {
                        if (getConfig().getBoolean("economy.confirm-purchase", true)) {
                            openConfirmation(player, "BUY", String.valueOf(amount),
                                    "&aConfirm purchase", "&7Buy " + amount + " Gems?");
                        } else {
                            buy(player, amount);
                            openBuy(player);
                        }
                    }
                } else if (id.equals("back")) openMain(player);
            }
            case "CATEGORIES" -> {
                if (id.equals("back")) openMain(player);
                else openRewards(player, id);
            }
            case "REWARDS" -> {
                if (id.equals("back")) openCategories(player);
                else if (getConfig().getBoolean("shop.confirm-purchase", true)) {
                    openConfirmation(player, "EXCHANGE", id,
                            "&aConfirm exchange", "&7Exchange this reward?");
                } else exchange(player, id);
            }
            case "CONFIRM" -> {
                String[] data = menu.context.split("\\|", 2);
                if (data.length != 2) return;
                if (id.equals("no")) {
                    player.closeInventory();
                    return;
                }
                if (id.equals("yes")) {
                    if (data[0].equals("BUY")) {
                        try {
                            buy(player, Integer.parseInt(data[1]));
                        } catch (NumberFormatException ignored) {
                            send(player, "invalid-amount");
                        }
                        openBuy(player);
                    } else if (data[0].equals("EXCHANGE")) {
                        exchange(player, data[1]);
                        openRewards(player, rewards.getString(
                                "rewards." + data[1] + ".category", ""));
                    }
                }
            }
            case "ADMIN" -> {
                switch (id) {
                    case "categories" -> openCategoryEditor(player);
                    case "rewards" -> openRewardEditor(player);
                    case "create-category" -> requestInput(player, "create-category", null);
                    case "create-reward" -> requestInput(player, "create-reward", null);
                    case "reload" -> {
                        loadFiles();
                        send(player, "reload-success");
                        openAdmin(player);
                    }
                }
            }
            case "CATEGORY_EDITOR" -> {
                if (id.equals("back")) openAdmin(player);
                else openCategoryActions(player, id);
            }
            case "CATEGORY_ACTIONS" -> {
                String category = menu.context;
                switch (id) {
                    case "toggle" -> {
                        toggleCategory(category);
                        openCategoryActions(player, category);
                    }
                    case "rename" -> requestInput(player, "category-name", category);
                    case "icon" -> {
                        setCategoryIcon(player, category);
                        openCategoryActions(player, category);
                    }
                    case "slot" -> requestInput(player, "category-slot", category);
                    case "delete" -> openConfirmation(player, "DELETE_CATEGORY",
                            category, "&cDelete Category?", "&7Delete category and its rewards?");
                    case "back" -> openCategoryEditor(player);
                }
            }
            case "REWARD_EDITOR" -> {
                if (id.equals("back")) openAdmin(player);
                else openRewardActions(player, id);
            }
            case "REWARD_ACTIONS" -> {
                String reward = menu.context;
                switch (id) {
                    case "toggle" -> {
                        toggleReward(reward);
                        openRewardActions(player, reward);
                    }
                    case "name" -> requestInput(player, "reward-name", reward);
                    case "price" -> requestInput(player, "reward-price", reward);
                    case "amount" -> requestInput(player, "reward-amount", reward);
                    case "category" -> requestInput(player, "reward-category", reward);
                    case "slot" -> requestInput(player, "reward-slot", reward);
                    case "item" -> {
                        setRewardItem(player, reward);
                        openRewardActions(player, reward);
                    }
                    case "delete" -> openConfirmation(player, "DELETE_REWARD",
                            reward, "&cDelete Reward?", "&7Delete this reward permanently?");
                    case "back" -> openRewardEditor(player);
                }
            }
        }

        if (menu.type.equals("CONFIRM") && id.equals("yes")) {
            String[] data = menu.context.split("\\|", 2);
            if (data.length == 2) {
                if (data[0].equals("DELETE_CATEGORY")) {
                    deleteCategory(data[1]);
                    player.sendMessage(color(prefix() + "&aKategori dan reward terkait dihapus."));
                    openCategoryEditor(player);
                } else if (data[0].equals("DELETE_REWARD")) {
                    deleteReward(data[1]);
                    player.sendMessage(color(prefix() + "&aReward dihapus."));
                    openRewardEditor(player);
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
        ensurePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    // PLACEHOLDER API

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

            if (params.matches("top_[1-9][0-9]*_name")) {
                try {
                    int rank = Integer.parseInt(
                            params.substring(4, params.length() - 5));
                    return plugin.topName(rank);
                } catch (NumberFormatException ignored) {
                    return "None";
                }
            }

            if (params.matches("top_[1-9][0-9]*_balance")) {
                try {
                    int rank = Integer.parseInt(
                            params.substring(4, params.length() - 8));
                    return String.valueOf(plugin.topBalance(rank));
                } catch (NumberFormatException ignored) {
                    return "0";
                }
            }

            if (player == null) return "0";

            if (params.equalsIgnoreCase("balance")) {
                return String.valueOf(plugin.balance(player.getUniqueId()));
            }

            if (params.equalsIgnoreCase("formatted")) {
                return plugin.format(plugin.balance(player.getUniqueId()));
            }

            if (params.equalsIgnoreCase("rank")) {
                return String.valueOf(plugin.rank(player.getUniqueId()));
            }

            return null;
        }
    }
            }
