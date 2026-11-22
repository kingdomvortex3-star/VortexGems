
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class CategoryMenu {

    private final VortexGems plugin;

    public CategoryMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("gui.category.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        MenuHolder holder = new MenuHolder("CATEGORY");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(config.getString(
                        "gui.category.title",
                        "<dark_gray>Gems Shop"
                ))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        for (String id : plugin.getCategoryManager().getEnabledCategories()) {
            int slot = plugin.getCategoryManager().getSlot(id);

            if (slot < 0 || slot >= size || slot == size - 5) {
                continue;
            }

            Material material = plugin.getCategoryManager().getMaterial(id);
            String name = plugin.getCategoryManager().getName(id);

            List<String> lore = plugin.getCategoryManager().getLore(id);
            lore.add("");
            lore.add("<yellow>Click to view rewards");

            inventory.setItem(slot, ItemUtil.create(
                    material,
                    plugin.getCategoryManager().getAmount(id),
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
                        "<gray>Return to main menu."
                )
        ));

        player.openInventory(inventory);
    }
}
