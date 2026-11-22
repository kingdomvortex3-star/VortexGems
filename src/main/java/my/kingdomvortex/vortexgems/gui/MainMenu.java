
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.manager.LeaderboardManager;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
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
                "<dark_gray><bold>VORTEX GEMS</bold>"
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

        int balanceSlot = getSlot(config, "gui.main.balance-slot", 4, size);
        int buySlot = getSlot(config, "gui.main.buy-slot", 20, size);
        int shopSlot = getSlot(config, "gui.main.shop-slot", 22, size);
        int leaderboardSlot = getSlot(
                config,
                "gui.main.leaderboard-slot",
                24,
                size
        );
        int closeSlot = getSlot(config, "gui.main.close-slot", 40, size);

        long balance = plugin.getGemsManager().getBalance(
                player.getUniqueId()
        );

        inventory.setItem(balanceSlot, ItemUtil.create(
                Material.EMERALD,
                1,
                "<gradient:#00E5FF:#7B61FF><bold>YOUR GEMS</bold></gradient>",
                List.of(
                        "",
                        "<gray>Player: <white>" + player.getName(),
                        "<gray>Balance: <aqua>" + ColorUtil.formatGems(balance),
                        "",
                        "<yellow>Your current Gems balance."
                )
        ));

        inventory.setItem(buySlot, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>BUY GEMS</bold>",
                List.of(
                        "",
                        "<gray>Purchase Gems using server money.",
                        "<gray>Price per Gem: <gold>" +
                                ColorUtil.format(
                                        (long) plugin.getEconomyManager()
                                                .getPricePerGem()
                                ),
                        "",
                        "<yellow>Click to open purchase menu."
                )
        ));

        inventory.setItem(shopSlot, ItemUtil.create(
                Material.CHEST,
                1,
                "<aqua><bold>GEMS SHOP</bold>",
                List.of(
                        "",
                        "<gray>Browse available rewards.",
                        "<gray>Spend your Gems on exclusive items.",
                        "",
                        "<yellow>Click to browse categories."
                )
        ));

        List<String> leaderboardLore = new ArrayList<>();
        leaderboardLore.add("");
        leaderboardLore.add("<gray>Top Gems holders:");
        leaderboardLore.add("");

        if (config.getBoolean("settings.leaderboard.enabled", true)) {
            int limit = Math.max(
                    1,
                    Math.min(
                            10,
                            config.getInt("settings.leaderboard.limit", 10)
                    )
            );

            List<LeaderboardManager.Entry> entries =
                    plugin.getLeaderboardManager().getTop(limit);

            if (entries.isEmpty()) {
                leaderboardLore.add("<dark_gray>No leaderboard data yet.");
            } else {
                int rank = 1;

                for (LeaderboardManager.Entry entry : entries) {
                    leaderboardLore.add(
                            "<yellow>" + rank++ + ". <white>" +
                                    entry.getName() +
                                    " <dark_gray>» <aqua>" +
                                    ColorUtil.format(entry.getBalance())
                    );
                }
            }
        } else {
            leaderboardLore.add("<red>Leaderboard is disabled.");
        }

        leaderboardLore.add("");
        leaderboardLore.add("<gray>Leaderboard overview.");

        inventory.setItem(leaderboardSlot, ItemUtil.create(
                Material.GOLDEN_HELMET,
                1,
                "<gold><bold>GEMS LEADERBOARD</bold>",
                leaderboardLore
        ));

        inventory.setItem(closeSlot, ItemUtil.create(
                Material.BARRIER,
                1,
                "<red><bold>CLOSE</bold>",
                List.of(
                        "",
                        "<gray>Close this menu."
                )
        ));

        player.openInventory(inventory);
    }

    private int getSlot(
            FileConfiguration config,
            String path,
            int fallback,
            int size
    ) {
        int slot = config.getInt(path, fallback);

        if (slot < 0 || slot >= size) {
            return Math.max(0, Math.min(fallback, size - 1));
        }

        return slot;
    }
}
