
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class CategoryMenu {

    private final VortexGems plugin;

    public CategoryMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = plugin.getConfig().getInt("gui.category.size", 54);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        MenuHolder holder = new MenuHolder("CATEGORY");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(plugin.getConfig().getString(
                        "gui.category.title",
                        "<dark_gray>ɢᴇᴍѕ ѕʜᴏᴘ | ᴄᴀᴛᴇɢᴏʀɪᴇѕ"
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

            if (material == null || material.isAir()) {
                material = Material.CHEST;
            }

            String name = plugin.getCategoryManager().getName(id);

            List<String> lore = plugin.getCategoryManager().getLore(id);

            inventory.setItem(slot, ItemUtil.create(
                    material,
                    plugin.getCategoryManager().getAmount(id),
                    "<gradient:#55ff55:#00aaaa><bold>" + name + "</bold></gradient>",
                    lore == null || lore.isEmpty()
                            ? List.of(
                                    "",
                                    "<gray>ᴠɪᴇᴡ ʀᴇᴡᴀʀᴅѕ ɪɴ ᴛʜɪѕ",
                                    "<gray>ᴄᴀᴛᴇɢᴏʀʏ.",
                                    "",
                                    "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                            )
                            : lore
            ));
        }

        inventory.setItem(size - 5, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>ʙᴀᴄᴋ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴍᴀɪɴ ᴍᴇɴᴜ"
                )
        ));

        player.openInventory(inventory);
    }
}
