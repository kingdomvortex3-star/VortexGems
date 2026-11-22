package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class SettingsMenu {

    private final VortexGems plugin;

    public SettingsMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = plugin.getConfig().getInt("gui.settings.size", 45);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 45;
        }

        MenuHolder holder = new MenuHolder("SETTINGS");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(plugin.getConfig().getString(
                        "gui.settings.title",
                        "<dark_gray>Gems Settings"
                ))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        double price = plugin.getEconomyManager().getPricePerGem();
        boolean transfer = plugin.getConfig().getBoolean(
                "settings.transfer.enabled", true
        );
        boolean bypass = plugin.getConfig().getBoolean(
                "settings.admin.op-bypass", true
        );
        int leaderboard = plugin.getConfig().getInt(
                "settings.leaderboard.limit", 10
        );

        inventory.setItem(10, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>ECONOMY SETTINGS</bold>",
                List.of(
                        "",
                        "<gray>Price per Gem:",
                        "<yellow>" + String.format("%,.2f", price),
                        "",
                        "<gray>Configure purchase price.",
                        "<yellow>Use config.yml to change."
                )
        ));

        inventory.setItem(12, ItemUtil.create(
                Material.EMERALD,
                1,
                "<green><bold>TRANSFER SETTINGS</bold>",
                List.of(
                        "",
                        "<gray>Player-to-player transfers:",
                        transfer ? "<green>ENABLED" : "<red>DISABLED",
                        "",
                        "<yellow>Click to toggle"
                )
        ));

        inventory.setItem(14, ItemUtil.create(
                Material.PLAYER_HEAD,
                1,
                "<aqua><bold>ADMIN OP BYPASS</bold>",
                List.of(
                        "",
                        "<gray>Allow OP to bypass admin permission:",
                        bypass ? "<green>ENABLED" : "<red>DISABLED",
                        "",
                        "<yellow>Click to toggle"
                )
        ));

        inventory.setItem(16, ItemUtil.create(
                Material.HOPPER,
                1,
                "<light_purple><bold>LEADERBOARD SETTINGS</bold>",
                List.of(
                        "",
                        "<gray>Displayed entries:",
                        "<yellow>" + leaderboard,
                        "",
                        "<gray>Configure in config.yml."
                )
        ));

        inventory.setItem(31, ItemUtil.create(
                Material.WRITABLE_BOOK,
                1,
                "<yellow><bold>RELOAD CONFIGURATION</bold>",
                List.of(
                        "",
                        "<gray>Reload plugin configuration files.",
                        "",
                        "<yellow>Click to reload"
                )
        ));

        inventory.setItem(40, ItemUtil.create(
                Material.ARROW,
                1,
                "<red><bold>BACK</bold>",
                List.of(
                        "",
                        "<gray>Return to admin menu."
                )
        ));

        player.openInventory(inventory);
    }
}
