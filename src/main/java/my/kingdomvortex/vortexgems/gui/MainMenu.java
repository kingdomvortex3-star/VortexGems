
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

public final class MainMenu {

    private final VortexGems plugin;

    public MainMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("gui.main.size", 45);
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 45;
        }

        String title = config.getString(
                "gui.main.title",
                "<dark_gray>ᴠᴏʀᴛᴇx ɢᴇᴍѕ"
        );

        MenuHolder holder = new MenuHolder("MAIN");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(title)
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        long balance = plugin.getGemsManager()
                .getBalance(player.getUniqueId());

        inventory.setItem(4, ItemUtil.create(
                Material.EMERALD,
                1,
                "<gradient:#55ff55:#00aaaa><bold>ᴠᴏʀᴛᴇx ɢᴇᴍѕ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ᴘʀᴇᴍɪᴜᴍ ɢᴇᴍѕ ѕʏѕᴛᴇᴍ",
                        "",
                        "<gray>ᴘʟᴀʏᴇʀ: <white>" + player.getName(),
                        "<gray>ʏᴏᴜʀ ʙᴀʟᴀɴᴄᴇ: <aqua>" +
                                ColorUtil.format(balance) + " ɢᴇᴍѕ",
                        "",
                        "<dark_gray>ᴠᴏʀᴛᴇx ɴᴇᴛᴡᴏʀᴋ"
                )
        ));

        inventory.setItem(20, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>ʙᴜʏ ɢᴇᴍѕ</bold>",
                List.of(
                        "",
                        "<gray>ᴘᴜʀᴄʜᴀѕᴇ ɢᴇᴍѕ ᴜѕɪɴɢ",
                        "<gray>ᴛʜᴇ ѕᴇʀᴠᴇʀ ᴇᴄᴏɴᴏᴍʏ.",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(22, ItemUtil.create(
                Material.CHEST,
                1,
                "<aqua><bold>ɢᴇᴍѕ ѕʜᴏᴘ</bold>",
                List.of(
                        "",
                        "<gray>ʙʀᴏᴡѕᴇ ᴄᴀᴛᴇɢᴏʀɪᴇѕ",
                        "<gray>ᴀɴᴅ ᴘᴜʀᴄʜᴀѕᴇ ʀᴇᴡᴀʀᴅѕ.",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(24, ItemUtil.create(
                Material.COMPASS,
                1,
                "<light_purple><bold>ᴀᴅᴍɪɴ ᴘᴀɴᴇʟ</bold>",
                List.of(
                        "",
                        "<gray>ᴍᴀɴᴀɢᴇ ᴄᴀᴛᴇɢᴏʀɪᴇѕ,",
                        "<gray>ʀᴇᴡᴀʀᴅѕ ᴀɴᴅ ѕᴇᴛᴛɪɴɢѕ.",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"
                )
        ));

        inventory.setItem(40, ItemUtil.create(
                Material.BARRIER,
                1,
                "<red><bold>ᴄʟᴏѕᴇ</bold>",
                List.of(
                        "",
                        "<gray>ᴄʟᴏѕᴇ ᴛʜɪѕ ᴍᴇɴᴜ."
                )
        ));

        player.openInventory(inventory);
    }
}
