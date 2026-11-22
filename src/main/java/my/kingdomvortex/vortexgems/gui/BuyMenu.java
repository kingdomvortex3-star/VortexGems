
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

public final class BuyMenu {

    private final VortexGems plugin;

    public BuyMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("gui.buy.size", 45);
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 45;
        }

        MenuHolder holder = new MenuHolder("BUY");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(config.getString(
                        "gui.buy.title",
                        "<dark_gray>ᴠᴏʀᴛᴇx ɢᴇᴍѕ | ʙᴜʏ"
                ))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        boolean enabled = plugin.getEconomyManager().isBuyingEnabled()
                && plugin.getEconomyManager().isAvailable();

        double price = plugin.getEconomyManager().getPricePerGem();

        long balance = plugin.getGemsManager()
                .getBalance(player.getUniqueId());

        inventory.setItem(13, ItemUtil.create(
                Material.EMERALD,
                1,
                "<gradient:#55ff55:#00aaaa><bold>ʙᴜʏ ɢᴇᴍѕ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ʏᴏᴜʀ ʙᴀʟᴀɴᴄᴇ: <aqua>" +
                                ColorUtil.format(balance) + " ɢᴇᴍѕ",
                        "<gray>ᴘʀɪᴄᴇ ᴘᴇʀ ɢᴇᴍ: <gold>" +
                                String.format("%,.2f", price),
                        "",
                        enabled
                                ? "<green>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴀᴠᴀɪʟᴀʙʟᴇ"
                                : "<red>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ"
                )
        ));

        addPurchase(inventory, 20, 10, price, enabled);
        addPurchase(inventory, 22, 50, price, enabled);
        addPurchase(inventory, 24, 100, price, enabled);
        addPurchase(inventory, 30, 500, price, enabled);
        addPurchase(inventory, 32, 1000, price, enabled);

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

    private void addPurchase(
            Inventory inventory,
            int slot,
            long amount,
            double pricePerGem,
            boolean enabled
    ) {
        double total = pricePerGem * amount;

        inventory.setItem(slot, ItemUtil.create(
                enabled ? Material.EMERALD_BLOCK : Material.BARRIER,
                1,
                enabled
                        ? "<green><bold>" + amount + " ɢᴇᴍѕ</bold>"
                        : "<red><bold>ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴄᴇɪᴠᴇ: <aqua>" +
                                ColorUtil.format(amount) + " ɢᴇᴍѕ",
                        "<gray>ᴄᴏѕᴛ: <gold>" +
                                String.format("%,.2f", total),
                        "",
                        enabled
                                ? "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴘᴜʀᴄʜᴀѕᴇ"
                                : "<red>ʙᴜʏɪɴɢ ɪѕ ᴅɪѕᴀʙʟᴇᴅ"
                )
        ));
    }
}
