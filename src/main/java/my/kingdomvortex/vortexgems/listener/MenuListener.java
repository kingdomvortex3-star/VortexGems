
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

        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu().toUpperCase()) {
            case "MAIN" -> handleMain(player, slot);
            case "BUY" -> handleBuy(player, slot);
            case "CATEGORY" -> handleCategory(player, slot);
            case "REWARD" -> handleReward(player, slot, holder);
            case "CONFIRM" -> handleConfirm(player, slot, holder);
            case "ADMIN" -> handleAdmin(player, slot);
            case "CATEGORY_EDITOR" -> handleCategoryEditor(player, slot);
            case "REWARD_EDITOR" -> handleRewardEditor(player, slot);
            case "SETTINGS" -> handleSettings(player, slot);
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
        long amount = switch (slot) {
            case 20 -> 10;
            case 22 -> 50;
            case 24 -> 100;
            case 30 -> 500;
            case 32 -> 1000;
            default -> 0;
        };

        if (slot == 40) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        if (amount <= 0) {
            return;
        }

        if (!plugin.getEconomyManager().buyGems(player, amount)) {
            player.sendMessage(ColorUtil.text(
                    plugin.getConfigManager().getMessages().getString(
                            "messages.economy.purchase-failed",
                            "<red>Purchase failed."
                    )
            ));
            return;
        }

        player.sendMessage(ColorUtil.text(
                "<green>Successfully purchased <aqua>" +
                        ColorUtil.formatGems(amount) + " Gems<green>."
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
        if (category == null) {
            return;
        }

        for (String id : plugin.getRewardManager().getRewardsInCategory(category)) {
            if (plugin.getRewardManager().getSlot(id) == slot) {
                plugin.getMenuManager().openConfirm(player, id);
                return;
            }
        }
    }

    private void handleConfirm(Player player, int slot, MenuHolder holder) {
        String rewardId = holder.getData();

        if (slot == 15) {
            plugin.getMenuManager().openRewards(
                    player,
                    plugin.getRewardManager().getCategory(rewardId)
            );
            return;
        }

        if (slot != 11 || rewardId == null
                || !plugin.getRewardManager().exists(rewardId)
                || !plugin.getRewardManager().isEnabled(rewardId)) {
            return;
        }

        long price = plugin.getRewardManager().getPrice(rewardId);

        if (price < 0 || plugin.getGemsManager().getBalance(
                player.getUniqueId()) < price) {
            player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
            return;
        }

        String type = plugin.getRewardManager().getType(rewardId);

        if (type.equals("ITEM")) {
            Material material = plugin.getRewardManager().getMaterial(rewardId);
            int amount = plugin.getRewardManager().getAmount(rewardId);

            if (material == null || !material.isItem()) {
                player.sendMessage(ColorUtil.text("<red>Invalid reward item."));
                return;
            }

            ItemStack reward = new ItemStack(material, amount);

            if (!canFit(player, reward)) {
                player.sendMessage(ColorUtil.text("<red>Your inventory is full."));
                return;
            }

            if (!plugin.getGemsManager().removeBalance(player.getUniqueId(), price)) {
                player.sendMessage(ColorUtil.text("<red>Purchase failed."));
                return;
            }

            player.getInventory().addItem(reward);

        } else if (type.equals("COMMAND")) {
            var commands = plugin.getRewardManager().getCommands(rewardId);

            if (commands.isEmpty()) {
                player.sendMessage(ColorUtil.text("<red>No reward commands configured."));
                return;
            }

            if (!plugin.getGemsManager().removeBalance(player.getUniqueId(), price)) {
                player.sendMessage(ColorUtil.text("<red>Purchase failed."));
                return;
            }

            for (String command : commands) {
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

        if (price > 0) {
            plugin.getTransactionManager().record(
                    player.getUniqueId(),
                    "REWARD_PURCHASE",
                    price,
                    "Purchased reward: " + rewardId
            );
        }

        player.sendMessage(ColorUtil.text("<green>Reward purchased successfully!"));
        player.closeInventory();
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(
                        0,
                        content.getMaxStackSize() - content.getAmount()
                );
            }

            if (remaining <= 0) {
                return true;
            }
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
            case 31 -> {
                plugin.reloadPlugin();
                player.sendMessage(ColorUtil.text("<green>VortexGems reloaded."));
                plugin.getMenuManager().openAdmin(player);
            }
        }
    }

    private void handleCategoryEditor(Player player, int slot) {
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
            if (index++ != slot) {
                continue;
            }

            if (player.getOpenInventory().getCursor() != null
                    && !player.getOpenInventory().getCursor().getType().isAir()) {
                return;
            }

            if (player.isSneaking()) {
                plugin.getCategoryManager().delete(id);
                player.sendMessage(ColorUtil.text("<green>Category deleted: " + id));
            } else {
                boolean enabled = plugin.getCategoryManager().isEnabled(id);
                plugin.getCategoryManager().setEnabled(id, !enabled);
                player.sendMessage(ColorUtil.text(
                        "<green>Category " + id + " is now " +
                                (!enabled ? "enabled." : "disabled.")
                ));
            }

            plugin.getMenuManager().openCategoryEditor(player);
            return;
        }
    }

    private void handleRewardEditor(Player player, int slot) {
        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        player.sendMessage(ColorUtil.text(
                "<yellow>Manage rewards using /gemsadmin reward commands."
        ));
    }

    private void handleSettings(Player player, int slot) {
        if (slot == 40) {
            plugin.getMenuManager().openAdmin(player);
        }
    }
}
