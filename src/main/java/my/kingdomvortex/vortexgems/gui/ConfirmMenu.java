
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ConfirmMenu {

    private final VortexGems plugin;
    private final String rewardId;

    public ConfirmMenu(VortexGems plugin, String rewardId) {
        this.plugin = plugin;
        this.rewardId = rewardId;
    }

    public void open(Player player) {
        if (!plugin.getRewardManager().exists(rewardId)
                || !plugin.getRewardManager().isEnabled(rewardId)) {
            player.sendMessage(ColorUtil.text("<red>Reward is unavailable."));
            return;
        }

        String title = plugin.getConfig().getString(
                "gui.confirm.title",
                "<dark_gray>Confirm Purchase"
        );

        MenuHolder holder = new MenuHolder("CONFIRM", rewardId);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                ColorUtil.text(title)
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        ItemStack preview = plugin.getRewardManager().getItemStack(rewardId);

        if (preview == null || preview.getType().isAir()) {
            Material material = plugin.getRewardManager().getMaterial(rewardId);

            if (material == null || material.isAir()) {
                material = Material.CHEST;
            }

            preview = new ItemStack(
                    material,
                    plugin.getRewardManager().getAmount(rewardId)
            );
        } else {
            preview = preview.clone();
        }

        long price = plugin.getRewardManager().getPrice(rewardId);
        long balance = plugin.getGemsManager().getBalance(
                player.getUniqueId()
        );

        ItemMeta meta = preview.getItemMeta();

        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            if (meta.lore() != null) {
                lore.addAll(meta.lore());
            }

            lore.add(ColorUtil.text(""));
            lore.add(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━"));
            lore.add(ColorUtil.text(
                    "<gray>Price: <aqua><bold>" +
                            ColorUtil.formatGems(price) + " Gems"
            ));
            lore.add(ColorUtil.text(
                    "<gray>Your balance: <aqua>" +
                            ColorUtil.formatGems(balance) + " Gems"
            ));

            if (balance >= price) {
                lore.add(ColorUtil.text("<green>You can afford this reward."));
            } else {
                lore.add(ColorUtil.text("<red>You don't have enough Gems."));
            }

            meta.lore(lore);
            preview.setItemMeta(meta);
        }

        inventory.setItem(13, preview);

        inventory.setItem(11, ItemUtil.create(
                Material.LIME_STAINED_GLASS_PANE,
                1,
                "<green><bold>CONFIRM PURCHASE</bold>",
                List.of(
                        "",
                        "<gray>Purchase this reward for:",
                        "<aqua>" + ColorUtil.formatGems(price) + " Gems",
                        "",
                        "<yellow>Click to confirm."
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.RED_STAINED_GLASS_PANE,
                1,
                "<red><bold>CANCEL</bold>",
                List.of(
                        "",
                        "<gray>Return without purchasing."
                )
        ));

        player.openInventory(inventory);
    }
}
