
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class RewardMenu {

    private final VortexGems plugin;
    private final String category;

    public RewardMenu(VortexGems plugin, String category) {
        this.plugin = plugin;
        this.category = category;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("gui.reward.size", 54);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        String categoryName = plugin.getCategoryManager().getName(category);

        MenuHolder holder = new MenuHolder("REWARD", category);

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(
                        config.getString(
                                "gui.reward.title",
                                "<dark_gray>{category}"
                        ).replace("{category}", categoryName)
                )
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        for (String id : plugin.getRewardManager()
                .getRewardsInCategory(category)) {

            int slot = plugin.getRewardManager().getSlot(id);

            if (slot < 0 || slot >= size || slot == size - 5) {
                continue;
            }

            ItemStack reward = plugin.getRewardManager().getItemStack(id);

            if (reward == null || reward.getType().isAir()) {
                Material material = plugin.getRewardManager().getMaterial(id);

                if (material == null || material.isAir()) {
                    continue;
                }

                reward = new ItemStack(
                        material,
                        plugin.getRewardManager().getAmount(id)
                );
            } else {
                reward = reward.clone();
            }

            ItemMeta meta = reward.getItemMeta();

            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore =
                        new ArrayList<>();

                if (meta.lore() != null) {
                    lore.addAll(meta.lore());
                }

                lore.add(ColorUtil.text(""));
                lore.add(ColorUtil.text(
                        "<dark_gray>━━━━━━━━━━━━━━━━"
                ));
                lore.add(ColorUtil.text(
                        "<gray>Price: <aqua><bold>" +
                                ColorUtil.formatGems(
                                        plugin.getRewardManager().getPrice(id)
                                )
                ));
                lore.add(ColorUtil.text(
                        "<yellow>Click to purchase"
                ));

                meta.lore(lore);

                reward.setItemMeta(meta);
            }

            inventory.setItem(slot, reward);
        }

        inventory.setItem(
                size - 5,
                ItemUtil.create(
                        Material.ARROW,
                        1,
                        "<yellow><bold>BACK</bold>",
                        List.of(
                                "",
                                "<gray>Return to categories."
                        )
                )
        );

        player.openInventory(inventory);
    }
}
