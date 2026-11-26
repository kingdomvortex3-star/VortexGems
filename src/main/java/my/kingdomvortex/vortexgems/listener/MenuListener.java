
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
import org.bukkit.event.inventory.InventoryDragEvent;
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
            case "CATEGORY_EDITOR" ->
                    handleCategoryEditor(player, slot, event.getClick());
            case "REWARD_EDITOR" -> handleRewardEditor(player, slot);
            case "REWARD_CATEGORY" ->
                    handleRewardCategory(player, slot, event.getClick(), holder);
            case "SETTINGS" -> handleSettings(player, slot);
            default -> player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        if (holder.getMenu().equalsIgnoreCase("REWARD_CREATE")) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleRewardCreate(
            InventoryClickEvent event,
            Player player,
            Inventory inventory,
            MenuHolder holder
    ) {
        String category = holder.getData();

        if (event.getClickedInventory() == player.getInventory()) {
            if (event.isShiftClick()
                    || event.getClick() == ClickType.NUMBER_KEY
                    || event.getClick() == ClickType.DOUBLE_CLICK
                    || event.getClick() == ClickType.SWAP_OFFHAND) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getClickedInventory() != inventory) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == 13) {
            if (event.getClick() != ClickType.LEFT
                    && event.getClick() != ClickType.RIGHT) {
                return;
            }

            ItemStack cursor = event.getCursor();
            ItemStack current = inventory.getItem(13);

            boolean hasCursor = cursor != null
                    && !cursor.getType().isAir()
                    && cursor.getAmount() > 0;

            boolean hasCurrent = current != null
                    && !current.getType().isAir()
                    && current.getType() != Material.HOPPER;

            if (!hasCursor) {
                if (!hasCurrent) {
                    return;
                }

                event.setCursor(current.clone());
                inventory.setItem(13, new ItemStack(Material.HOPPER));
                return;
            }

            if (cursor.getType() == Material.HOPPER) {
                return;
            }

            inventory.setItem(13, cursor.clone());
            event.setCursor(hasCurrent ? current.clone() : null);
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

    my.kingdomvortex.vortexgems.gui.BuyMenu buyMenu =
            new my.kingdomvortex.vortexgems.gui.BuyMenu(plugin);

    if (slot == 20) {
        buyMenu.decrease(player);
        plugin.getMenuManager().openBuy(player);
        return;
    }

    if (slot == 24) {
        buyMenu.increase(player);
        plugin.getMenuManager().openBuy(player);
        return;
    }

    if (slot != 22) {
        return;
    }

    long amount = buyMenu.getSelectedAmount(player);

    if (!plugin.getEconomyManager().buyGems(player, amount)) {
        player.sendMessage(ColorUtil.text(
                "<red>Purchase failed. Check your money and purchase settings."
        ));
        return;
    }

    player.sendMessage(ColorUtil.text(
            "<green>You purchased <aqua>"
                    + ColorUtil.formatGems(amount)
                    + " Gems<green>."
    ));

    buyMenu.resetSelection(player);
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

        for (String id :
                plugin.getRewardManager().getRewardsInCategory(category)) {
            if (plugin.getRewardManager().getSlot(id) == slot) {
                plugin.getMenuManager().openConfirm(player, id);
                return;
            }
        }
    }

    private void handleConfirm(Player player, int slot, MenuHolder holder) {
        String rewardId = holder.getData();

        if (rewardId == null
                || !plugin.getRewardManager().exists(rewardId)) {
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

        if (slot != 11
                || !plugin.getRewardManager().isEnabled(rewardId)) {
            return;
        }

        long price = plugin.getRewardManager().getPrice(rewardId);
        String type = plugin.getRewardManager().getType(rewardId);

        if (price < 0) {
            player.sendMessage(ColorUtil.text("<red>Invalid reward price."));
            return;
        }

        if ("ITEM".equalsIgnoreCase(type)) {
            ItemStack reward =
                    plugin.getRewardManager().getItemStack(rewardId);

            if (reward == null || !canFit(player, reward)) {
                player.sendMessage(ColorUtil.text(
                        "<red>Invalid reward or your inventory is full."
                ));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text(
                        "<red>You don't have enough Gems."
                ));
                return;
            }

            player.getInventory().addItem(reward);

        } else if ("COMMAND".equalsIgnoreCase(type)) {
            List<String> commands =
                    plugin.getRewardManager().getCommands(rewardId);

            if (commands == null || commands.isEmpty()) {
                player.sendMessage(ColorUtil.text(
                        "<red>No reward commands configured."
                ));
                return;
            }

            if (price > 0 && !plugin.getGemsManager().removeBalance(
                    player.getUniqueId(), price
            )) {
                player.sendMessage(ColorUtil.text(
                        "<red>You don't have enough Gems."
                ));
                return;
            }

            for (String command : commands) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command.replace("{player}", player.getName())
                                .replace(
                                        "{uuid}",
                                        player.getUniqueId().toString()
                                )
                );
            }

        } else {
            player.sendMessage(ColorUtil.text(
                    "<red>Unsupported reward type."
            ));
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

        player.sendMessage(ColorUtil.text(
                "<green>Reward purchased successfully!"
        ));

        player.closeInventory();
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();

        for (ItemStack content :
                player.getInventory().getStorageContents()) {
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
                player.sendMessage(ColorUtil.text(
                        "<green>VortexGems reloaded."
                ));
                plugin.getMenuManager().openAdmin(player);
            }
        }
    }

    private void handleCategoryEditor(
            Player player,
            int slot,
            ClickType click
    ) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            plugin.getMenuManager().openAdmin(player);
            return;
        }

        if (slot == 45) {
            createCategory(player);
            return;
        }

        if (slot == 53) {
            player.sendMessage(ColorUtil.text(
                    "<aqua><bold>CATEGORY GUIDE"
            ));
            player.sendMessage(ColorUtil.text(
                    "<gray>Left: change slot | Shift + Left: change icon"
            ));
            player.sendMessage(ColorUtil.text(
                    "<gray>Right: delete | Middle: toggle"
            ));
            return;
        }

        int index = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (index++ != slot) {
                continue;
            }

            if (click == ClickType.RIGHT) {
                String categoryName =
                        plugin.getCategoryManager().getName(id);

                plugin.getCategoryManager().delete(id);

                player.sendMessage(ColorUtil.text(
                        "<red>Category <white>"
                                + categoryName
                                + " <red>deleted."
                ));

                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            if (click == ClickType.MIDDLE) {
                boolean enabled = plugin.getCategoryManager().isEnabled(id);
                plugin.getCategoryManager().setEnabled(id, !enabled);

                player.sendMessage(ColorUtil.text(
                        enabled
                                ? "<red>Category disabled."
                                : "<green>Category enabled."
                ));

                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            // SHIFT + LEFT CLICK: Change category icon.
            if (click == ClickType.SHIFT_LEFT) {
                ItemStack hand =
                        player.getInventory().getItemInMainHand();

                if (hand == null || hand.getType().isAir()) {
                    player.sendMessage(ColorUtil.text(
                            "<red>Hold an item in your main hand first."
                    ));
                    return;
                }

                Material newMaterial = hand.getType();

                boolean changed = plugin.getCategoryManager().setMaterial(
                        id,
                        newMaterial
                );

                if (changed) {
                    player.sendMessage(ColorUtil.text(
                            "<green>Category icon changed to <white>"
                                    + newMaterial.name()
                                    + "<green>."
                    ));
                } else {
                    player.sendMessage(ColorUtil.text(
                            "<red>Failed to change category icon."
                    ));
                }

                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            if (click == ClickType.LEFT) {
                editCategorySlot(player, id);
                return;
            }

            return;
        }
    }

    private void createCategory(Player player) {
        player.closeInventory();

        player.sendMessage(ColorUtil.text(
                "<gradient:#00E5FF:#8A2BE2><bold>CREATE CATEGORY</bold></gradient>"
        ));
        player.sendMessage(ColorUtil.text(
                "<gray>Enter the category name in chat."
        ));
        player.sendMessage(ColorUtil.text(
                "<dark_gray>Example: <white>wakwau"
        ));
        player.sendMessage(ColorUtil.text(
                "<gray>Type <red>cancel <gray>to cancel."
        ));

        plugin.getInputManager().request(player, input -> {
            if (!isAdmin(player)) {
                return;
            }

            String name = input.trim();

            if (name.equalsIgnoreCase("cancel")) {
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            if (name.isBlank()) {
                player.sendMessage(ColorUtil.text(
                        "<red>Category name cannot be empty."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            String id = name.toLowerCase()
                    .replaceAll("[^a-z0-9_-]+", "_")
                    .replaceAll("^_+|_+$", "");

            if (id.isBlank() || plugin.getCategoryManager().exists(id)) {
                player.sendMessage(ColorUtil.text(
                        "<red>Invalid or existing category ID."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            int size = getCategoryMenuSize();
            int backSlot = size - 5;
            int freeSlot = -1;

            for (int i = 0; i < size; i++) {
                if (i == backSlot || isCategorySlotOccupied(i, null)) {
                    continue;
                }

                freeSlot = i;
                break;
            }

            if (freeSlot == -1) {
                player.sendMessage(ColorUtil.text(
                        "<red>No free category slots available."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            boolean created = plugin.getCategoryManager().create(
                    id,
                    name,
                    Material.CHEST,
                    freeSlot
            );

            player.sendMessage(ColorUtil.text(
                    created
                            ? "<green>Category <aqua>" + name + " <green>created successfully."
                            : "<red>Failed to create category."
            ));

            plugin.getMenuManager().openCategoryEditor(player);
        });
    }

    private void editCategorySlot(Player player, String categoryId) {
        String categoryName =
                plugin.getCategoryManager().getName(categoryId);

        int currentSlot =
                plugin.getCategoryManager().getSlot(categoryId) + 1;

        int size = getCategoryMenuSize();
        int backSlot = size - 5;

        player.closeInventory();

        player.sendMessage(ColorUtil.text(
                "<gradient:#00E5FF:#8A2BE2><bold>EDIT CATEGORY SLOT</bold></gradient>"
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Category: <aqua>" + categoryName
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Current slot: <yellow>" + currentSlot
        ));

        player.sendMessage(ColorUtil.text(
                "<yellow>Enter a new slot number in chat."
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Allowed: <aqua>1 - " + size
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Slot <red>" + (backSlot + 1)
                        + " <gray>is reserved for the Back button."
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Type <red>cancel <gray>to cancel."
        ));

        plugin.getInputManager().request(player, input -> {
            if (!isAdmin(player)) {
                return;
            }

            if (!plugin.getCategoryManager().exists(categoryId)) {
                player.sendMessage(ColorUtil.text(
                        "<red>Category no longer exists."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            int requestedSlot;

            try {
                requestedSlot = Integer.parseInt(input.trim());
            } catch (NumberFormatException exception) {
                player.sendMessage(ColorUtil.text(
                        "<red>Please enter a valid slot number."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            if (requestedSlot < 1 || requestedSlot > size) {
                player.sendMessage(ColorUtil.text(
                        "<red>Slot must be between 1 and " + size + "."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            int newSlot = requestedSlot - 1;

            if (newSlot == backSlot) {
                player.sendMessage(ColorUtil.text(
                        "<red>This slot is reserved for the Back button."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            if (isCategorySlotOccupied(newSlot, categoryId)) {
                player.sendMessage(ColorUtil.text(
                        "<red>That slot is already used by another category."
                ));
                plugin.getMenuManager().openCategoryEditor(player);
                return;
            }

            boolean updated =
                    plugin.getCategoryManager().setSlot(categoryId, newSlot);

            if (updated) {
                player.sendMessage(ColorUtil.text(
                        "<green>Category <aqua>" + categoryName
                                + " <green>moved to slot <yellow>"
                                + requestedSlot + "<green>."
                ));
            } else {
                player.sendMessage(ColorUtil.text(
                        "<red>Failed to update category slot."
                ));
            }

            plugin.getMenuManager().openCategoryEditor(player);
        });
    }

    private int getCategoryMenuSize() {
        int size = plugin.getConfig().getInt("gui.category.size", 54);

        if (size < 9 || size > 54 || size % 9 != 0) {
            return 54;
        }

        return size;
    }

    private boolean isCategorySlotOccupied(int slot, String exceptId) {
        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (exceptId != null && id.equalsIgnoreCase(exceptId)) {
                continue;
            }

            if (plugin.getCategoryManager().getSlot(id) == slot) {
                return true;
            }
        }

        return false;
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

        for (String category :
                plugin.getCategoryManager().getCategoryIds()) {
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
                boolean enabled =
                        plugin.getRewardManager().isEnabled(id);

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
            case 10 -> editGemPrice(player);

            case 12 -> {
                boolean enabled = plugin.getConfig().getBoolean(
                        "settings.transfer.enabled", true
                );

                plugin.getConfig().set(
                        "settings.transfer.enabled", !enabled
                );

                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }

            case 14 -> {
                boolean enabled = plugin.getConfig().getBoolean(
                        "settings.admin.op-bypass", true
                );

                plugin.getConfig().set(
                        "settings.admin.op-bypass", !enabled
                );

                plugin.saveConfig();
                plugin.getMenuManager().openSettings(player);
            }

            case 16 -> editLeaderboardLimit(player);

            case 31 -> {
                plugin.reloadPlugin();

                player.sendMessage(ColorUtil.text(
                        "<green>VortexGems configuration reloaded."
                ));

                plugin.getMenuManager().openSettings(player);
            }

            case 40 -> plugin.getMenuManager().openAdmin(player);
        }
    }

    private void editGemPrice(Player player) {
        if (!isAdmin(player)) {
            player.closeInventory();
            return;
        }

        player.closeInventory();

        player.sendMessage(ColorUtil.text(
                "<gold><bold>GEMS ECONOMY"
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Current price: <yellow>"
                        + plugin.getEconomyManager().getPricePerGem()
                        + " money <gray>per 1 Gem."
        ));

        player.sendMessage(ColorUtil.text(
                "<yellow>Enter the new price in chat."
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Type <red>cancel <gray>to cancel."
        ));

        plugin.getInputManager().request(player, input -> {
            if (!isAdmin(player)) {
                return;
            }

            if (input.trim().equalsIgnoreCase("cancel")) {
                plugin.getMenuManager().openSettings(player);
                return;
            }

            double price;

            try {
                price = Double.parseDouble(input.replace(",", "").trim());
            } catch (NumberFormatException exception) {
                player.sendMessage(ColorUtil.text(
                        "<red>Invalid price. Please enter a number."
                ));
                plugin.getMenuManager().openSettings(player);
                return;
            }

            if (!Double.isFinite(price) || price <= 0) {
                player.sendMessage(ColorUtil.text(
                        "<red>Price must be greater than zero."
                ));
                plugin.getMenuManager().openSettings(player);
                return;
            }

            plugin.getConfig().set(
                    "settings.economy.price-per-gem",
                    price
            );

            plugin.saveConfig();

            player.sendMessage(ColorUtil.text(
                    "<green>Gem price updated to <yellow>"
                            + price + " money <green>per Gem."
            ));

            plugin.getMenuManager().openSettings(player);
        });
    }

    private void editLeaderboardLimit(Player player) {
        player.closeInventory();

        player.sendMessage(ColorUtil.text(
                "<yellow>Enter the new leaderboard limit in chat."
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Type <red>cancel <gray>to cancel."
        ));

        plugin.getInputManager().request(player, input -> {
            if (!isAdmin(player)) {
                return;
            }

            if (input.trim().equalsIgnoreCase("cancel")) {
                plugin.getMenuManager().openSettings(player);
                return;
            }

            int limit;

            try {
                limit = Integer.parseInt(input.trim());
            } catch (NumberFormatException exception) {
                player.sendMessage(ColorUtil.text(
                        "<red>Please enter a valid whole number."
                ));
                plugin.getMenuManager().openSettings(player);
                return;
            }

            if (limit < 1 || limit > 100) {
                player.sendMessage(ColorUtil.text(
                        "<red>Limit must be between 1 and 100."
                ));
                plugin.getMenuManager().openSettings(player);
                return;
            }

            plugin.getConfig().set(
                    "settings.leaderboard.limit",
                    limit
            );

            plugin.saveConfig();

            player.sendMessage(ColorUtil.text(
                    "<green>Leaderboard limit updated to <yellow>"
                            + limit + "<green>."
            ));

            plugin.getMenuManager().openSettings(player);
        });
    }
}

