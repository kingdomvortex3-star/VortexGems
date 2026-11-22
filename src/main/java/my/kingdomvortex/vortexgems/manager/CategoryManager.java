
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CategoryManager {

    private final VortexGems plugin;

    public CategoryManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.getConfigManager().reloadCategories();
    }

    public Set<String> getCategoryIds() {
        ConfigurationSection section = getConfig()
                .getConfigurationSection("categories");

        return section == null ? Set.of() : section.getKeys(false);
    }

    public List<String> getEnabledCategories() {
        List<String> result = new ArrayList<>();

        for (String id : getCategoryIds()) {
            if (isEnabled(id)) {
                result.add(id);
            }
        }

        return result;
    }

    public boolean exists(String id) {
        return id != null && getConfig().contains("categories." + id);
    }

    public boolean isEnabled(String id) {
        return exists(id) &&
                getConfig().getBoolean("categories." + id + ".enabled", false);
    }

    public String getName(String id) {
        return getConfig().getString("categories." + id + ".name", id);
    }

    public Material getMaterial(String id) {
        String value = getConfig().getString(
                "categories." + id + ".material", "CHEST"
        );

        Material material = Material.matchMaterial(value);
        return material == null ? Material.CHEST : material;
    }

    public int getAmount(String id) {
        return Math.max(1, Math.min(64,
                getConfig().getInt("categories." + id + ".amount", 1)));
    }

    public int getSlot(String id) {
        return getConfig().getInt("categories." + id + ".slot", 0);
    }

    public List<String> getLore(String id) {
        return getConfig().getStringList("categories." + id + ".lore");
    }

    public boolean create(
            String id,
            String name,
            Material material,
            int slot
    ) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        if (exists(id) || material == null || slot < 0 || slot >= 54) {
            return false;
        }

        String path = "categories." + id;
        FileConfiguration config = getConfig();

        config.set(path + ".enabled", true);
        config.set(path + ".name", name);
        config.set(path + ".material", material.name());
        config.set(path + ".amount", 1);
        config.set(path + ".slot", slot);
        config.set(path + ".lore", List.of(
                "",
                "<gray>Click to open this category."
        ));

        plugin.getConfigManager().saveCategories();
        return true;
    }

    public boolean delete(String id) {
        if (!exists(id)) {
            return false;
        }

        getConfig().set("categories." + id, null);
        plugin.getConfigManager().saveCategories();
        return true;
    }

    public boolean setEnabled(String id, boolean enabled) {
        return setValue(id, "enabled", enabled);
    }

    public boolean setName(String id, String name) {
        return setValue(id, "name", name);
    }

    public boolean setMaterial(String id, Material material) {
        if (material == null) return false;
        return setValue(id, "material", material.name());
    }

    public boolean setSlot(String id, int slot) {
        if (slot < 0 || slot >= 54) return false;
        return setValue(id, "slot", slot);
    }

    public boolean setLore(String id, List<String> lore) {
        return setValue(id, "lore", lore);
    }

    public boolean setValue(String id, String key, Object value) {
        if (!exists(id) || key == null || key.isBlank()) {
            return false;
        }

        getConfig().set("categories." + id + "." + key, value);
        plugin.getConfigManager().saveCategories();
        return true;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfigManager().getCategories();
    }
}
