
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardCreateMenu {

    private static final Map<UUID, Long> PRICES = new ConcurrentHashMap<>();

    private final VortexGems plugin;

    public RewardCreateMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String category) {
        if (!plugin.getCategoryManager().exists(category)) {
            player.sendMessage(ColorUtil.text("<red>Category not found."));
            plugin.getMenuManager().openRewardEditor(player);
            return;
        }

        MenuHolder holder = new MenuHolder("REWARD_CREATE", category);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                ColorUtil.text("<dark_gray>VORTEX - CREATE REWARD")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 27; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        inventory.setItem(13, ItemUtil.create(
                Material.HOPPER,
                1,
                "<yellow><bold>REWARD ITEM</bold>",
                List.of(
                        "",
                        "<gray>Place the item you want to sell here.",
                        "<gray>Its enchantments, name and lore",
                        "<gray>will be saved.",
                        "",
                        "<yellow>Place an item in this slot."
                )
        ));

        inventory.setItem(11, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>SET GEMS PRICE</bold>",
                List.of(
                        "",
                        "<gray>Current price: <aqua>" +
                                PRICES.getOrDefault(player.getUniqueId(), 0L) +
                                " Gems",
                        "",
                        "<yellow>Click to enter the price in chat."
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>SAVE REWARD</bold>",
                List.of(
                        "",
                        "<gray>Save this item to the selected category.",
                        "",
                        "<yellow>Click to save."
                )
        ));

        inventory.setItem(22, ItemUtil.create(
                Material.BARRIER,
                1,
                "<red><bold>CANCEL</bold>",
                List.of(
                        "",
                        "<gray>Cancel reward creation."
                )
        ));

        player.openInventory(inventory);
    }

    public void promptPrice(Player player, String category) {
        plugin.getInputManager().request(player, input -> {
            long price;

            try {
                price = Long.parseLong(input);
            } catch (NumberFormatException exception) {
                player.sendMessage(ColorUtil.text(
                        "<red>Invalid price. Enter a whole number."
                ));
                open(player, category);
                return;
            }

            if (price < 0) {
                player.sendMessage(ColorUtil.text(
                        "<red>Price cannot be negative."
                ));
                open(player, category);
                return;
            }

            PRICES.put(player.getUniqueId(), price);

            player.sendMessage(ColorUtil.text(
                    "<green>Reward price set to <aqua>" + price + " Gems<green>."
            ));

            open(player, category);
        });
    }

    public boolean save(Player player, Inventory inventory, String category) {
        ItemStack item = inventory.getItem(13);

        if (item == null
                || item.getType().isAir()
                || item.getType() == Material.HOPPER) {
            player.sendMessage(ColorUtil.text(
                    "<red>Place a valid reward item in the item slot first."
            ));
            return false;
        }

        long price = PRICES.getOrDefault(player.getUniqueId(), -1L);

        if (price < 0) {
            player.sendMessage(ColorUtil.text(
                    "<red>Please set the Gems price first."
            ));
            return false;
        }

        String baseId = item.getType().name().toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");

        String id = baseId;
        int number = 2;

        while (plugin.getRewardManager().exists(id)) {
            id = baseId + "_" + number++;
        }

        ItemStack storedItem = item.clone();

        boolean created = plugin.getRewardManager().createItemReward(
                id,
                category,
                storedItem,
                price,
                0
        );

        if (!created) {
            player.sendMessage(ColorUtil.text(
                    "<red>Failed to save this reward."
            ));
            return false;
        }

        PRICES.remove(player.getUniqueId());

        player.sendMessage(ColorUtil.text(
                "<green>Reward <white>" + id +
                        " <green>saved successfully for <aqua>" +
                        price + " Gems<green>."
        ));

        return true;
    }

    public static void clearPrice(UUID uuid) {
        PRICES.remove(uuid);
    }
}
