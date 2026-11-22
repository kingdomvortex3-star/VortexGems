
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

public final class RewardEditorMenu {

    private final VortexGems plugin;

    public RewardEditorMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder("REWARD_EDITOR");

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ColorUtil.text("<dark_gray>ᴠᴏʀᴛᴇx | ʀᴇᴡᴀʀᴅ ᴍᴀɴᴀɢᴇʀ")
        );

        holder.setInventory(inventory);
        fill(inventory);

        int slot = 10;

        for (String categoryId :
                plugin.getCategoryManager().getCategoryIds()) {

            if (slot >= 44) {
                break;
            }

            Material material =
                    plugin.getCategoryManager().getMaterial(categoryId);

            if (material == null || material.isAir()) {
                material = Material.CHEST;
            }

            String name =
                    plugin.getCategoryManager().getName(categoryId);

            inventory.setItem(slot, ItemUtil.create(
                    material,
                    1,
                    "<gradient:#55ff55:#00aaaa><bold>" +
                            name + "</bold></gradient>",
                    List.of(
                            "",
                            "<gray>ɪᴅ: <white>" + categoryId,
                            "<gray>ᴍᴀɴᴀɢᴇ ʀᴇᴡᴀʀᴅѕ",
                            "",
                            "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                    )
            ));

            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>ʙᴀᴄᴋ</bold>",
                List.of("", "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ")
        ));

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String categoryId) {
        String categoryName =
                plugin.getCategoryManager().getName(categoryId);

        MenuHolder holder = new MenuHolder(
                "REWARD_EDITOR",
                categoryId
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ColorUtil.text(
                        "<dark_gray>ʀᴇᴡᴀʀᴅ ᴇᴅɪᴛᴏʀ | " + categoryName
                )
        );

        holder.setInventory(inventory);
        fill(inventory);

        int slot = 10;

        for (String rewardId :
                plugin.getRewardManager().getRewardsInCategory(categoryId)) {

            if (slot >= 44) {
                break;
            }

            ItemStack item =
                    plugin.getRewardManager().getItemStack(rewardId);

            if (item == null || item.getType().isAir()) {
                Material material =
                        plugin.getRewardManager().getMaterial(rewardId);

                if (material == null || material.isAir()) {
                    material = Material.CHEST;
                }

                item = new ItemStack(
                        material,
                        Math.max(
                                1,
                                plugin.getRewardManager().getAmount(rewardId)
                        )
                );
            } else {
                item = item.clone();
            }

            String rewardName =
                    plugin.getRewardManager().getName(rewardId);

            long price =
                    plugin.getRewardManager().getPrice(rewardId);

            boolean enabled =
                    plugin.getRewardManager().isEnabled(rewardId);

            item = ItemUtil.named(
                    item,
                    "<gradient:#55ff55:#00aaaa><bold>" +
                            rewardName + "</bold></gradient>"
            );

            item = ItemUtil.withLore(
                    item,
                    List.of(
                            "",
                            "<gray>ɪᴅ: <white>" + rewardId,
                            "<gray>ᴘʀɪᴄᴇ: <aqua>" +
                                    ColorUtil.formatGems(price) + " ɢᴇᴍѕ",
                            "<gray>ѕᴛᴀᴛᴜѕ: " +
                                    (enabled
                                            ? "<green>ᴇɴᴀʙʟᴇᴅ"
                                            : "<red>ᴅɪѕᴀʙʟᴇᴅ"),
                            "",
                            "<yellow>ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ",
                            "<gray>ᴍɪᴅᴅʟᴇ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ",
                            "<red>ѕʜɪꜰᴛ + ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ"
                    )
            );

            inventory.setItem(slot, item);

            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        inventory.setItem(45, ItemUtil.create(
                Material.EMERALD,
                1,
                "<green><bold>ᴀᴅᴅ ʀᴇᴡᴀʀᴅ</bold>",
                List.of(
                        "",
                        "<gray>ᴄʀᴇᴀᴛᴇ ᴀ ɴᴇᴡ ɪᴛᴇᴍ ʀᴇᴡᴀʀᴅ",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴄʀᴇᴀᴛᴇ"
                )
        ));

        inventory.setItem(49, ItemUtil.create(
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

    private void fill(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }
    }
}
