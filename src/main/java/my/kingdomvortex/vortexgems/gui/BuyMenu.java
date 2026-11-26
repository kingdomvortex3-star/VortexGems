
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuyMenu {

    private static final Map<UUID, Long> SELECTED_AMOUNTS =
            new ConcurrentHashMap<>();

    private final VortexGems plugin;

    public BuyMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    private long getMinimum() {
        return Math.max(1, plugin.getConfig().getLong(
                "settings.economy.minimum-purchase", 1
        ));
    }

    private long getMaximum() {
        return Math.max(getMinimum(), plugin.getConfig().getLong(
                "settings.economy.maximum-purchase", 100000
        ));
    }

    public long getSelectedAmount(Player player) {
        long amount = SELECTED_AMOUNTS.getOrDefault(
                player.getUniqueId(), getMinimum()
        );

        amount = Math.max(getMinimum(), Math.min(amount, getMaximum()));

        SELECTED_AMOUNTS.put(player.getUniqueId(), amount);
        return amount;
    }

    public void increase(Player player) {
        long amount = getSelectedAmount(player);

        if (amount < getMaximum()) {
            SELECTED_AMOUNTS.put(player.getUniqueId(), amount + 1);
        }
    }

    public void decrease(Player player) {
        long amount = getSelectedAmount(player);

        if (amount > getMinimum()) {
            SELECTED_AMOUNTS.put(player.getUniqueId(), amount - 1);
        }
    }

    public void resetSelection(Player player) {
        SELECTED_AMOUNTS.remove(player.getUniqueId());
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("gui.buy.size", 45);

        if (size < 36 || size > 54 || size % 9 != 0) {
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

        long amount = getSelectedAmount(player);

        double price = plugin.getEconomyManager().getPricePerGem();
        double total = plugin.getEconomyManager().calculatePrice(amount);

        long balance = plugin.getGemsManager()
                .getBalance(player.getUniqueId());

        inventory.setItem(13, ItemUtil.create(
                Material.EMERALD,
                1,
                "<gradient:#55ff55:#00aaaa><bold>ʙᴜʏ ɢᴇᴍѕ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ʏᴏᴜʀ ʙᴀʟᴀɴᴄᴇ: <aqua>"
                                + ColorUtil.format(balance) + " ɢᴇᴍѕ",
                        "<gray>ѕᴇʟᴇᴄᴛᴇᴅ: <aqua>"
                                + ColorUtil.format(amount) + " ɢᴇᴍѕ",
                        "<gray>ᴘʀɪᴄᴇ ᴘᴇʀ ɢᴇᴍ: <gold>"
                                + String.format("%,.2f", price),
                        "<gray>ᴛᴏᴛᴀʟ ᴘʀɪᴄᴇ: <gold>"
                                + String.format("%,.2f", total),
                        "",
                        enabled
                                ? "<green>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴀᴠᴀɪʟᴀʙʟᴇ"
                                : "<red>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ"
                )
        ));

        inventory.setItem(20, ItemUtil.create(
                Material.RED_STAINED_GLASS_PANE,
                1,
                "<red><bold>-1 ɢᴇᴍ</bold>",
                List.of(
                        "",
                        "<gray>ᴄʟɪᴄᴋ ᴛᴏ ʀᴇᴅᴜᴄᴇ 1 ɢᴇᴍ",
                        "<dark_gray>ᴍɪɴɪᴍᴜᴍ: "
                                + ColorUtil.format(getMinimum())
                )
        ));

        inventory.setItem(22, ItemUtil.create(
                enabled ? Material.EMERALD_BLOCK : Material.BARRIER,
                1,
                enabled
                        ? "<green><bold>ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ</bold>"
                        : "<red><bold>ᴘᴜʀᴄʜᴀѕᴇ ᴅɪѕᴀʙʟᴇᴅ</bold>",
                List.of(
                        "",
                        "<gray>ᴀᴍᴏᴜɴᴛ: <aqua>"
                                + ColorUtil.format(amount) + " ɢᴇᴍѕ",
                        "<gray>ᴛᴏᴛᴀʟ: <gold>"
                                + String.format("%,.2f", total),
                        "",
                        enabled
                                ? "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ʙᴜʏ"
                                : "<red>ᴠᴀᴜʟᴛ ᴏʀ ʙᴜʏɪɴɢ ɪѕ ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ"
                )
        ));

        inventory.setItem(24, ItemUtil.create(
                Material.LIME_STAINED_GLASS_PANE,
                1,
                "<green><bold>+1 ɢᴇᴍ</bold>",
                List.of(
                        "",
                        "<gray>ᴄʟɪᴄᴋ ᴛᴏ ᴀᴅᴅ 1 ɢᴇᴍ",
                        "<dark_gray>ᴍᴀxɪᴍᴜᴍ: "
                                + ColorUtil.format(getMaximum())
                )
        ));

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
