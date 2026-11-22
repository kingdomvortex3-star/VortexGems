
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

public final class ConfirmMenu {

    private final VortexGems plugin;

    public ConfirmMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String rewardId) {
        String rewardName =
                plugin.getRewardManager().getName(rewardId);

        long price =
                plugin.getRewardManager().getPrice(rewardId);

        long balance =
                plugin.getGemsManager().getBalance(player.getUniqueId());

        MenuHolder holder = new MenuHolder("CONFIRM", rewardId);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                ColorUtil.text("<dark_gray>ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemUtil.filler());
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

        ItemStack preview = ItemUtil.named(
                rewardItem,
                "<gradient:#55ff55:#00aaaa><bold>" +
                        rewardName + "</bold></gradient>"
        );

        preview = ItemUtil.withLore(
                preview,
                List.of(
                        "",
                        "<gray>ʀᴇᴡᴀʀᴅ: <white>" + rewardName,
                        "<gray>ᴘʀɪᴄᴇ: <aqua>" +
                                ColorUtil.formatGems(price) + " ɢᴇᴍѕ",
                        "<gray>ʏᴏᴜʀ ʙᴀʟᴀɴᴄᴇ: <aqua>" +
                                ColorUtil.formatGems(balance) + " ɢᴇᴍѕ",
                        "",
                        balance >= price
                                ? "<green>ʏᴏᴜ ᴄᴀɴ ᴀꜰꜰᴏʀᴅ ᴛʜɪѕ ʀᴇᴡᴀʀᴅ"
                                : "<red>ɪɴѕᴜꜰꜰɪᴄɪᴇɴᴛ ɢᴇᴍѕ"
                )
        );

        inventory.setItem(13, preview);

        inventory.setItem(11, ItemUtil.create(
                balance >= price ? Material.LIME_STAINED_GLASS_PANE : Material.BARRIER,
                1,
                balance >= price
                        ? "<green><bold>ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ</bold>"
                        : "<red><bold>ɪɴѕᴜꜰꜰɪᴄɪᴇɴᴛ ɢᴇᴍѕ</bold>",
                List.of(
                        "",
                        balance >= price
                                ? "<gray>ᴄʟɪᴄᴋ ᴛᴏ ᴄᴏɴꜰɪʀᴍ"
                                : "<gray>ʏᴏᴜ ɴᴇᴇᴅ ᴍᴏʀᴇ ɢᴇᴍѕ"
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.RED_STAINED_GLASS_PANE,
                1,
                "<red><bold>ᴄᴀɴᴄᴇʟ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ʀᴇᴡᴀʀᴅѕ"
                )
        ));

        player.openInventory(inventory);
    }
}
