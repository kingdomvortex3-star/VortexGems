
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import my.kingdomvortex.vortexgems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class CategoryEditorMenu {

    private final VortexGems plugin;

    public CategoryEditorMenu(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder("CATEGORY_EDITOR");

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ColorUtil.text(
                        "<gradient:#00E5FF:#8A2BE2><bold>VORTEX CATEGORY MANAGER</bold></gradient>"
                )
        );

        holder.setInventory(inventory);

        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, ItemUtil.filler());
        }

        int displaySlot = 0;

        for (String id : plugin.getCategoryManager().getCategoryIds()) {
            if (displaySlot >= 45) {
                break;
            }

            String name = plugin.getCategoryManager().getName(id);
            int categorySlot = plugin.getCategoryManager().getSlot(id);
            boolean enabled = plugin.getCategoryManager().isEnabled(id);

            inventory.setItem(displaySlot++, ItemUtil.create(
                    plugin.getCategoryManager().getMaterial(id),
                    plugin.getCategoryManager().getAmount(id),
                    "<gradient:#55E7FF:#A78BFA><bold>" + name + "</bold></gradient>",
                    List.of(
                            "",
                            "<dark_gray>━━━━━━━━━━━━━━━━",
                            "<gray>Category ID: <white>" + id,
                            "<gray>Display Slot: <aqua>" + (categorySlot + 1),
                            "<gray>Status: " + (enabled
                                    ? "<green>ENABLED"
                                    : "<red>DISABLED"),
                            "<dark_gray>━━━━━━━━━━━━━━━━",
                            "",
                            "<yellow>LEFT CLICK <gray>Change slot",
                            "<red>RIGHT CLICK <gray>Delete category",
                            "<light_purple>MIDDLE CLICK <gray>Toggle status",
                            ""
                    )
            ));
        }

        inventory.setItem(45, ItemUtil.create(
                Material.LIME_DYE,
                1,
                "<gradient:#39FF88:#00BFA5><bold>CREATE CATEGORY</bold></gradient>",
                List.of(
                        "",
                        "<gray>Create a new shop category.",
                        "",
                        "<yellow>Click to create"
                )
        ));

        inventory.setItem(49, ItemUtil.create(
                Material.ARROW,
                1,
                "<gradient:#FF6B6B:#FFB86B><bold>BACK</bold></gradient>",
                List.of(
                        "",
                        "<gray>Return to admin menu."
                )
        ));

        inventory.setItem(53, ItemUtil.create(
                Material.BOOK,
                1,
                "<aqua><bold>CATEGORY GUIDE</bold>",
                List.of(
                        "",
                        "<gray>Manage your shop categories.",
                        "",
                        "<yellow>Left Click <dark_gray>- <white>Change slot",
                        "<red>Right Click <dark_gray>- <white>Delete",
                        "<light_purple>Middle Click <dark_gray>- <white>Toggle status",
                        "",
                        "<gray>Slot numbers: <aqua>1 - 54",
                        "<red>Slot 50 is reserved for Back."
                )
        ));

        player.openInventory(inventory);
    }
}
