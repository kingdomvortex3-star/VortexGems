
package my.kingdomvortex.vortexgems.listener;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.gui.MenuHolder;
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

        event.setCancelled(true);

        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu().toUpperCase()) {
            case "MAIN" -> handleMain(player, slot);
            case "BUY" -> handleBuy(player, slot, top);
            case "CATEGORY" -> handleCategory(player, slot, top);
            case "REWARD" -> handleReward(player, slot, top, holder);
            case "CONFIRM" -> handleConfirm(player, slot, holder);
            case "ADMIN" -> handleAdmin(player, slot);
            case "CATEGORY_EDITOR" -> handleCategoryEditor(player, slot, event.getClick());
            case "REWARD_EDITOR" -> handleRewardEditor(player, slot, event.getClick());
            case "SETTINGS" -> handleSettings(player, slot);
            default -> player.closeInventory();
        }
    }

    private void handleMain(Player player, int slot) {
        if (slot == plugin.getConfig().getInt("gui.main.buy-slot", 20)) {
            plugin.getMenuManager().openBuy(player);
            return;
        }

        if (slot == plugin.getConfig().getInt("gui.main.shop-slot", 22)) {
            plugin.getMenuManager().openCategories(player);
            return;
        }

        if (slot == plugin.getConfig().getInt("gui.main.close-slot", 40)) {
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
                "<green>You successfully purchased <aqua>" +
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

        if (category == null || category.isBlank()) {
            return;
        }

        for (String id : plugin.getRewardManager().getRewardsInCategory(category)) {
            if (plugin.getRewardManager().getSlot(id) == slot
                    && plugin.getRewardManager().isEnabled(id)) {
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

        if (slot != 11) {
            return;
        }

        if (!plugin.getRewardManager().isEnabled(rewardId)) {
            player.sendMessage(ColorUtil.text("<red>This reward is disabled."));
            player.closeInventory();
            return;
        }

        Material material = plugin.getRewardManager().getMaterial(rewardId);
        int amount = plugin.getRewardManager().getAmount(rewardId);
        long price = plugin.getRewardManager().getPrice(rewardId);
        String type = plugin.getRewardManager().getType(rewardId);

        if (price < 0) {
            player.sendMessage(ColorUtil.text("<red>Invalid reward price."));
            return;
        }

        if ("ITEM".equalsIgnoreCase(type)) {
            if (material == null || !material.isItem() || amount < 1) {
                player.sendMessage(ColorUtil.text("<red>Invalid reward item."));
                return;
            }

            ItemStack reward = new ItemStack(material);
            if (!canFit(player, reward, amount)) {
                player.sendMessage(ColorUtil.text("<red>Your inventory is full."));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(),
                    price
            )) {
                player.sendMessage(ColorUtil.text("<red>You don't have enough Gems."));
                return;
            }

            player.getInventory().addItem(new ItemStack(material, amount));

        } else if ("COMMAND".equalsIgnoreCase(type)) {
            List<String> commands = plugin.getRewardManager().getCommands(rewardId);

            if (commands == null || commands.isEmpty()) {
                player.sendMessage(ColorUtil.text("<red>No reward commands configured."));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(),
                    price
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

    private boolean canFit(Player player, ItemStack prototype, int amount) {
        long capacity = 0;

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                capacity += prototype.getMaxStackSize();
            } else if (content.isSimilar(prototype)) {
                capacity += Math.max(
                        0,
                        content.getMaxStackSize() - content.getAmount()
                );
            }

            if (capacity >= amount) {
                return true;
            }
        }

        return capacity >= amount;
    }

    private boolean isAdmin(Player player) {
        boolean opBypass = plugin.getConfig().getBoolean(
                "settings.admin.op-bypass",
                true
        );

        String permission = plugin.getConfig().getString(
                "settings.admin.permission",
                "vortexgems.admin"
        );

        return player.hasPermission(permission) || (opBypass && player.isOp());
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
                    "<yellow>Use /gemsadmin category create to create a category."
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
                player.sendMessage(ColorUtil.text(
                        "<green>Category deleted: <white>" + id
                ));
            } else {
                boolean enabled = plugin.getCategoryManager().isEnabled(id);
                plugin.getCategoryManager().setEnabled(id, !enabled);

                player.sendMessage(ColorUtil.text(
                        "<green>Category <white>" + id + " <green>is now " +
                                (!enabled ? "<green>enabled." : "<red>disabled.")
                ));
            }

            plugin.getMenuManager().openCategoryEditor(player);
            return;
        }
    }

    private void handleRewardEditor(Player player, int slot, ClickType click) {
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
                    "<yellow>Use /gemsadmin reward create to create a reward."
            ));
            return;
        }

        int index = 0;

        for (String id : plugin.getRewardManager().getRewardIds()) {
            if (index++ != slot) {
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
                        "<green>Reward <white>" + id + " <green>is now " +
                                (!enabled ? "<green>enabled." : "<red>disabled.")
                ));
            }

            plugin.getMenuManager().openRewardEditor(player);
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
                        "settings.transfer.enabled",
                        true
                );

                plugin.getConfig().set("settings.transfer.enabled", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 14 -> {
                boolean enabled = plugin.getConfig().getBoolean(
                        "settings.admin.op-bypass",
                        true
                );

                plugin.getConfig().set("settings.admin.op-bypass", !enabled);
                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }
            case 31 -> {
                plugin.reloadPlugin();
                player.sendMessage(ColorUtil.text("<green>VortexGems configuration reloaded."));
                plugin.getMenuManager().openSettings(player);
            }
            case 40 -> plugin.getMenuManager().openAdmin(player);
        }
    }
}
