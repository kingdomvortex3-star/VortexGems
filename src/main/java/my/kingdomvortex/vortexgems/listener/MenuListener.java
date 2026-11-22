
package my.kingdomvortex.vortexgems.listener;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.gui.MenuHolder;
import my.kingdomvortex.vortexgems.gui.RewardCreateMenu;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        String menu = holder.getMenu().toUpperCase();

        if (menu.equals("REWARD_CREATE")) {
            handleRewardCreate(event, player, top, holder);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();

        switch (menu) {
            case "MAIN" -> handleMain(player, slot);
            case "BUY" -> handleBuy(player, slot, top);
            case "CATEGORY" -> handleCategory(player, slot, top);
            case "REWARD" -> handleReward(player, slot, top, holder);
            case "CONFIRM" -> handleConfirm(player, slot, holder);
            case "ADMIN" -> handleAdmin(player, slot);
            case "CATEGORY_EDITOR" -> handleCategoryEditor(player, slot, event.getClick());
            case "REWARD_EDITOR" -> handleRewardEditor(player, slot);
            case "REWARD_CATEGORY" -> handleRewardCategory(player, slot, event.getClick(), holder);
            case "SETTINGS" -> handleSettings(player, slot);
            default -> player.closeInventory();
        }
    }

    private void handleRewardCreate(
            InventoryClickEvent event,
            Player player,
            Inventory inventory,
            MenuHolder holder
    ) {
        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getRawSlot();
        String category = holder.getData();

        if (slot == 13) {
            if (event.getClick() != ClickType.LEFT
                    && event.getClick() != ClickType.RIGHT) {
                return;
            }

            ItemStack cursor = event.getCursor();
            ItemStack current = inventory.getItem(13);

            if (cursor == null || cursor.getType().isAir()) {
                if (current == null
                        || current.getType().isAir()
                        || current.getType() == Material.HOPPER) {
                    return;
                }

                event.setCursor(current.clone());
                inventory.setItem(13, new ItemStack(Material.HOPPER));

            } else {
                if (cursor.getType() == Material.HOPPER) {
                    return;
                }

                inventory.setItem(13, cursor.clone());
                event.setCursor(
                        current != null && current.getType() != Material.HOPPER
                                ? current.clone()
                                : null
                );
            }

            return;
        }

        if (slot == 11) {
            player.closeInventory();

            new RewardCreateMenu(plugin).promptPrice(player, category);
            return;
        }

        if (slot == 15) {
            boolean saved = new RewardCreateMenu(plugin).save(
                    player,
                    inventory,
                    category
            );

            if (saved) {
                plugin.getMenuManager().openRewardCategory(player, category);
            }

            return;
        }

        if (slot == 22) {
            RewardCreateMenu.clearPrice(player.getUniqueId());
            plugin.getMenuManager().openRewardCategory(player, category);
        }
    }

    private void handleMain(Player player, int slot) {
        if (slot == plugin.getConfig().getInt("gui.main.buy-slot", 20)) {
            plugin.getMenuManager().openBuy(player);
        } else if (slot == plugin.getConfig().getInt("gui.main.shop-slot", 22)) {
            plugin.getMenuManager().openCategories(player);
        } else if (slot == plugin.getConfig().getInt("gui.main.close-slot", 40)) {
            player.closeInventory();
        }
    }

    private void handleBuy(Player player, int slot, Inventory inventory) {
        if (slot == inventory.getSize() - 5) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        long amount = switch (slot) {
            case 20 -> 10;
            case 22 -> 50;
            case 24 -> 100;
            case 30 -> 500;
            case 32 -> 1000;
            default -> 0;
        };

        if (amount <= 0) {
            return;
        }

        if (!plugin.getEconomyManager().buyGems(player, amount)) {
            player.sendMessage(ColorUtil.text(
                    "<red>Purchase failed. Check your money and purchase settings."
            ));
            return;
        }

        player.sendMessage(ColorUtil.text(
                "<green>You purchased <aqua>" +
                        ColorUtil.formatGems(amount) + " Gems<green>."
        ));

        plugin.getMenuManager().openBuy(player);
    }

    private void handleCategory(Player player, int slot, Inventory inventory) {
        if (slot == inventory.getSize() - 5) {
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

    private void handleReward(
            Player player,
            int slot,
            Inventory inventory,
            MenuHolder holder
    ) {
        if (slot == inventory.getSize() - 5) {
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

        if (rewardId == null || !plugin.getRewardManager().exists(rewardId)) {
            player.closeInventory();
            return;
        }

        if (slot == 15) {
            plugin.getMenuManager().openRewards(
                    player,
                    plugin.getRewardManager().getCategory(rewardId)
            );
            return;
        }

        if (slot != 11 || !plugin.getRewardManager().isEnabled(rewardId)) {
            return;
        }

        long price = plugin.getRewardManager().getPrice(rewardId);
        String type = plugin.getRewardManager().getType(rewardId);

        if (price < 0) {
            player.sendMessage(ColorUtil.text("<red>Invalid reward price."));
            return;
        }

        if ("ITEM".equalsIgnoreCase(type)) {
            ItemStack reward = plugin.getRewardManager().getItemStack(rewardId);

            if (reward == null || !canFit(player, reward)) {
                player.sendMessage(ColorUtil.text(
                        "<red>Invalid reward or your inventory is full."
                ));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            player.getInventory().addItem(reward);

        } else if ("COMMAND".equalsIgnoreCase(type)) {
            List<String> commands = plugin.getRewardManager().getCommands(rewardId);

            if (commands == null || commands.isEmpty()) {
                player.sendMessage(ColorUtil.text("<red>No reward commands configured."));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
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

    private boolean isAdmin(Player player) {
        String permission = plugin.getConfig().getString(
                "settings.admin.permission",
                "vortexgems.admin"
        );

        return player.hasPermission(permission)
                || (plugin.getConfig().getBoolean(
                        "settings.admin.op-bypass", true
                ) && player.isOp());
    }

    private void handleAdmin(Player player, int slot) {
        if (!isAdmin(player)) {
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

    private void handleCategoryEditor(Player player, int slot, ClickType click) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            player.sendMessage(ColorUtil.text(
                    "<yellow>Category creation is available through /gemsadmin category create."
            ));
            return;
        }

        int index = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (index++ != slot) {
                continue;
            }

            if (click == ClickType.RIGHT) {
                plugin.getCategoryManager().delete(id);
            } else {
                boolean enabled = plugin.getCategoryManager().isEnabled(id);
                plugin.getCategoryManager().setEnabled(id, !enabled);
            }

            plugin.getMenuManager().openCategoryEditor(player);
            return;
        }
    }

    private void handleRewardEditor(Player player, int slot) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        int index = 0;

        for (String category : plugin.getCategoryManager().getCategoryIds()) {
            if (index++ == slot) {
                plugin.getMenuManager().openRewardCategory(player, category);
                return;
            }
        }

        if (slot == 45) {
            player.sendMessage(ColorUtil.text(
                    "<yellow>Select a category first."
            ));
        }
    }

    private void handleRewardCategory(
            Player player,
            int slot,
            ClickType click,
            MenuHolder holder
    ) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        String category = holder.getData();

        if (category == null) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            plugin.getMenuManager().openRewardEditor(player);
            return;
        }

        if (slot == 45) {
            new RewardCreateMenu(plugin).open(player, category);
            return;
        }

        int index = 0;

        for (String id : plugin.getRewardManager().getRewardIds()) {
            if (!plugin.getRewardManager().getCategory(id)
                    .equalsIgnoreCase(category)) {
                continue;
            }

            if (plugin.getRewardManager().getSlot(id) != slot) {
                continue;
            }

            if (click == ClickType.RIGHT) {
                plugin.getRewardManager().delete(id);
                player.sendMessage(ColorUtil.text(
                        "<green>Reward deleted: <white>" + id
                ));
            } else {
                boolean enabled = plugin.getRewardManager().isEnabled(id);
                plugin.getRewardManager().setEnabled(id, !enabled);
                player.sendMessage(ColorUtil.text(
                        "<green>Reward status updated."
                ));
            }

            plugin.getMenuManager().openRewardCategory(player, category);
            return;
        }
    }

    private void handleSettings(Player player, int slot) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 12 -> {
                boolean enabled = plugin.getConfig().getBoolean(
                        "settings.transfer.enabled", true
                );
                plugin.getConfig().set("settings.transfer.enabled", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 14 -> {
                boolean enabled = plugin.getConfig().getBoolean(
                        "settings.admin.op-bypass", true
                );
                plugin.getConfig().set("settings.admin.op-bypass", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 31 -> {
                plugin.reloadPlugin();
                plugin.getMenuManager().openSettings(player);
            }
            case 40 -> plugin.getMenuManager().openAdmin(player);
        }
    }
}
