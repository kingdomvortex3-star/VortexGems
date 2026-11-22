package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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
                Component.text("VORTEX - REWARD MANAGER")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int slot = 0;

        for (String id : plugin.getRewardManager().getRewardIds()) {
            if (slot >= 45) {
                break;
            }

            String name = plugin.getRewardManager().getName(id);
            String category = plugin.getRewardManager().getCategory(id);
            Material material = plugin.getRewardManager().getMaterial(id);
            int amount = plugin.getRewardManager().getAmount(id);
            long price = plugin.getRewardManager().getPrice(id);
            boolean enabled = plugin.getRewardManager().isEnabled(id);
            String type = plugin.getRewardManager().getType(id);

            inventory.setItem(slot++, ItemUtil.create(
                    material,
                    amount,
                    name,
                    List.of(
                            "",
                            "<gray>ID: <white>" + id,
                            "<gray>Category: <white>" + category,
                            "<gray>Type: <white>" + type,
                            "<gray>Price: <aqua>" + String.format("%,d", price) + " Gems",
                            "<gray>Status: " + (enabled ? "<green>Enabled" : "<red>Disabled"),
                            "",
                            "<yellow>Left-click: Edit",
                            "<red>Right-click: Delete"
                    )
            ));
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>CREATE REWARD</bold>",
                List.of(
                        "",
                        "<gray>Create a new shop reward.",
                        "",
                        "<yellow>Click to create"
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
}
