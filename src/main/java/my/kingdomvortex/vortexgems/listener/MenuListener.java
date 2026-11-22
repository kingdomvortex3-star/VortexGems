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
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class MenuListener implements Listener {

    private final VortexGems plugin;

    public MenuListener(VortexGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu().toLowerCase(Locale.ROOT)) {
            case "main" -> handleMain(player, slot);
            case "buy" -> handleBuy(player, slot);
            case "categories" -> handleCategories(player, slot);
            case "rewards" -> handleRewards(player, slot, holder);
            case "confirm" -> handleConfirm(player, slot, holder);
            case "admin" -> handleAdmin(player, slot);
            case "category_editor" -> handleCategoryEditor(player, slot);
            case "reward_editor" -> handleRewardEditor(player, slot);
            case "settings" -> handleSettings(player, slot);
            default -> {
            }
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

        if (amount == 0) {
            if (slot == 40) {
                plugin.getMenuManager().openMain(player);
            }
            return;
        }

        if (!plugin.getEconomyManager().buyGems(player, amount)) {
            player.sendMessage(ColorUtil.text("<red>Gems purchase failed. Check your money."));
            return;
        }

        player.sendMessage(ColorUtil.text(
                "<green>Successfully purchased <aqua>"
                        + amount + " Gems<green>."
        ));

        plugin.getMenuManager().openBuy(player);
    }

    private void handleCategories(Player player, int slot) {
        if (slot == 40) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        // CategoryMenu uses category IDs as inventory holder data per item.
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);

        if (item == null || item.getType().isAir()) {
            return;
        }

        String category = item.getItemMeta() == null
                ? null
                : item.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "category"),
                        org.bukkit.persistence.PersistentDataType.STRING
                );

        if (category != null) {
            plugin.getMenuManager().openRewards(player, category);
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

        String rewardId = item.getItemMeta() == null
                ? null
                : item.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "reward"),
                        org.bukkit.persistence.PersistentDataType.STRING
                );

        if (rewardId != null) {
            plugin.getMenuManager().openConfirm(player, rewardId);
        }
    }

    private void handleConfirm(Player player, int slot, MenuHolder holder) {
        String rewardId = holder.getData();

        if (slot == 11) {
            player.closeInventory();
            return;
        }

        if (slot != 15 || rewardId == null || !plugin.getRewardManager().exists(rewardId)) {
            return;
        }

        var reward = plugin.getConfigManager().getRewards()
                .getConfigurationSection("rewards." + rewardId);

        if (reward == null || !plugin.getRewardManager().isEnabled(rewardId)) {
            player.sendMessage(ColorUtil.text("<red>This reward is unavailable."));
            player.closeInventory();
            return;
        }

        long price = plugin.getRewardManager().getPrice(rewardId);

        if (!plugin.getGemsManager().removeBalance(player.getUniqueId(), price)) {
            player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
            return;
        }

        String type = plugin.getRewardManager().getType(rewardId);

        if (type.equalsIgnoreCase("ITEM")) {
            Material material = plugin.getRewardManager().getMaterial(rewardId);
            int amount = plugin.getRewardManager().getAmount(rewardId);
            ItemStack item = new ItemStack(material, amount);

            var leftovers = player.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                plugin.getGemsManager().addBalance(player.getUniqueId(), price);
                player.sendMessage(ColorUtil.text("<red>Your inventory is full."));
                return;
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            for (String command : plugin.getRewardManager().getCommands(rewardId)) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command.replace("{player}", player.getName())
                                .replace("{uuid}", player.getUniqueId().toString())
                );
            }
        } else {
            plugin.getGemsManager().addBalance(player.getUniqueId(), price);
            player.sendMessage(ColorUtil.text("<red>Invalid reward type."));
            return;
        }

        plugin.getTransactionManager().record(
                player.getUniqueId(),
                "REWARD_PURCHASE",
                -price,
                "Purchased reward: " + rewardId
        );

        player.sendMessage(ColorUtil.text("<green>Reward purchased successfully!"));
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
                player.sendMessage(ColorUtil.text("<green>Configuration reloaded."));
                plugin.getMenuManager().openAdmin(player);
            }
            case 40 -> plugin.getMenuManager().openMain(player);
        }
    }

    private void handleCategoryEditor(Player player, int slot) {
        if (slot == 40 || slot == 49) {
            plugin.getMenuManager().openAdmin(player);
        }
    }

    private void handleRewardEditor(Player player, int slot) {
        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
        }
    }

    private void handleSettings(Player player, int slot) {
        if (!player.hasPermission("vortexgems.admin")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
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
            case 31 -> {
                plugin.reloadPlugin();
                player.sendMessage(ColorUtil.text("<green>Configuration reloaded."));
                plugin.getMenuManager().openSettings(player);
            }
            case 40 -> plugin.getMenuManager().openAdmin(player);
        }
    }
    }
