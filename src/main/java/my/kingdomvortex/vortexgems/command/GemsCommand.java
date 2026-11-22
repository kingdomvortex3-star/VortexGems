
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
import java.util.Locale;

public final class GemsCommand implements CommandExecutor, TabCompleter {

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
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuManager().openMain(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "edit" -> {
                if (!hasAdminPermission(player)) {
                    player.sendMessage(ColorUtil.text(
                            "<red>You don't have permission to edit VortexGems."
                    ));
                    return true;
                }

                plugin.getMenuManager().openAdmin(player);
            }

            case "balance", "bal" -> showBalance(player, args);

            case "pay", "transfer" -> transfer(player, args);

            case "shop", "store" ->
                    plugin.getMenuManager().openCategories(player);

            case "buy" ->
                    plugin.getMenuManager().openBuy(player);

            case "help" -> sendHelp(player);

            default -> player.sendMessage(ColorUtil.text(
                    "<red>Unknown command. Use <white>/gems help"
            ));
        }

        return true;
    }

    private boolean hasAdminPermission(Player player) {
        boolean opBypass = plugin.getConfig().getBoolean(
                "settings.admin.op-bypass",
                true
        );

        String permission = plugin.getConfig().getString(
                "settings.admin.permission",
                "vortexgems.admin"
        );

        return player.hasPermission(permission)
                || (opBypass && player.isOp());
    }

    private void showBalance(Player player, String[] args) {
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ColorUtil.text("<red>Player not found."));
                return;
            }

            long balance = plugin.getGemsManager().getBalance(
                    target.getUniqueId()
            );

            player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(ColorUtil.text("<gold><bold>GEMS BALANCE"));
            player.sendMessage(ColorUtil.text(
                    "<gray>Player: <white>" + target.getName()
            ));
            player.sendMessage(ColorUtil.text(
                    "<gray>Balance: <aqua>" + ColorUtil.formatGems(balance)
            ));
            player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
            return;
        }

        long balance = plugin.getGemsManager().getBalance(
                player.getUniqueId()
        );

        player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ColorUtil.text("<gold><bold>YOUR GEMS"));
        player.sendMessage(ColorUtil.text(
                "<gray>Balance: <aqua>" + ColorUtil.formatGems(balance)
        ));
        player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
    }

    private void transfer(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean(
                "settings.transfer.enabled",
                true
        )) {
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

        if (target == null) {
            player.sendMessage(ColorUtil.text(
                    "<red>That player is not online."
            ));
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

        long minimum = plugin.getConfig().getLong(
                "settings.transfer.minimum",
                1
        );

        long maximum = plugin.getConfig().getLong(
                "settings.transfer.maximum",
                1000000
        );

        if (amount < minimum || amount > maximum) {
            player.sendMessage(ColorUtil.text(
                    "<red>Transfer amount must be between <white>" +
                            ColorUtil.formatGems(minimum) +
                            " <red>and <white>" +
                            ColorUtil.formatGems(maximum)
            ));
            return;
        }

        boolean success = plugin.getGemsManager().transfer(
                player.getUniqueId(),
                player.getName(),
                target.getUniqueId(),
                target.getName(),
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
                amount,
                "Transferred to " + target.getName()
        );

        plugin.getTransactionManager().record(
                target.getUniqueId(),
                "TRANSFER_IN",
                amount,
                "Received from " + player.getName()
        );

        String formatted = ColorUtil.formatGems(amount);

        player.sendMessage(ColorUtil.text(
                "<green>You sent <aqua>" + formatted +
                        " <green>to <white>" + target.getName() + "<green>."
        ));

        target.sendMessage(ColorUtil.text(
                "<green>You received <aqua>" + formatted +
                        " <green>from <white>" + player.getName() + "<green>."
        ));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ColorUtil.text("<gold><bold>VORTEX GEMS"));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems <gray>- Open Gems menu"
        ));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems edit <gray>- Open admin editor"
        ));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems balance [player] <gray>- Check balance"
        ));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems pay <player> <amount> <gray>- Transfer Gems"
        ));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems shop <gray>- Open reward shop"
        ));
        player.sendMessage(ColorUtil.text(
                "<yellow>/gems buy <gray>- Buy Gems"
        ));
        player.sendMessage(ColorUtil.text(
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━"
        ));
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
                    "edit",
                    "balance",
                    "bal",
                    "pay",
                    "transfer",
                    "shop",
                    "store",
                    "buy",
                    "help"
            ));
        } else if (args.length == 2 &&
                List.of("pay", "transfer", "balance", "bal")
                        .contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
        } else if (args.length == 3 &&
                List.of("pay", "transfer")
                        .contains(args[0].toLowerCase(Locale.ROOT))) {
            suggestions.addAll(List.of("100", "500", "1000"));
        }

        String input = args[args.length - 1].toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(input))
                .toList();
    }
}
