package my.kingdomvortex.vortexgems.listener;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.gui.MenuHolder;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final VortexGems plugin;

    public MenuListener(VortexGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenuType().toLowerCase()) {
            case "main" -> handleMain(player, slot);
            case "buy" -> handleBuy(player, slot);
            case "categories" -> handleCategories(player, slot, holder);
            case "rewards" -> handleRewards(player, slot, holder);
            case "confirm" -> handleConfirm(player, slot, holder);
            case "admin" -> handleAdmin(player, slot);
            case "category_editor" -> handleCategoryEditor(player, slot, holder);
            case "reward_editor" -> handleRewardEditor(player, slot, holder);
            case "settings" -> handleSettings(player, slot);
            default -> player.closeInventory();
        }
    }

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 20 -> plugin.getMenuManager().openBuy(player);
            case 22 -> plugin.getMenuManager().openCategories(player);
            case 24 -> plugin.getMenuManager().openAdmin(player);
            case 40 -> player.closeInventory();
        }
    }

    private void handleBuy(Player player, int slot) {
        int amount = switch (slot) {
            case 20 -> 10;
            case 22 -> 50;
            case 24 -> 100;
            case 30 -> 500;
            case 32 -> 1000;
            default -> 0;
        };

        if (amount <= 0) {
            if (slot == 40) {
                plugin.getMenuManager().openMain(player);
            }
            return;
        }

        if (!plugin.getEconomyManager().buyGems(player, amount)) {
            player.sendMessage(ColorUtil.text(
                    plugin.getConfigManager().getMessage(
                            "economy.purchase-failed",
                            "<red>Purchase failed."
                    )
            ));
            return;
        }

        player.sendMessage(ColorUtil.text(
                plugin.getConfigManager().getMessage(
                        "economy.purchase-success",
                        "<green>You purchased <white>{amount} Gems<green>."
                ).replace("{amount}", String.valueOf(amount))
        ));

        plugin.getMenuManager().openBuy(player);
    }

    private void handleCategories(Player player, int slot, MenuHolder holder) {
        if (slot == 40) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }

        String categoryId = holder.getData();
        if (categoryId != null && !categoryId.isBlank()) {
            plugin.getMenuManager().openRewards(player, categoryId);
        }
    }

    private void handleRewards(Player player, int slot, MenuHolder holder) {
        if (slot == 40) {
            plugin.getMenuManager().openCategories(player);
            return;
        }

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }

        String rewardId = holder.getData();
        if (rewardId == null || rewardId.isBlank()) {
            return;
        }

        plugin.getMenuManager().openConfirm(player, rewardId);
    }

    private void handleConfirm(Player player, int slot, MenuHolder holder) {
        String rewardId = holder.getData();

        if (slot == 11) {
            player.closeInventory();
            return;
        }

        if (slot != 15 || rewardId == null || rewardId.isBlank()) {
            return;
        }

        var reward = plugin.getRewardManager().getReward(rewardId);
        if (reward == null) {
            player.sendMessage(ColorUtil.text("<red>Reward not found."));
            player.closeInventory();
            return;
        }

        int price = reward.getInt("price", 0);
        if (price <= 0) {
            player.sendMessage(ColorUtil.text("<red>Invalid reward price."));
            return;
        }

        String type = reward.getString("type", "ITEM").toUpperCase();
        String command = reward.getString("command", "");

        if (type.equals("ITEM")) {
            String materialName = reward.getString("material", "DIAMOND");
            Material material = Material.matchMaterial(materialName);

            if (material == null || !material.isItem()) {
                player.sendMessage(ColorUtil.text("<red>Invalid reward item."));
                return;
            }

            int amount = Math.max(1, reward.getInt("amount", 1));
            ItemStack rewardItem = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));

            if (amount > material.getMaxStackSize()) {
                player.sendMessage(ColorUtil.text("<red>Reward amount exceeds maximum stack size."));
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(ColorUtil.text("<red>Your inventory is full."));
                return;
            }

            if (!plugin.getGemsManager().removeBalance(player.getUniqueId(), price)) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            player.getInventory().addItem(rewardItem);
        } else if (type.equals("COMMAND")) {
            if (command.isBlank()) {
                player.sendMessage(ColorUtil.text("<red>Reward command is not configured."));
                return;
            }

            if (!plugin.getGemsManager().removeBalance(player.getUniqueId(), price)) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{player}", player.getName())
                            .replace("{uuid}", player.getUniqueId().toString())
            );
        } else {
            player.sendMessage(ColorUtil.text("<red>Unsupported reward type."));
            return;
        }

        plugin.getTransactionManager().record(
                player.getUniqueId(),
                "REWARD_PURCHASE",
                -price,
                "Purchased reward: " + rewardId
        );

        player.sendMessage(ColorUtil.text(
                plugin.getConfigManager().getMessage(
                        "reward.purchase-success",
                        "<green>Reward purchased successfully!"
                )
        ));

        player.closeInventory();
    }

    private void handleAdmin(Player player, int slot) {
        if (!player.hasPermission("vortexgems.admin")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> plugin.getMenuManager().openCategoryEditor(player);
            case 12 -> plugin.getMenuManager().openRewardEditor(player);
            case 14 -> plugin.getMenuManager().openSettings(player);
            case 31 -> {
                plugin.reloadPlugin();
                player.sendMessage(ColorUtil.text("<green>VortexGems configuration reloaded."));
                plugin.getMenuManager().openAdmin(player);
            }
            case 40 -> plugin.getMenuManager().openMain(player);
        }
    }

    private void handleCategoryEditor(Player player, int slot, MenuHolder holder) {
        if (slot == 40) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 41) {
            player.closeInventory();
            player.sendMessage(ColorUtil.text(
                    "<yellow>Use /gemsadmin category create <id> <name> to create a category."
            ));
        }
    }

    private void handleRewardEditor(Player player, int slot, MenuHolder holder) {
        if (slot == 40) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 41) {
            player.closeInventory();
            player.sendMessage(ColorUtil.text(
                    "<yellow>Use /gemsadmin reward create <id> <category> <name> <price> to create a reward."
            ));
        }
    }

    private void handleSettings(Player player, int slot) {
        if (!player.hasPermission("vortexgems.admin")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                player.sendMessage(ColorUtil.text(
                        "<yellow>Change purchase price in config.yml: economy.price-per-gem"
                ));
            }
            case 12 -> {
                boolean enabled = plugin.getConfig().getBoolean("transfer.enabled", true);
                plugin.getConfig().set("transfer.enabled", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 14 -> {
                boolean enabled = plugin.getConfig().getBoolean("admin.op-bypass", true);
                plugin.getConfig().set("admin.op-bypass", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 16 -> {
                player.closeInventory();
                player.sendMessage(ColorUtil.text(
                        "<yellow>Change leaderboard limit in config.yml: leaderboard.limit"
                ));
            }
            case 31 -> {
                plugin.reloadPlugin();
                player.sendMessage(ColorUtil.text("<green>Configuration reloaded."));
                plugin.getMenuManager().openSettings(player);
            }
            case 40 -> plugin.getMenuManager().openAdmin(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Reserved for future menu cleanup.
    }
  }
