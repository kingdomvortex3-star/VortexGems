
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
        int size = 45;

        MenuHolder holder = new MenuHolder("settings");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text("<dark_gray><bold>VORTEX GEMS SETTINGS")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        double price = plugin.getEconomyManager().getPricePerGem();

        inventory.setItem(10, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>GEMS ECONOMY",
                List.of(
                        "",
                        "<gray>Set the price for purchasing Gems.",
                        "",
                        "<yellow>Current price:",
                        "<white>" + ColorUtil.format((long) price)
                                + " money <gray>/ 1 Gem",
                        "",
                        "<green>Click to change price"
                )
        ));

        boolean transferEnabled = plugin.getConfig().getBoolean(
                "transfer.enabled", true
        );

        inventory.setItem(12, ItemUtil.create(
                Material.EMERALD,
                1,
                "<green><bold>PLAYER TRANSFER",
                List.of(
                        "",
                        "<gray>Allow players to transfer Gems.",
                        "",
                        "<yellow>Status: " + (transferEnabled
                                ? "<green>ENABLED"
                                : "<red>DISABLED"),
                        "",
                        "<green>Click to toggle"
                )
        ));

        boolean opBypass = plugin.getConfig().getBoolean(
                "admin.op-bypass", true
        );

        inventory.setItem(14, ItemUtil.create(
                Material.PLAYER_HEAD,
                1,
                "<aqua><bold>ADMIN OP BYPASS",
                List.of(
                        "",
                        "<gray>Allow server operators to bypass",
                        "<gray>the admin permission requirement.",
                        "",
                        "<yellow>Status: " + (opBypass
                                ? "<green>ENABLED"
                                : "<red>DISABLED"),
                        "",
                        "<green>Click to toggle"
                )
        ));

        int leaderboardLimit = plugin.getConfig().getInt(
                "leaderboard.limit", 10
        );

        inventory.setItem(16, ItemUtil.create(
                Material.HOPPER,
                1,
                "<light_purple><bold>LEADERBOARD LIMIT",
                List.of(
                        "",
                        "<gray>Set the number of players",
                        "<gray>shown on the leaderboard.",
                        "",
                        "<yellow>Current limit:",
                        "<white>" + leaderboardLimit + " players",
                        "",
                        "<green>Click to change"
                )
        ));

        inventory.setItem(31, ItemUtil.create(
                Material.WRITABLE_BOOK,
                1,
                "<yellow><bold>RELOAD CONFIGURATION",
                List.of(
                        "",
                        "<gray>Reload plugin configuration files.",
                        "",
                        "<green>Click to reload"
                )
        ));

        inventory.setItem(40, ItemUtil.create(
                Material.ARROW,
                1,
                "<red><bold>BACK",
                List.of(
                        "",
                        "<gray>Return to admin menu."
                )
        ));

        player.openInventory(inventory);
    }
}
