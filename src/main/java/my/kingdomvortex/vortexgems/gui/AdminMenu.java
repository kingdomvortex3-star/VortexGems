
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class AdminMenu {

    private final VortexGems plugin;

    public AdminMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = 45;

        MenuHolder holder = new MenuHolder("ADMIN");

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                Component.text("VORTEX GEMS ADMIN")
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        inventory.setItem(11, ItemUtil.create(
                Material.CHEST,
                1,
                "<gold><bold>CATEGORY MANAGER</bold>",
                List.of(
                        "",
                        "<gray>Create, edit and delete categories.",
                        "",
                        "<yellow>Click to open"
                )
        ));

        inventory.setItem(13, ItemUtil.create(
                Material.DIAMOND,
                1,
                "<aqua><bold>REWARD MANAGER</bold>",
                List.of(
                        "",
                        "<gray>Create and edit shop rewards.",
                        "",
                        "<yellow>Click to open"
                )
        ));

        inventory.setItem(15, ItemUtil.create(
                Material.COMPARATOR,
                1,
                "<light_purple><bold>GEMS SETTINGS</bold>",
                List.of(
                        "",
                        "<gray>Configure buying and economy.",
                        "",
                        "<yellow>Click to open"
                )
        ));

        inventory.setItem(31, ItemUtil.create(
                Material.BARRIER,
                1,
                "<red><bold>CLOSE</bold>",
                List.of(
                        "",
                        "<gray>Close administration menu."
                )
        ));

        player.openInventory(inventory);
    }
}
