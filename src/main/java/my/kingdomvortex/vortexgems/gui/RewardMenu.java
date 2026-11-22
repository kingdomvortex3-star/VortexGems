
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

public final class RewardMenu {

    private final VortexGems plugin;

    public RewardMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String categoryId) {
        int size = plugin.getConfig().getInt("gui.reward.size", 54);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        String categoryName = plugin.getCategoryManager().getName(categoryId);

        MenuHolder holder = new MenuHolder("REWARD", categoryId);

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text("<dark_gray>ʀᴇᴡᴀʀᴅѕ | " + categoryName)
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        for (String rewardId :
                plugin.getRewardManager().getRewardsInCategory(categoryId)) {

            if (!plugin.getRewardManager().isEnabled(rewardId)) {
                continue;
            }

            int slot = plugin.getRewardManager().getSlot(rewardId);

            if (slot < 0 || slot >= size || slot == size - 5) {
                continue;
            }

            ItemStack rewardItem =
                    plugin.getRewardManager().getItemStack(rewardId);

            if (rewardItem == null || rewardItem.getType().isAir()) {
                Material material =
                        plugin.getRewardManager().getMaterial(rewardId);

                if (material == null || material.isAir()) {
                    material = Material.CHEST;
                }

                rewardItem = new ItemStack(
                        material,
                        Math.max(1, plugin.getRewardManager().getAmount(rewardId))
                );
            } else {
                rewardItem = rewardItem.clone();
            }

            String rewardName =
                    plugin.getRewardManager().getName(rewardId);

            long price =
                    plugin.getRewardManager().getPrice(rewardId);

            List<String> lore =
                    plugin.getRewardManager().getLore(rewardId);

            ItemStack display = ItemUtil.named(
                    rewardItem,
                    "<gradient:#55ff55:#00aaaa><bold>" +
                            rewardName + "</bold></gradient>"
            );

            display = ItemUtil.withLore(
                    display,
                    lore == null || lore.isEmpty()
                            ? List.of(
                                    "",
                                    "<gray>ᴘʀɪᴄᴇ: <aqua>" +
                                            ColorUtil.formatGems(price) + " ɢᴇᴍѕ",
                                    "",
                                    "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴘᴜʀᴄʜᴀѕᴇ"
                            )
                            : lore
            );

            inventory.setItem(slot, display);
        }

        inventory.setItem(size - 5, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>ʙᴀᴄᴋ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴄᴀᴛᴇɢᴏʀɪᴇѕ"
                )
        ));

        player.openInventory(inventory);
    }
}
