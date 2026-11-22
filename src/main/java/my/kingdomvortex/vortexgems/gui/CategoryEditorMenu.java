
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
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
                ColorUtil.text("<dark_gray>ᴠᴏʀᴛᴇx | ᴄᴀᴛᴇɢᴏʀʏ ᴍᴀɴᴀɢᴇʀ")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int displaySlot = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (displaySlot >= 45) {
                break;
            }

            Material icon = plugin.getCategoryManager().getMaterial(id);

            if (icon == null || icon.isAir()) {
                icon = Material.CHEST;
            }

            String name = plugin.getCategoryManager().getName(id);
            int categorySlot = plugin.getCategoryManager().getSlot(id);
            boolean enabled = plugin.getCategoryManager().isEnabled(id);

            inventory.setItem(displaySlot++, ItemUtil.create(
                    icon,
                    plugin.getCategoryManager().getAmount(id),
                    "<gradient:#55E7FF:#A78BFA><bold>ᴄᴀᴛᴇɢᴏʀʏ: "
                            + name + "</bold></gradient>",
                    List.of(
                            "",
                            "<gray>ɪᴅ: <white>" + id,
                            "<gray>ѕʜᴏᴘ ѕʟᴏᴛ: <aqua>" + (categorySlot + 1),
                            "<gray>ѕᴛᴀᴛᴜѕ: "
                                    + (enabled ? "<green>ᴇɴᴀʙʟᴇᴅ" : "<red>ᴅɪѕᴀʙʟᴇᴅ"),
                            "",
                            "<green>ʟᴇꜰᴛ ᴄʟɪᴄᴋ: ᴇᴅɪᴛ ѕʟᴏᴛ",
                            "<aqua>ѕʜɪꜰᴛ + ʟᴇꜰᴛ: ᴄʜᴀɴɢᴇ ɪᴄᴏɴ",
                            "<red>ʀɪɢʜᴛ ᴄʟɪᴄᴋ: ᴅᴇʟᴇᴛᴇ",
                            "<light_purple>ᴍɪᴅᴅʟᴇ ᴄʟɪᴄᴋ: ᴛᴏɢɢʟᴇ"
                    )
            ));
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>ᴄʀᴇᴀᴛᴇ ᴄᴀᴛᴇɢᴏʀʏ</bold>",
                List.of("", "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴄʀᴇᴀᴛᴇ")
        ));

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<yellow><bold>ʙᴀᴄᴋ</bold>",
                List.of("", "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ")
        ));

        inventory.setItem(53, ItemUtil.create(
                Material.BOOK,
                1,
                "<aqua><bold>ɢᴜɪᴅᴇ</bold>",
                List.of(
                        "",
                        "<gray>ѕʜɪꜰᴛ + ʟᴇꜰᴛ ᴄʟɪᴄᴋ ᴡɪᴛʜ ᴀɴ ɪᴛᴇᴍ",
                        "<gray>ɪɴ ʏᴏᴜʀ ʜᴀɴᴅ ᴛᴏ ᴄʜᴀɴɢᴇ ᴛʜᴇ ɪᴄᴏɴ."
                )
        ));

        player.openInventory(inventory);
    }
}
