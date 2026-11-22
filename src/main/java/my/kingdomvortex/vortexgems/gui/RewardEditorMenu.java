package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
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
                ColorUtil.text("<dark_gray>VORTEX - REWARD MANAGER")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        List<String> rewardIds = new ArrayList<>(
                plugin.getRewardManager().getRewardIds()
        );

        int displaySlot = 0;

        for (String id : rewardIds) {
            if (displaySlot >= 45) {
                break;
            }

            String name = plugin.getRewardManager().getName(id);
            String category = plugin.getRewardManager().getCategory(id);
            Material material = plugin.getRewardManager().getMaterial(id);
            int amount = plugin.getRewardManager().getAmount(id);
            long price = plugin.getRewardManager().getPrice(id);
            String type = plugin.getRewardManager().getType(id);
            boolean enabled = plugin.getRewardManager().isEnabled(id);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>ID: <white>" + id);
            lore.add("<gray>Category: <white>" + category);
            lore.add("<gray>Type: <white>" + type);
            lore.add("<gray>Amount: <white>" + amount);
            lore.add("<gray>Price: <aqua>" + ColorUtil.formatGems(price));
            lore.add("<gray>Status: " +
                    (enabled ? "<green>Enabled" : "<red>Disabled"));
            lore.add("");
            lore.add("<yellow>Left-click: Edit");
            lore.add("<red>Right-click: Delete");

            inventory.setItem(displaySlot++, ItemUtil.create(
                    material,
                    amount,
                    name,
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
