
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
                ColorUtil.text(
                        "<gradient:#00D9FF:#9B5CFF><bold>ᴠᴏʀᴛᴇx ᴄᴀᴛᴇɢᴏʀʏ</bold></gradient>"
                )
        );

        holder.setInventory(inventory);

        // Background
        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        // Categories
        int displaySlot = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (displaySlot >= 45) {
                break;
            }

            String name = plugin.getCategoryManager().getName(id);
            int categorySlot = plugin.getCategoryManager().getSlot(id);
            boolean enabled = plugin.getCategoryManager().isEnabled(id);

            Material icon = plugin.getCategoryManager().getMaterial(id);

            if (icon == null || icon.isAir()) {
                icon = Material.CHEST;
            }

            inventory.setItem(displaySlot++, ItemUtil.create(
                    icon,
                    plugin.getCategoryManager().getAmount(id),
                    "<gradient:#55E7FF:#A78BFA><bold>ᴄᴀᴛᴇɢᴏʀʏ: "
                            + name + "</bold></gradient>",
                    List.of(
                            "",
                            "<dark_gray>━━━━━━━━━━━━━━━━━━",
                            "<gray>ɪᴅ: <white>" + id,
                            "<gray>ѕʟᴏᴛ: <aqua>" + (categorySlot + 1),
                            "<gray>ѕᴛᴀᴛᴜѕ: "
                                    + (enabled
                                    ? "<green>ᴇɴᴀʙʟᴇᴅ"
                                    : "<red>ᴅɪѕᴀʙʟᴇᴅ"),
                            "<dark_gray>━━━━━━━━━━━━━━━━━━",
                            "",
                            "<green>ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ѕʟᴏᴛ",
                            "<red>ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ",
                            "<light_purple>ᴍɪᴅᴅʟᴇ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ",
                            ""
                    )
            ));
        }

        // Create category
        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<gradient:#39FF88:#00BFA5><bold>ᴄʀᴇᴀᴛᴇ ᴄᴀᴛᴇɢᴏʀʏ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴄʀᴇᴀᴛᴇ ᴀ ɴᴇᴡ ѕʜᴏᴘ ᴄᴀᴛᴇɢᴏʀʏ.",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴄʀᴇᴀᴛᴇ"
                )
        ));

        // Back
        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<gradient:#FF6B6B:#FFB86B><bold>ʙᴀᴄᴋ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ʀᴇᴛᴜʀɴ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ."
                )
        ));

        // Guide
        inventory.setItem(53, ItemUtil.create(
                Material.BOOK,
                1,
                "<aqua><bold>ᴄᴀᴛᴇɢᴏʀʏ ɢᴜɪᴅᴇ</bold>",
                List.of(
                        "",
                        "<gray>ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ѕʜᴏᴘ ᴄᴀᴛᴇɢᴏʀɪᴇѕ.",
                        "",
                        "<green>ᴛᴏᴜᴄʜ ʟᴇꜰᴛ <dark_gray>- <white>ᴄʜᴀɴɢᴇ ѕʟᴏᴛ",
                        "<red>ʀɪɢʜᴛ ᴄʟɪᴄᴋ <dark_gray>- <white>ᴅᴇʟᴇᴛᴇ",
                        "<light_purple>ᴍɪᴅᴅʟᴇ ᴄʟɪᴄᴋ <dark_gray>- <white>ᴛᴏɢɢʟᴇ",
                        "",
                        "<gray>ѕʟᴏᴛ ɴᴜᴍʙᴇʀ: <aqua>1 - 54",
                        "<red>ѕʟᴏᴛ 50 ɪѕ ʀᴇѕᴇʀᴠᴇᴅ."
                )
        ));

        player.openInventory(inventory);
    }
}
