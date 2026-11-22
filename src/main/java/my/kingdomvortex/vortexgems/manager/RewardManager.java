
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RewardManager {

    private final VortexGems plugin;

    public RewardManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.getConfigManager().reloadRewards();
    }

    public Set<String> getRewardIds() {
        ConfigurationSection section = getConfig()
                .getConfigurationSection("rewards");

        return section == null ? Set.of() : section.getKeys(false);
    }

    public List<String> getRewardsInCategory(String category) {
        List<String> result = new ArrayList<>();

        for (String id : getRewardIds()) {
            if (isEnabled(id)
                    && getCategory(id).equalsIgnoreCase(category)) {
                result.add(id);
            }
        }

        return result;
    }

    public boolean exists(String id) {
        return id != null && getConfig().contains("rewards." + id);
    }

    public boolean isEnabled(String id) {
        return exists(id)
                && getConfig().getBoolean("rewards." + id + ".enabled", false);
    }

    public String getName(String id) {
        return getConfig().getString("rewards." + id + ".name", id);
    }

    public String getCategory(String id) {
        return getConfig().getString("rewards." + id + ".category", "");
    }

    public Material getMaterial(String id) {
        ItemStack stored = getItemStack(id);

        if (stored != null) {
            return stored.getType();
        }

        Material material = Material.matchMaterial(
                getConfig().getString("rewards." + id + ".material", "CHEST")
        );

        return material == null ? Material.CHEST : material;
    }

    public int getAmount(String id) {
        ItemStack stored = getItemStack(id);

        if (stored != null) {
            return stored.getAmount();
        }

        return Math.max(1, Math.min(
                64,
                getConfig().getInt("rewards." + id + ".amount", 1)
        ));
    }

    public long getPrice(String id) {
        return Math.max(
                0,
                getConfig().getLong("rewards." + id + ".price", 0)
        );
    }

    public int getSlot(String id) {
        return getConfig().getInt("rewards." + id + ".slot", 0);
    }

    public String getType(String id) {
        return getConfig().getString(
                "rewards." + id + ".type",
                "ITEM"
        ).toUpperCase();
    }

    public List<String> getCommands(String id) {
        return getConfig().getStringList(
                "rewards." + id + ".commands"
        );
    }

    public List<String> getLore(String id) {
        return getConfig().getStringList(
                "rewards." + id + ".lore"
        );
    }

    /**
     * Gets the complete stored reward ItemStack.
     * Returns a clone so callers cannot modify the stored object accidentally.
     */
    public ItemStack getItemStack(String id) {
        if (!exists(id)) {
            return null;
        }

        Object value = getConfig().get(
                "rewards." + id + ".item"
        );

        if (value instanceof ItemStack item && !item.getType().isAir()) {
            return item.clone();
        }

        Material material = Material.matchMaterial(
                getConfig().getString(
                        "rewards." + id + ".material",
                        "CHEST"
                )
        );

        if (material == null || material.isAir()) {
            return null;
        }

        int amount = Math.max(1, Math.min(
                64,
                getConfig().getInt("rewards." + id + ".amount", 1)
        ));

        return new ItemStack(material, amount);
    }

    /**
     * Creates a reward using the complete item from an inventory.
     */
    public boolean createItemReward(
            String id,
            String category,
            ItemStack item,
            long price,
            int slot
    ) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        if (exists(id)
                || !plugin.getCategoryManager().exists(category)
                || item == null
                || item.getType().isAir()
                || price < 0
                || slot < 0
                || slot >= 54) {
            return false;
        }

        ItemStack storedItem = item.clone();

        String path = "rewards." + id;

        getConfig().set(path + ".enabled", true);
        getConfig().set(path + ".category", category);
        getConfig().set(path + ".name", storedItem.getType().name());
        getConfig().set(path + ".material", storedItem.getType().name());
        getConfig().set(path + ".amount", storedItem.getAmount());
        getConfig().set(path + ".price", price);
        getConfig().set(path + ".slot", slot);
        getConfig().set(path + ".type", "ITEM");
        getConfig().set(path + ".item", storedItem);
        getConfig().set(path + ".commands", List.of());
        getConfig().set(path + ".lore", List.of());

        plugin.getConfigManager().saveRewards();
        return true;
    }

    public boolean create(
            String id,
            String category,
            String name,
            Material material,
            int amount,
            long price,
            int slot
    ) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        if (exists(id)
                || !plugin.getCategoryManager().exists(category)) {
            return false;
        }

        if (material == null
                || material.isAir()
                || amount < 1
                || amount > 64
                || price < 0
                || slot < 0
                || slot >= 54) {
            return false;
        }

        String path = "rewards." + id;

        getConfig().set(path + ".enabled", true);
        getConfig().set(path + ".category", category);
        getConfig().set(path + ".name", name);
        getConfig().set(path + ".material", material.name());
        getConfig().set(path + ".amount", amount);
        getConfig().set(path + ".price", price);
        getConfig().set(path + ".slot", slot);
        getConfig().set(path + ".type", "ITEM");
        getConfig().set(path + ".item", new ItemStack(material, amount));
        getConfig().set(path + ".commands", List.of());
        getConfig().set(path + ".lore", List.of(
                "",
                "<gray>Click to purchase."
        ));

        plugin.getConfigManager().saveRewards();
        return true;
    }

    public boolean delete(String id) {
        if (!exists(id)) {
            return false;
        }

        getConfig().set("rewards." + id, null);
        plugin.getConfigManager().saveRewards();
        return true;
    }

    public boolean setEnabled(String id, boolean enabled) {
        return setValue(id, "enabled", enabled);
    }

    public boolean setName(String id, String name) {
        return setValue(id, "name", name);
    }

    public boolean setCategory(String id, String category) {
        if (!plugin.getCategoryManager().exists(category)) {
            return false;
        }

        return setValue(id, "category", category);
    }

    public boolean setMaterial(String id, Material material) {
        if (material == null || material.isAir()) {
            return false;
        }

        if (!setValue(id, "material", material.name())) {
            return false;
        }

        return setValue(
                id,
                "item",
                new ItemStack(
                        material,
                        getAmount(id)
                )
        );
    }

    public boolean setItemStack(String id, ItemStack item) {
        if (!exists(id)
                || item == null
                || item.getType().isAir()) {
            return false;
        }

        ItemStack stored = item.clone();

        getConfig().set("rewards." + id + ".item", stored);
        getConfig().set(
                "rewards." + id + ".material",
                stored.getType().name()
        );
        getConfig().set(
                "rewards." + id + ".amount",
                stored.getAmount()
        );

        plugin.getConfigManager().saveRewards();
        return true;
    }

    public boolean setAmount(String id, int amount) {
        if (amount < 1 || amount > 64 || !exists(id)) {
            return false;
        }

        ItemStack item = getItemStack(id);

        if (item != null) {
            item.setAmount(amount);
            getConfig().set("rewards." + id + ".item", item);
        }

        getConfig().set("rewards." + id + ".amount", amount);
        plugin.getConfigManager().saveRewards();
        return true;
    }

    public boolean setPrice(String id, long price) {
        if (price < 0) {
            return false;
        }

        return setValue(id, "price", price);
    }

    public boolean setSlot(String id, int slot) {
        if (slot < 0 || slot >= 54) {
            return false;
        }

        return setValue(id, "slot", slot);
    }

    public boolean setType(String id, String type) {
        if (!type.equalsIgnoreCase("ITEM")
                && !type.equalsIgnoreCase("COMMAND")) {
            return false;
        }

        return setValue(id, "type", type.toUpperCase());
    }

    public boolean setCommands(String id, List<String> commands) {
        return setValue(id, "commands", commands);
    }

    public boolean setLore(String id, List<String> lore) {
        return setValue(id, "lore", lore);
    }

    public boolean setValue(String id, String key, Object value) {
        if (!exists(id) || key == null || key.isBlank()) {
            return false;
        }

        getConfig().set("rewards." + id + "." + key, value);
        plugin.getConfigManager().saveRewards();
        return true;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfigManager().getRewards();
    }
}
