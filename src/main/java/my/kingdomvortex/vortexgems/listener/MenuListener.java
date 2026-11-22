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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu().toUpperCase(Locale.ROOT)) {
            case "MAIN" -> handleMain(player, slot);
            case "BUY" -> handleBuy(player, slot);
            case "CATEGORY" -> handleCategory(player, slot);
            case "REWARD" -> handleReward(player, slot, holder);
            case "CONFIRM" -> handleConfirm(player, slot, holder);
            case "ADMIN" -> handleAdmin(player, slot);
            case "CATEGORY_EDITOR" -> handleCategoryEditor(player, slot, event);
            case "REWARD_EDITOR" -> handleRewardEditor(player, slot, event);
            case "SETTINGS" -> handleSettings(player, slot);
            default -> {
            }
        }
    }

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 20 -> plugin.getMenuManager().openBuy(player);
            case 22 -> plugin.getMenuManager().openCategories(player);
            case 24 -> {
                if (player.hasPermission("vortexgems.admin")) {
                    plugin.getMenuManager().openAdmin(player);
                } else {
                    player.sendMessage(ColorUtil.text("<red>You don't have permission."));
                }
            }
            case 40 -> player.closeInventory();
        }
    }

    private void handleBuy(Player player, int slot) {
        long amount = switch (slot) {
            case 20 -> 10;
            case 22 -> 50;
            case 24 -> 100;
            case 30 -> 500;
            case 32 -> 1000;
            default -> 0;
        };

        if (amount == 0) {
            if (slot == 40) plugin.getMenuManager().openMain(player);
            return;
        }

        if (!plugin.getEconomyManager().buyGems(player, amount)) {
            player.sendMessage(ColorUtil.text(
                    "<red>Purchase failed. Check your money and purchase settings."
            ));
            return;
        }

        player.sendMessage(ColorUtil.text(
                "<green>You purchased <aqua>" + ColorUtil.formatGems(amount) + "<green>."
        ));

        plugin.getMenuManager().openBuy(player);
    }

    private void handleCategory(Player player, int slot) {
        if (slot == 49) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        for (String id : plugin.getCategoryManager().getEnabledCategories()) {
            if (plugin.getCategoryManager().getSlot(id) == slot) {
                plugin.getMenuManager().openRewards(player, id);
                return;
            }
        }
    }

    private void handleReward(Player player, int slot, MenuHolder holder) {
        if (slot == 49) {
            plugin.getMenuManager().openCategories(player);
            return;
        }

        String category = holder.getData();

        if (category == null) return;

        for (String id : plugin.getRewardManager().getRewardsInCategory(category)) {
            if (plugin.getRewardManager().getSlot(id) == slot) {
                plugin.getMenuManager().openConfirm(player, id);
                return;
            }
        }
    }

    private void handleConfirm(Player player, int slot, MenuHolder holder) {
        String rewardId = holder.getData();

        // Tombol confirm berada di slot 11.
        if (slot == 15) {
            String category = rewardId == null
                    ? null
                    : plugin.getRewardManager().getCategory(rewardId);

            if (category == null) {
                plugin.getMenuManager().openCategories(player);
            } else {
                plugin.getMenuManager().openRewards(player, category);
            }
            return;
        }

        if (slot != 11 || rewardId == null) return;

        if (!plugin.getRewardManager().exists(rewardId)
                || !plugin.getRewardManager().isEnabled(rewardId)) {
            player.sendMessage(ColorUtil.text("<red>Reward is unavailable."));
            player.closeInventory();
            return;
        }

        long price = plugin.getRewardManager().getPrice(rewardId);

        if (price <= 0) {
            player.sendMessage(ColorUtil.text("<red>Invalid reward price."));
            return;
        }

        String type = plugin.getRewardManager().getType(rewardId);

        if (type.equalsIgnoreCase("ITEM")) {
            Material material = plugin.getRewardManager().getMaterial(rewardId);
            int amount = plugin.getRewardManager().getAmount(rewardId);

            if (material == null || !material.isItem()) {
                player.sendMessage(ColorUtil.text("<red>Invalid reward item."));
                return;
            }

            ItemStack rewardItem = new ItemStack(material, amount);

            if (!canFit(player, rewardItem)) {
                player.sendMessage(ColorUtil.text("<red>Your inventory is full."));
                return;
            }

            if (!plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            player.getInventory().addItem(rewardItem);

        } else if (type.equalsIgnoreCase("COMMAND")) {
            if (!plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            for (String command : plugin.getRewardManager().getCommands(rewardId)) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command.replace("{player}", player.getName())
                                .replace("{uuid}", player.getUniqueId().toString())
                );
            }

        } else {
            player.sendMessage(ColorUtil.text("<red>Unsupported reward type."));
            return;
        }

        plugin.getTransactionManager().record(
                player.getUniqueId(),
                "REWARD_PURCHASE",
                price,
                "Purchased reward: " + rewardId
        );

        player.sendMessage(ColorUtil.text("<green>Reward purchased successfully!"));
        player.closeInventory();
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();

        for (ItemStack contents : player.getInventory().getStorageContents()) {
            if (contents == null || contents.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (contents.isSimilar(item)) {
                remaining -= Math.max(
                        0,
                        contents.getMaxStackSize() - contents.getAmount()
                );
            }

            if (remaining <= 0) return true;
        }

        return remaining <= 0;
    }

    private void handleAdmin(Player player, int slot) {
        if (!player.hasPermission("vortexgems.admin")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 11 -> plugin.getMenuManager().openCategoryEditor(player);
            case 13 -> plugin.getMenuManager().openRewardEditor(player);
            case 15 -> plugin.getMenuManager().openSettings(player);
            case 31 -> player.closeInventory();
        }
    }

    private void handleCategoryEditor(
            Player player,
            int slot,
            InventoryClickEvent event
    ) {
        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            player.sendMessage(ColorUtil.text(
                    "<yellow>Use /gemsadmin category create to create a category."
            ));
            return;
        }

        int index = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (index++ != slot) continue;

            if (event.isRightClick()) {
                plugin.getCategoryManager().delete(id);
                player.sendMessage(ColorUtil.text(
                        "<green>Category <white>" + id + " <green>deleted."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
            } else {
                player.sendMessage(ColorUtil.text(
                        "<yellow>Category: <white>" + id
                                + "<gray>. Use /gemsadmin category delete "
                                + id + " to delete it."
                ));
            }
            return;
        }
    }

    private void handleRewardEditor(
            Player player,
            int slot,
            InventoryClickEvent event
    ) {
        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            player.sendMessage(ColorUtil.text(
                    "<yellow>Use /gemsadmin reward create to create a reward."
            ));
            return;
        }

        int index = 0;

        for (String id : plugin.getRewardManager().getRewardIds()) {
            if (index++ != slot) continue;

            if (event.isRightClick()) {
                plugin.getRewardManager().delete(id);
                player.sendMessage(ColorUtil.text(
                        "<green>Reward <white>" + id + " <green>deleted."
                ));
                plugin.getMenuManager().openRewardEditor(player);
            } else {
                player.sendMessage(ColorUtil.text(
                        "<yellow>Reward: <white>" + id
                                + "<gray>. Use /gemsadmin reward delete "
                                + id + " to delete it."
                ));
            }
            return;
        }
    }

    private void handleSettings(Player player, int slot) {
        if (!player.hasPermission("vortexgems.admin")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 12 -> {
                boolean current = plugin.getConfig().getBoolean("transfer.enabled", true);
                plugin.getConfig().set("transfer.enabled", !current);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 14 -> {
                boolean current = plugin.getConfig().getBoolean("admin.op-bypass", true);
                plugin.getConfig().set("admin.op-bypass", !current);
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
