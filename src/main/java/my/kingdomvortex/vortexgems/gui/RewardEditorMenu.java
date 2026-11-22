
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

        // Category harus ditempatkan pada slot yang sama
        // dengan indeks yang diperiksa MenuListener.
        int slot = 0;

        for (String categoryId : plugin.getCategoryManager().getCategoryIds()) {
            if (slot >= 45) {
                break;
            }

            Material material =
                    plugin.getCategoryManager().getMaterial(categoryId);

            if (material == null || material.isAir()) {
                material = Material.CHEST;
            }

            String name =
                    plugin.getCategoryManager().getName(categoryId);

            inventory.setItem(slot++, ItemUtil.create(
                    material,
                    1,
                    "<gradient:#55ff55:#00aaaa><bold>"
                            + name + "</bold></gradient>",
                    List.of(
                            "",
                            "<gray>ɪᴅ: <white>" + categoryId,
                            "<gray>ᴍᴀɴᴀɢᴇ ʀᴇᴡᴀʀᴅѕ",
                            "",
                            "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                    )
            ));
        }

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>ʙᴀᴄᴋ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ"
                )
        ));

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String categoryId) {
        if (!plugin.getCategoryManager().exists(categoryId)) {
            player.sendMessage(ColorUtil.text(
                    "<red>Category not found."
            ));
            open(player);
            return;
        }

        String categoryName =
                plugin.getCategoryManager().getName(categoryId);

        // Penting: gunakan REWARD_CATEGORY agar MenuListener
        // memanggil handleRewardCategory(), bukan handleRewardEditor().
        MenuHolder holder = new MenuHolder(
                "REWARD_CATEGORY",
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

        for (String rewardId : plugin.getRewardManager().getRewardIds()) {

            if (!plugin.getRewardManager()
                    .getCategory(rewardId)
                    .equalsIgnoreCase(categoryId)) {
                continue;
            }

            int rewardSlot =
                    plugin.getRewardManager().getSlot(rewardId);

            // Slot 45 = ADD REWARD
            // Slot 49 = BACK
            if (rewardSlot < 0
                    || rewardSlot >= inventory.getSize()
                    || rewardSlot == 45
                    || rewardSlot == 49) {
                continue;
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
                    "<gradient:#55ff55:#00aaaa><bold>"
                            + rewardName + "</bold></gradient>"
            );

            item = ItemUtil.withLore(
                    item,
                    List.of(
                            "",
                            "<gray>ɪᴅ: <white>" + rewardId,
                            "<gray>ᴘʀɪᴄᴇ: <aqua>"
                                    + ColorUtil.formatGems(price) + " ɢᴇᴍѕ",
                            "<gray>ѕᴛᴀᴛᴜѕ: "
                                    + (enabled
                                    ? "<green>ᴇɴᴀʙʟᴇᴅ"
                                    : "<red>ᴅɪѕᴀʙʟᴇᴅ"),
                            "",
                            "<yellow>ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ",
                            "<gray>ᴍɪᴅᴅʟᴇ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ"
                    )
            );

            inventory.setItem(rewardSlot, item);
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
