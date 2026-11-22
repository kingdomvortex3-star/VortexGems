
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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
                ColorUtil.text(config.getString(
                        "gui.reward.title",
                        "<dark_gray>{category}"
                ).replace("{category}", categoryName))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        for (String id : plugin.getRewardManager().getRewardsInCategory(category)) {
            int slot = plugin.getRewardManager().getSlot(id);

            if (slot < 0 || slot >= size || slot == size - 5) {
                continue;
            }

            Material material = plugin.getRewardManager().getMaterial(id);
            int amount = plugin.getRewardManager().getAmount(id);
            String name = plugin.getRewardManager().getName(id);
            long price = plugin.getRewardManager().getPrice(id);

            List<String> lore = new ArrayList<>(
                    plugin.getRewardManager().getLore(id)
            );

            lore.add("");
            lore.add("<gray>Price: <aqua>" + ColorUtil.formatGems(price));
            lore.add("<yellow>Click to purchase");

            inventory.setItem(slot, ItemUtil.create(
                    material,
                    amount,
                    name,
                    lore
            ));
        }

        inventory.setItem(size - 5, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>BACK</bold>",
                List.of(
                        "",
                        "<gray>Return to categories."
                )
        ));

        player.openInventory(inventory);
    }
}
