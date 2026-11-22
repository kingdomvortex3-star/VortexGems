
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
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
                "VORTEX - REWARD MANAGER"
        );

        holder.setInventory(inventory);

        fill(inventory);

        int slot = 0;

        for (String category : plugin.getCategoryManager().getCategoryIds()) {
            if (slot >= 45) {
                break;
            }

            inventory.setItem(slot++, ItemUtil.create(
                    plugin.getCategoryManager().getMaterial(category),
                    1,
                    plugin.getCategoryManager().getName(category),
                    List.of(
                            "",
                            "<gray>Category: <white>" + category,
                            "<gray>Rewards: <aqua>" +
                                    plugin.getRewardManager()
                                            .getRewardsInCategory(category).size(),
                            "",
                            "<yellow>Click to manage rewards"
                    )
            ));
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>CREATE REWARD</bold>",
                List.of(
                        "",
                        "<gray>Select a category first.",
                        "<gray>Then place an item into the editor.",
                        "",
                        "<yellow>Click to begin"
                )
        ));

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>BACK</bold>",
                List.of(
                        "",
                        "<gray>Return to admin menu."
                )
        ));

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String category) {
        if (!plugin.getCategoryManager().exists(category)) {
            player.sendMessage(
                    "§cCategory does not exist."
            );
            open(player);
            return;
        }

        MenuHolder holder = new MenuHolder(
                "REWARD_CATEGORY",
                category
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                "VORTEX - " + category.toUpperCase()
        );

        holder.setInventory(inventory);

        fill(inventory);

        for (String id : plugin.getRewardManager()
                .getRewardIds()) {

            if (!plugin.getRewardManager()
                    .getCategory(id).equalsIgnoreCase(category)) {
                continue;
            }

            int slot = plugin.getRewardManager().getSlot(id);

            if (slot < 0 || slot >= 45) {
                continue;
            }

            var item = plugin.getRewardManager().getItemStack(id);

            if (item == null) {
                item = new org.bukkit.inventory.ItemStack(
                        plugin.getRewardManager().getMaterial(id)
                );
            }

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>ID: <white>" + id);
            lore.add("<gray>Price: <gold>" +
                    plugin.getRewardManager().getPrice(id) + " Gems");
            lore.add("<gray>Status: " +
                    (plugin.getRewardManager().isEnabled(id)
                            ? "<green>Enabled"
                            : "<red>Disabled"));
            lore.add("");
            lore.add("<yellow>Left-click: Toggle");
            lore.add("<red>Right-click: Delete");

            org.bukkit.inventory.ItemStack display = item.clone();

            org.bukkit.inventory.meta.ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {
                meta.displayName(
                        my.kingdomvortex.vortexgems.util.ColorUtil.text(
                                plugin.getRewardManager().getName(id)
                        )
                );

                meta.lore(
                        my.kingdomvortex.vortexgems.util.ColorUtil.lore(lore)
                );

                display.setItemMeta(meta);
            }

            inventory.setItem(slot, display);
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>ADD REWARD</bold>",
                List.of(
                        "",
                        "<gray>Add an item from your inventory.",
                        "",
                        "<yellow>Click to add"
                )
        ));

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>BACK</bold>",
                List.of()
        ));

        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }
    }
}
