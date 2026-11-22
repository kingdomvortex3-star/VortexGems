
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardCreateMenu {

    private static final Map<UUID, Long> PRICES =
            new ConcurrentHashMap<>();

    private final VortexGems plugin;

    public RewardCreateMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String category) {
        if (!plugin.getCategoryManager().exists(category)) {
            player.sendMessage(ColorUtil.text("<red>Category not found."));
            plugin.getMenuManager().openRewardEditor(player);
            return;
        }

        MenuHolder holder = new MenuHolder("REWARD_CREATE", category);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                ColorUtil.text("<dark_gray>ᴄʀᴇᴀᴛᴇ ʀᴇᴡᴀʀᴅ")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 27; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        inventory.setItem(13, ItemUtil.create(
                Material.HOPPER,
                1,
                "<yellow><bold>ʀᴇᴡᴀʀᴅ ɪᴛᴇᴍ</bold>",
                List.of(
                        "",
                        "<gray>ᴘʟᴀᴄᴇ ᴛʜᴇ ɪᴛᴇᴍ ʏᴏᴜ ᴡᴀɴᴛ",
                        "<gray>ᴛᴏ ѕᴇʟʟ ɪɴ ᴛʜɪѕ ѕʟᴏᴛ.",
                        "",
                        "<yellow>ᴅʀᴀɢ ɪᴛᴇᴍ ʜᴇʀᴇ"
                )
        ));

        inventory.setItem(11, ItemUtil.create(
                Material.GOLD_INGOT,
                1,
                "<gold><bold>ѕᴇᴛ ɢᴇᴍѕ ᴘʀɪᴄᴇ</bold>",
                List.of(
                        "",
                        "<gray>ᴄᴜʀʀᴇɴᴛ: <aqua>" +
                                PRICES.getOrDefault(player.getUniqueId(), 0L) +
                                " ɢᴇᴍѕ",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ"
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<green><bold>ѕᴀᴠᴇ ʀᴇᴡᴀʀᴅ</bold>",
                List.of(
                        "",
                        "<gray>ѕᴀᴠᴇ ᴛʜɪѕ ɪᴛᴇᴍ ᴛᴏ ᴄᴀᴛᴇɢᴏʀʏ.",
                        "",
                        "<yellow>ᴄʟɪᴄᴋ ᴛᴏ ѕᴀᴠᴇ"
                )
        ));

        inventory.setItem(22, ItemUtil.create(
                Material.BARRIER,
                1,
                "<red><bold>ᴄᴀɴᴄᴇʟ</bold>",
                List.of("", "<gray>ᴄᴀɴᴄᴇʟ ᴄʀᴇᴀᴛɪᴏɴ")
        ));

        player.openInventory(inventory);
    }

    public void promptPrice(Player player, String category) {
        plugin.getInputManager().request(player, input -> {
            long price;

            try {
                price = Long.parseLong(input);
            } catch (NumberFormatException exception) {
                player.sendMessage(ColorUtil.text(
                        "<red>ᴘʀɪᴄᴇ ᴍᴜѕᴛ ʙᴇ ᴀ ᴡʜᴏʟᴇ ɴᴜᴍʙᴇʀ."
                ));
                open(player, category);
                return;
            }

            if (price < 0) {
                player.sendMessage(ColorUtil.text(
                        "<red>ᴘʀɪᴄᴇ ᴄᴀɴɴᴏᴛ ʙᴇ ɴᴇɢᴀᴛɪᴠᴇ."
                ));
                open(player, category);
                return;
            }

            PRICES.put(player.getUniqueId(), price);

            player.sendMessage(ColorUtil.text(
                    "<green>ᴘʀɪᴄᴇ ѕᴇᴛ: <aqua>" + price + " ɢᴇᴍѕ"
            ));

            open(player, category);
        });
    }

    public boolean save(Player player, Inventory inventory, String category) {
        ItemStack item = inventory.getItem(13);

        if (item == null
                || item.getType().isAir()
                || item.getType() == Material.HOPPER) {
            player.sendMessage(ColorUtil.text(
                    "<red>ᴘʟᴀᴄᴇ ᴀ ᴠᴀʟɪᴅ ɪᴛᴇᴍ ꜰɪʀѕᴛ."
            ));
            return false;
        }

        long price = PRICES.getOrDefault(player.getUniqueId(), -1L);

        if (price < 0) {
            player.sendMessage(ColorUtil.text(
                    "<red>ѕᴇᴛ ᴛʜᴇ ɢᴇᴍѕ ᴘʀɪᴄᴇ ꜰɪʀѕᴛ."
            ));
            return false;
        }

        int freeSlot =
                plugin.getRewardManager().getNextAvailableSlot(category);

        if (freeSlot < 0) {
            player.sendMessage(ColorUtil.text(
                    "<red>ᴛʜɪѕ ᴄᴀᴛᴇɢᴏʀʏ ɪѕ ꜰᴜʟʟ."
            ));
            return false;
        }

        String baseId = item.getType().name().toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");

        String id = baseId;
        int number = 2;

        while (plugin.getRewardManager().exists(id)) {
            id = baseId + "_" + number++;
        }

        boolean created = plugin.getRewardManager().createItemReward(
                id,
                category,
                item.clone(),
                price,
                freeSlot
        );

        if (!created) {
            player.sendMessage(ColorUtil.text(
                    "<red>ꜰᴀɪʟᴇᴅ ᴛᴏ ѕᴀᴠᴇ ʀᴇᴡᴀʀᴅ."
            ));
            return false;
        }

        PRICES.remove(player.getUniqueId());

        player.sendMessage(ColorUtil.text(
                "<green>ʀᴇᴡᴀʀᴅ ѕᴀᴠᴇᴅ: <white>" + id
                        + " <gray>| <aqua>" + price + " ɢᴇᴍѕ"
        ));

        return true;
    }

    public static void clearPrice(UUID uuid) {
        PRICES.remove(uuid);
    }
}
