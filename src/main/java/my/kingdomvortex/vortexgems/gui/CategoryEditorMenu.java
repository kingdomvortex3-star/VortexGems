
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class CategoryEditorMenu {

    private final VortexGems plugin;

    public CategoryEditorMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder("CATEGORY_EDITOR");

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                Component.text("VORTEX - CATEGORY MANAGER")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int slot = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (slot >= 45) break;

            String name = plugin.getCategoryManager().getName(id);

            inventory.setItem(slot++, ItemUtil.create(
                    plugin.getCategoryManager().getMaterial(id),
                    1,
                    name,
                    List.of(
                            "",
                            "<gray>ID: <white>" + id,
                            "<gray>Status: " +
                                    (plugin.getCategoryManager().isEnabled(id)
                                            ? "<green>Enabled"
                                            : "<red>Disabled"),
                            "",
                            "<yellow>Left-click: Edit",
                            "<red>Right-click: Delete"
                    )
            ));
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>CREATE CATEGORY</bold>",
                List.of(
                        "",
                        "<gray>Create a new category.",
                        "",
                        "<yellow>Click to create"
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
}
