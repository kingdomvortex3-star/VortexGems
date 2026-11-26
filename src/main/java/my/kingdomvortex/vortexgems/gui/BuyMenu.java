
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuyMenu {

    private static final Map<UUID, Long> SELECTED =
            new ConcurrentHashMap<>();

    private static final long DEFAULT_AMOUNT = 1L;

    private final VortexGems plugin;

    public BuyMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        long amount = getAmount(player);
        double unitPrice = plugin.getEconomyManager().getPricePerGem();
        double total = plugin.getEconomyManager().calculatePrice(amount);

        boolean enabled = plugin.getEconomyManager().isBuyingEnabled()
                && plugin.getEconomyManager().isAvailable();

        long balance = plugin.getGemsManager()
                .getBalance(player.getUniqueId());

        int size = plugin.getConfig().getInt("gui.buy.size", 45);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 45;
        }

        MenuHolder holder = new MenuHolder("BUY");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(plugin.getConfig().getString(
                        "gui.buy.title",
                        "<dark_gray>ᴠᴏʀᴛᴇx ɢᴇᴍѕ | ʙᴜʏ"
                ))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        inventory.setItem(13, ItemUtil.create(
                Material.EMERALD,
                1,
                "<gradient:#55ff55:#00aaaa><bold>ᴘᴜʀᴄʜᴀѕᴇ ɢᴇᴍѕ</bold></gradient>",
                List.of(
                        "",
                        "<gray>ʏᴏᴜʀ ʙᴀʟᴀɴᴄᴇ: <aqua>"
                                + ColorUtil.format(balance) + " ɢᴇᴍѕ",
                        "",
                        "<gray>ѕᴇʟᴇᴄᴛᴇᴅ ᴀᴍᴏᴜɴᴛ: <aqua>"
                                + ColorUtil.format(amount) + " ɢᴇᴍѕ",
                        "<gray>ᴘʀɪᴄᴇ ᴘᴇʀ ɢᴇᴍ: <gold>"
                                + String.format("%,.2f", unitPrice),
                        "<gray>ᴛᴏᴛᴀʟ ᴘʀɪᴄᴇ: <gold>"
                                + String.format("%,.2f", total),
                        "",
                        enabled
                                ? "<green>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴀᴠᴀɪʟᴀʙʟᴇ"
                                : "<red>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴅɪѕᴀʙʟᴇᴅ"
                )
        ));

        inventory.setItem(20, ItemUtil.create(
                Material.RED_STAINED_GLASS_PANE,
                1,
                "<red><bold>- 1 ɢᴇᴍ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴅᴜᴄᴇ ᴘᴜʀᴄʜᴀѕᴇ ᴀᴍᴏᴜɴᴛ",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ʀᴇᴅᴜᴄᴇ"
                )
        ));

        inventory.setItem(22, ItemUtil.create(
                enabled ? Material.EMERALD_BLOCK : Material.BARRIER,
                1,
                enabled
                        ? "<green><bold>ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ</bold>"
                        : "<red><bold>ʙᴜʏɪɴɢ ᴅɪѕᴀʙʟᴇᴅ</bold>",
                List.of(
                        "",
                        "<gray>ʀᴇᴄᴇɪᴠᴇ: <aqua>"
                                + ColorUtil.format(amount) + " ɢᴇᴍѕ",
                        "<gray>ᴘᴀʏ: <gold>"
                                + String.format("%,.2f", total),
                        "",
                        enabled
                                ? "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴘᴜʀᴄʜᴀѕᴇ"
                                : "<red>ᴘᴜʀᴄʜᴀѕɪɴɢ ɪѕ ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ"
                )
        ));

        inventory.setItem(24, ItemUtil.create(
                Material.LIME_STAINED_GLASS_PANE,
                1,
                "<green><bold>+ 1 ɢᴇᴍ</bold>",
                List.of(
                        "",
                        "<gray>ɪɴᴄʀᴇᴀѕᴇ ᴘᴜʀᴄʜᴀѕᴇ ᴀᴍᴏᴜɴᴛ",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ɪɴᴄʀᴇᴀѕᴇ"
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

    public long getAmount(Player player) {
        return SELECTED.getOrDefault(
                player.getUniqueId(),
                DEFAULT_AMOUNT
        );
    }

    public void increase(Player player) {
        long current = getAmount(player);

        long maximum = plugin.getConfig().getLong(
                "settings.economy.maximum-purchase",
                100000
        );

        if (current >= maximum || current == Long.MAX_VALUE) {
            player.sendMessage(ColorUtil.text(
                    "<red>You have reached the maximum purchase amount."
            ));
            return;
        }

        SELECTED.put(player.getUniqueId(), current + 1);
        open(player);
    }

    public void decrease(Player player) {
        long current = getAmount(player);

        long minimum = plugin.getConfig().getLong(
                "settings.economy.minimum-purchase",
                1
        );

        if (current <= minimum) {
            player.sendMessage(ColorUtil.text(
                    "<red>You have reached the minimum purchase amount."
            ));
            return;
        }

        SELECTED.put(player.getUniqueId(), current - 1);
        open(player);
    }

    public void reset(Player player) {
        SELECTED.remove(player.getUniqueId());
    }
}
