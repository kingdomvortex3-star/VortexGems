
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
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
                Component.text("VORTEX - REWARD MANAGER")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int slot = 0;

        for (String id : plugin.getRewardManager().getRewardIds()) {
            if (slot >= 45) {
                break;
            }

            var manager = plugin.getRewardManager();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>ID: <white>" + id);
            lore.add("<gray>Category: <aqua>" + manager.getCategory(id));
            lore.add("<gray>Type: <white>" + manager.getType(id));
            lore.add("<gray>Amount: <white>" + manager.getAmount(id));
            lore.add("<gray>Price: <gold>" + manager.getPrice(id) + " Gems");
            lore.add("<gray>Status: " + (manager.isEnabled(id)
                    ? "<green>Enabled"
                    : "<red>Disabled"));
            lore.add("");
            lore.add("<yellow>Reward configuration");

            Material material = manager.getMaterial(id);

            inventory.setItem(slot++, ItemUtil.create(
                    material == null ? Material.CHEST : material,
                    1,
                    manager.getName(id),
                    lore
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
                        "<yellow>Use /gemsadmin reward create"
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
