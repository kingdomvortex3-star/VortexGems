package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
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

        MenuHolder holder = new MenuHolder("MAIN");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ColorUtil.text(config.getString(
                        "gui.main.title",
                        "<dark_gray>VORTEX GEMS"
                ))
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int balanceSlot = config.getInt("gui.main.balance-slot", 4);
        int buySlot = config.getInt("gui.main.buy-slot", 20);
        int shopSlot = config.getInt("gui.main.shop-slot", 22);
        int leaderboardSlot = config.getInt("gui.main.leaderboard-slot", 24);
        int closeSlot = config.getInt("gui.main.close-slot", 40);

        long balance = plugin.getGemsManager().getBalance(
                player.getUniqueId()
        );

        setItem(inventory, balanceSlot, ItemUtil.create(
                Material.EMERALD,
                1,
                "<green><bold>YOUR GEMS</bold>",
                List.of(
                        "",
                        "<gray>Your current balance:",
                        "<aqua><bold>" + ColorUtil.formatGems(balance),
                        "",
                        "<yellow>Your Gems wallet."
                )
        ));

        setItem(inventory, buySlot, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>BUY GEMS</bold>",
                List.of(
                        "",
                        "<gray>Purchase Gems using server money.",
                        "",
                        "<yellow>Click to open."
                )
        ));

        setItem(inventory, shopSlot, ItemUtil.create(
                Material.CHEST,
                1,
                "<aqua><bold>GEMS SHOP</bold>",
                List.of(
                        "",
                        "<gray>Browse available reward categories.",
                        "<gray>Purchase exclusive rewards.",
                        "",
                        "<yellow>Click to open."
                )
        ));

        List<String> leaderboardLore = new ArrayList<>();
        leaderboardLore.add("");
        leaderboardLore.add("<gray>Top Gems players:");
        leaderboardLore.add("");

        int rank = 1;

        for (var entry : plugin.getLeaderboardManager().getTop(5)) {
            leaderboardLore.add(
                    "<yellow>#" + rank++ + " <white>" + entry.getName()
                            + " <dark_gray>- <aqua>"
                            + ColorUtil.format(entry.getBalance())
            );
        }

        if (rank == 1) {
            leaderboardLore.add("<gray>No leaderboard data available.");
        }

        leaderboardLore.add("");
        leaderboardLore.add("<yellow>Top 5 players");

        setItem(inventory, leaderboardSlot, ItemUtil.create(
                Material.GOLDEN_HELMET,
                1,
                "<light_purple><bold>LEADERBOARD</bold>",
                leaderboardLore
        ));

        setItem(inventory, closeSlot, ItemUtil.create(
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

    private void setItem(
            Inventory inventory,
            int slot,
            org.bukkit.inventory.ItemStack item
    ) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }
}
