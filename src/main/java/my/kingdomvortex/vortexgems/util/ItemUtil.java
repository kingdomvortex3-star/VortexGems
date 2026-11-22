
package my.kingdomvortex.vortexgems.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack create(
            Material material,
            String name,
            List<String> lore
    ) {
        return create(material, 1, name, lore);
    }

    public static ItemStack create(
            Material material,
            int amount,
            String name,
            List<String> lore
    ) {
        Material safeMaterial = material == null
                ? Material.BARRIER
                : material;

        int safeAmount = Math.max(1, Math.min(
                safeMaterial.getMaxStackSize(), amount
        ));

        ItemStack item = new ItemStack(safeMaterial, safeAmount);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (name != null && !name.isBlank()) {
            meta.displayName(ColorUtil.text(name));
        }

        if (lore != null) {
            meta.lore(ColorUtil.lore(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack named(Material material, String name) {
        return create(material, name, List.of());
    }

    public static ItemStack withLore(
            Material material,
            String name,
            String... lore
    ) {
        return create(material, name, List.of(lore));
    }

    public static ItemStack filler() {
        return create(
                Material.GRAY_STAINED_GLASS_PANE,
                1,
                "<dark_gray>",
                new ArrayList<>()
        );
    }

    public static ItemStack fromMaterial(String materialName) {
        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            material = Material.BARRIER;
        }

        return new ItemStack(material);
    }

    public static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static void setName(ItemStack item, String name) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.displayName(ColorUtil.text(name));
        item.setItemMeta(meta);
    }

    public static void setLore(ItemStack item, List<String> lore) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.lore(ColorUtil.lore(lore));
        item.setItemMeta(meta);
    }
}
