
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class AdminMenu {

    private final VortexGems plugin;

    public AdminMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder("ADMIN");

        Inventory inventory = Bukkit.createInventory(
                holder,
                45,
                ColorUtil.text(
                        "<gradient:#00D9FF:#9B5CFF><bold>ᴠᴏʀᴛᴇx ᴀᴅᴍɪɴ ᴍᴇɴᴜ</bold></gradient>"
                )
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 45; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        inventory.setItem(11, ItemUtil.create(
                Material.CHEST,
                1,
                "<gradient:#55E7FF:#A78BFA><bold>ᴄᴀᴛᴇɢᴏʀʏ ᴍᴀɴᴀɢᴇʀ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ѕʜᴏᴘ ᴄᴀᴛᴇɢᴏʀɪᴇѕ.",
                        "",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "<aqua>• <gray>ᴄʀᴇᴀᴛᴇ ᴄᴀᴛᴇɢᴏʀɪᴇ",
                        "<aqua>• <gray>ᴇᴅɪᴛ ѕʟᴏᴛ",
                        "<aqua>• <gray>ᴇɴᴀʙʟᴇ / ᴅɪѕᴀʙʟᴇ",
                        "<aqua>• <gray>ᴅᴇʟᴇᴛᴇ ᴄᴀᴛᴇɢᴏʀɪᴇ",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(13, ItemUtil.create(
                Material.DIAMOND,
                1,
                "<gradient:#00E5FF:#448AFF><bold>ʀᴇᴡᴀʀᴅ ᴍᴀɴᴀɢᴇʀ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ɢᴇᴍѕ ѕʜᴏᴘ ʀᴇᴡᴀʀᴅѕ.",
                        "",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "<aqua>• <gray>ᴄʀᴇᴀᴛᴇ ʀᴇᴡᴀʀᴅ",
                        "<aqua>• <gray>ѕᴇᴛ ɢᴇᴍѕ ᴘʀɪᴄᴇ",
                        "<aqua>• <gray>ᴇᴅɪᴛ ʀᴇᴡᴀʀᴅ",
                        "<aqua>• <gray>ᴇɴᴀʙʟᴇ / ᴅɪѕᴀʙʟᴇ",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.COMPARATOR,
                1,
                "<gradient:#D580FF:#FF80AB><bold>ɢᴇᴍѕ ѕᴇᴛᴛɪɴɢѕ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴄᴏɴꜰɪɢᴜʀᴇ ᴠᴏʀᴛᴇxɢᴇᴍѕ.",
                        "",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "<light_purple>• <gray>ᴇᴄᴏɴᴏᴍʏ ѕᴇᴛᴛɪɴɢѕ",
                        "<light_purple>• <gray>ᴛʀᴀɴѕꜰᴇʀ ѕᴇᴛᴛɪɴɢѕ",
                        "<light_purple>• <gray>ᴀᴅᴍɪɴ ᴘᴇʀᴍɪѕѕɪᴏɴ",
                        "<light_purple>• <gray>ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ",
                        "<dark_gray>━━━━━━━━━━━━━━━━",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(31, ItemUtil.create(
                Material.BARRIER,
                1,
                "<gradient:#FF5252:#FF8A65><bold>ᴄʟᴏѕᴇ ᴍᴇɴᴜ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴄʟᴏѕᴇ ᴛʜᴇ ᴀᴅᴍɪɴ ᴍᴇɴᴜ.",
                        "",
                        "<red>ᴄʟɪᴄᴋ ᴛᴏ ᴄʟᴏѕᴇ"
                )
        ));

        player.openInventory(inventory);
    }
}
