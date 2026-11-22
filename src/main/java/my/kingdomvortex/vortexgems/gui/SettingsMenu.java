package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SettingsMenu {

    private final VortexGems plugin;

    public SettingsMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = plugin.getConfig().getInt("gui.admin-settings.size", 45);
        String title = plugin.getConfig().getString(
                "gui.admin-settings.title",
                "<dark_gray>Gems Settings"
        );

        Inventory inventory = plugin.getServer().createInventory(
                new MenuHolder("settings", null),
                size,
                ColorUtil.text(title)
        );

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(
                    slot,
                    ItemUtil.filler(Material.BLACK_STAINED_GLASS_PANE)
            );
        }

        inventory.setItem(10, ItemUtil.create(
                Material.GOLD_INGOT,
                "<gold>Economy Settings",
                "<gray>Configure Gems purchase price.",
                "",
                "<yellow>Current price:",
                "<white>" + plugin.getEconomyManager().getPrice() + " money",
                "",
                "<green>Click to edit"
        ));

        inventory.setItem(12, ItemUtil.create(
                Material.EMERALD,
                "<green>Transfer Settings",
                "<gray>Configure player-to-player transfers.",
                "",
                "<yellow>Status:",
                plugin.getConfig().getBoolean("transfer.enabled", true)
                        ? "<green>Enabled"
                        : "<red>Disabled",
                "",
                "<green>Click to toggle"
        ));

        inventory.setItem(14, ItemUtil.create(
                Material.PLAYER_HEAD,
                "<aqua>Admin OP Bypass",
                "<gray>Configure administrator bypass.",
                "",
                "<yellow>Status:",
                plugin.getConfig().getBoolean("admin.op-bypass", true)
                        ? "<green>Enabled"
                        : "<red>Disabled",
                "",
                "<green>Click to toggle"
        ));

        inventory.setItem(16, ItemUtil.create(
                Material.HOPPER,
                "<gold>Leaderboard Settings",
                "<gray>Configure leaderboard display.",
                "",
                "<yellow>Entries:",
                "<white>" + plugin.getConfig().getInt("leaderboard.limit", 10),
                "",
                "<green>Click to edit"
        ));

        inventory.setItem(31, ItemUtil.create(
                Material.WRITABLE_BOOK,
                "<yellow>Reload Configuration",
                "<gray>Reload plugin configuration files.",
                "",
                "<green>Click to reload"
        ));

        inventory.setItem(40, ItemUtil.create(
                Material.ARROW,
                "<red>Back",
                "<gray>Return to admin menu."
        ));

        player.openInventory(inventory);
    }
              }
