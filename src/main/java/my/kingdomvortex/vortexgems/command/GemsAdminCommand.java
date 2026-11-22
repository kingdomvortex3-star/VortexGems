package my.kingdomvortex.vortexgems.command;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GemsAdminCommand implements CommandExecutor, TabCompleter {

    private final VortexGems plugin;

    public GemsAdminCommand(VortexGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("vortexgems.admin")) {
            sender.sendMessage(ColorUtil.text("<red>You don't have permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "balance", "bal" -> balance(sender, args);
            case "add" -> modifyBalance(sender, args, "add");
            case "remove" -> modifyBalance(sender, args, "remove");
            case "set" -> modifyBalance(sender, args, "set");
            case "category" -> category(sender, args);
            case "reward" -> reward(sender, args);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void reload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(ColorUtil.text(
                "<green>VortexGems configuration successfully reloaded."
        ));
    }

    private void balance(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin balance <player>"
            ));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ColorUtil.text("<red>Player not found."));
            return;
        }

        long amount = plugin.getGemsManager().getBalance(target.getUniqueId());

        sender.sendMessage(ColorUtil.text(
                "<gold>" + target.getName()
                        + "<gray>'s balance: <aqua>"
                        + String.format("%,d", amount) + " Gems"
        ));
    }

    private void modifyBalance(
            CommandSender sender,
            String[] args,
            String action
    ) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin " + action + " <player> <amount>"
            ));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ColorUtil.text("<red>Player not found."));
            return;
        }

        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ColorUtil.text("<red>Invalid amount."));
            return;
        }

        if (amount < 0 || (action.equals("add") || action.equals("remove")) && amount == 0) {
            sender.sendMessage(ColorUtil.text("<red>Amount must be greater than zero."));
            return;
        }

        boolean success;

        switch (action) {
            case "add" -> success = plugin.getGemsManager().addBalance(
                    target.getUniqueId(), amount
            );
            case "remove" -> success = plugin.getGemsManager().removeBalance(
                    target.getUniqueId(), amount
            );
            case "set" -> success = plugin.getGemsManager().setBalance(
                    target.getUniqueId(), amount
            );
            default -> {
                return;
            }
        }

        if (!success) {
            sender.sendMessage(ColorUtil.text(
                    "<red>Failed to update the player's Gems balance."
            ));
            return;
        }

        sender.sendMessage(ColorUtil.text(
                "<green>Successfully " + action + "ed <aqua>"
                        + String.format("%,d", amount)
                        + " Gems <green>for <white>" + target.getName() + "<green>."
        ));

        if (target.isOnline()) {
            target.getPlayer().sendMessage(ColorUtil.text(
                    "<gold>Your Gems balance has been updated by an administrator."
            ));
        }
    }

    private void category(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin category <create|delete|list>"
            ));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 6) {
                    sender.sendMessage(ColorUtil.text(
                            "<red>Usage: /gemsadmin category create <id> <name> <material> <slot>"
                    ));
                    return;
                }

                String id = args[2].toLowerCase(Locale.ROOT);
                String name = args[3];
                Material material = Material.matchMaterial(args[4]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage(ColorUtil.text("<red>Invalid material."));
                    return;
                }

                int slot;

                try {
                    slot = Integer.parseInt(args[5]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(ColorUtil.text("<red>Invalid slot."));
                    return;
                }

                if (slot < 0 || slot > 53) {
                    sender.sendMessage(ColorUtil.text("<red>Slot must be between 0 and 53."));
                    return;
                }

                if (plugin.getCategoryManager().exists(id)) {
                    sender.sendMessage(ColorUtil.text("<red>Category already exists."));
                    return;
                }

                plugin.getCategoryManager().create(id, name, material, slot);

                sender.sendMessage(ColorUtil.text(
                        "<green>Category <white>" + id + " <green>created."
                ));
            }

            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.text(
                            "<red>Usage: /gemsadmin category delete <id>"
                    ));
                    return;
                }

                String id = args[2].toLowerCase(Locale.ROOT);

                if (!plugin.getCategoryManager().exists(id)) {
                    sender.sendMessage(ColorUtil.text("<red>Category not found."));
                    return;
                }

                plugin.getCategoryManager().delete(id);

                sender.sendMessage(ColorUtil.text(
                        "<green>Category <white>" + id + " <green>deleted."
                ));
            }

            case "list" -> {
                List<String> categories = plugin.getCategoryManager().getCategoryIds();

                sender.sendMessage(ColorUtil.text(
                        "<gold>Categories: <white>"
                                + (categories.isEmpty()
                                ? "None"
                                : String.join(", ", categories))
                ));
            }

            default -> sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin category <create|delete|list>"
            ));
        }
    }

    private void reward(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin reward <create|delete|list>"
            ));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 9) {
                    sender.sendMessage(ColorUtil.text(
                            "<red>Usage: /gemsadmin reward create <id> <category> <name> <material> <amount> <price> <slot>"
                    ));
                    return;
                }

                String id = args[2].toLowerCase(Locale.ROOT);
                String category = args[3].toLowerCase(Locale.ROOT);
                String name = args[4];
                Material material = Material.matchMaterial(args[5]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage(ColorUtil.text("<red>Invalid material."));
                    return;
                }

                int amount;
                long price;
                int slot;

                try {
                    amount = Integer.parseInt(args[6]);
                    price = Long.parseLong(args[7]);
                    slot = Integer.parseInt(args[8]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(ColorUtil.text("<red>Invalid number."));
                    return;
                }

                if (amount <= 0 || amount > material.getMaxStackSize()) {
                    sender.sendMessage(ColorUtil.text("<red>Invalid reward amount."));
                    return;
                }

                if (price <= 0) {
                    sender.sendMessage(ColorUtil.text("<red>Price must be greater than zero."));
                    return;
                }

                if (slot < 0 || slot > 53) {
                    sender.sendMessage(ColorUtil.text("<red>Slot must be between 0 and 53."));
                    return;
                }

                if (!plugin.getCategoryManager().exists(category)) {
                    sender.sendMessage(ColorUtil.text("<red>Category does not exist."));
                    return;
                }

                if (plugin.getRewardManager().exists(id)) {
                    sender.sendMessage(ColorUtil.text("<red>Reward already exists."));
                    return;
                }

                plugin.getRewardManager().create(
                        id,
                        category,
                        name,
                        material,
                        amount,
                        price,
                        slot
                );

                sender.sendMessage(ColorUtil.text(
                        "<green>Reward <white>" + id + " <green>created."
                ));
            }

            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.text(
                            "<red>Usage: /gemsadmin reward delete <id>"
                    ));
                    return;
                }

                String id = args[2].toLowerCase(Locale.ROOT);

                if (!plugin.getRewardManager().exists(id)) {
                    sender.sendMessage(ColorUtil.text("<red>Reward not found."));
                    return;
                }

                plugin.getRewardManager().delete(id);

                sender.sendMessage(ColorUtil.text(
                        "<green>Reward <white>" + id + " <green>deleted."
                ));
            }

            case "list" -> {
                List<String> rewards = plugin.getRewardManager().getRewardIds();

                sender.sendMessage(ColorUtil.text(
                        "<gold>Rewards: <white>"
                                + (rewards.isEmpty()
                                ? "None"
                                : String.join(", ", rewards))
                ));
            }

            default -> sender.sendMessage(ColorUtil.text(
                    "<red>Usage: /gemsadmin reward <create|delete|list>"
            ));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ColorUtil.text("<gold><bold>VORTEX GEMS ADMIN"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin reload"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin balance <player>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin add <player> <amount>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin remove <player> <amount>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin set <player> <amount>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin category create <id> <name> <material> <slot>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin category delete <id>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin category list"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin reward create <id> <category> <name> <material> <amount> <price> <slot>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin reward delete <id>"));
        sender.sendMessage(ColorUtil.text("<yellow>/gemsadmin reward list"));
        sender.sendMessage(ColorUtil.text("<dark_gray>━━━━━━━━━━━━━━━━━━━━"));
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
                    "reload", "balance", "add", "remove", "set",
                    "category", "reward", "help"
            ));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("category")) {
            suggestions.addAll(List.of("create", "delete", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
            suggestions.addAll(List.of("create", "delete", "list"));
        } else if (args.length == 2 && List.of("balance", "add", "remove", "set")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.getName() != null) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("category")
                && args[1].equalsIgnoreCase("delete")) {
            suggestions.addAll(plugin.getCategoryManager().getCategoryIds());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reward")
                && args[1].equalsIgnoreCase("delete")) {
            suggestions.addAll(plugin.getRewardManager().getRewardIds());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("reward")
                && args[1].equalsIgnoreCase("create")) {
            suggestions.addAll(plugin.getCategoryManager().getCategoryIds());
        }

        String input = args[args.length - 1].toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(input))
                .toList();
    }
}
