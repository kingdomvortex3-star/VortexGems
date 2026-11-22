
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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
            player.sendMessage(ColorUtil.text("<red>Reward tidak tersedia."));
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

        Material material = plugin.getRewardManager().getMaterial(rewardId);
        int amount = plugin.getRewardManager().getAmount(rewardId);
        String name = plugin.getRewardManager().getName(rewardId);
        long price = plugin.getRewardManager().getPrice(rewardId);

        inventory.setItem(13, ItemUtil.create(
                material,
                amount,
                name,
                List.of(
                        "",
                        "<gray>Price: <aqua>" + ColorUtil.formatGems(price),
                        "<gray>Your balance: <aqua>" +
                                ColorUtil.formatGems(
                                        plugin.getGemsManager().getBalance(
                                                player.getUniqueId()
                                        )
                                )
                )
        ));

        inventory.setItem(11, ItemUtil.create(
                Material.LIME_STAINED_GLASS_PANE,
                1,
                "<green><bold>CONFIRM PURCHASE</bold>",
                List.of(
                        "",
                        "<gray>Click to confirm your purchase."
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
