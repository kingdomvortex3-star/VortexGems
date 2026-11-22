package my.kingdomvortex.vortexgems.command;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GemsCommand implements CommandExecutor, TabCompleter {

    private final VortexGems plugin;

    public GemsCommand(VortexGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuManager().openMain(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance", "bal" -> showBalance(player, args);
            case "pay", "transfer" -> transfer(player, args);
            case "shop", "store" -> plugin.getMenuManager().openCategories(player);
            case "buy" -> plugin.getMenuManager().openBuy(player);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage(ColorUtil.text(
                        "<red>Unknown command. Use <white>/gems help"
                ));
            }
        }

        return true;
    }

    private void showBalance(Player player, String[] args) {
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ColorUtil.text("<red>Player not found."));
                return;
            }

            long balance = plugin.getGemsManager().getBalance(target.getUniqueId());

            player.sendMessage(ColorUtil.text(
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━"
            ));
            player.sendMessage(ColorUtil.text(
                    "<gold><bold>GEMS BALANCE"
            ));
            player.sendMessage(ColorUtil.text(
                    "<gray>Player: <white>" + target.getName()
            ));
            player.sendMessage(ColorUtil.text(
                    "<gray>Balance: <aqua>" + String.format("%,d", balance) + " Gems"
            ));
            player.sendMessage(ColorUtil.text(
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━"
            ));
            return;
        }

        long balance = plugin.getGemsManager().getBalance(player.getUniqueId());

        player.sendMessage(ColorUtil.text(
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━"
        ));
        player.sendMessage(ColorUtil.text(
                "<gold><bold>YOUR GEMS"
        ));
        player.sendMessage(ColorUtil.text(
                "<gray>Balance: <aqua>" + String.format("%,d", balance) + " Gems"
        ));
        player.sendMessage(ColorUtil.text(
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━"
        ));
    }

    private void transfer(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("transfer.enabled", true)) {
            player.sendMessage(ColorUtil.text(
                    "<red>Gems transfer is currently disabled."
            ));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ColorUtil.text(
                    "<red>Usage: /gems pay <player> <amount>"
            ));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ColorUtil.text("<red>That player is not online."));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(ColorUtil.text(
                    "<red>You cannot transfer Gems to yourself."
            ));
            return;
        }

        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException exception) {
            player.sendMessage(ColorUtil.text("<red>Invalid amount."));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(ColorUtil.text(
                    "<red>Amount must be greater than zero."
            ));
            return;
        }

        long maximum = plugin.getConfig().getLong(
                "transfer.max-amount",
                Long.MAX_VALUE
        );

        if (amount > maximum) {
            player.sendMessage(ColorUtil.text(
                    "<red>Maximum transfer amount is <white>"
                            + String.format("%,d", maximum) + " Gems."
            ));
            return;
        }

        boolean success = plugin.getGemsManager().transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                amount
        );

        if (!success) {
            player.sendMessage(ColorUtil.text(
                    "<red>Transfer failed. Check your Gems balance."
            ));
            return;
        }

        plugin.getTransactionManager().record(
                player.getUniqueId(),
                "TRANSFER_OUT",
                -amount,
                "Transferred to " + target.getName()
        );

        plugin.getTransactionManager().record(
                target.getUniqueId(),
                "TRANSFER_IN",
                amount,
                "Received from " + player.getName()
        );

        String formatted = String.format("%,d", amount);

        player.sendMessage(ColorUtil.text(
                "<green>You sent <aqua>" + formatted
                        + " Gems <green>to <white>" + target.getName() + "<green>."
        ));

        target.sendMessage(ColorUtil.text(
                "<green>You received <aqua>" + formatted
                        + " Gems <green>from <white>" + player.getName() + "<green>."
        ));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ColorUtil.text("<gold><bold>VORTEX GEMS"));
        player.sendMessage(ColorUtil.text("<yellow>/gems <gray>- Open Gems menu"));
        player.sendMessage(ColorUtil.text("<yellow>/gems balance [player] <gray>- Check balance"));
        player.sendMessage(ColorUtil.text("<yellow>/gems pay <player> <amount> <gray>- Transfer Gems"));
        player.sendMessage(ColorUtil.text("<yellow>/gems shop <gray>- Open reward shop"));
        player.sendMessage(ColorUtil.text("<yellow>/gems buy <gray>- Buy Gems"));
        player.sendMessage(ColorUtil.text("<yellow>/gems help <gray>- Show commands"));
        player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(List.of(
                    "balance",
                    "pay",
                    "shop",
                    "buy",
                    "help"
            ));
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("pay")
                || args[0].equalsIgnoreCase("balance"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
        } else if (args.length == 3
                && args[0].equalsIgnoreCase("pay")) {
            suggestions.add("100");
            suggestions.add("500");
            suggestions.add("1000");
        }

        String input = args[args.length - 1].toLowerCase();

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .toList();
    }
}
