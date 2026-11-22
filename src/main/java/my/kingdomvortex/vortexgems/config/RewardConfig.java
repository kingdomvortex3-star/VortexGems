package my.kingdomvortex.vortexgems.config;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class RewardConfig {

    private final VortexGems plugin;
    private File file;
    private YamlConfiguration config;

    public RewardConfig(VortexGems plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "rewards.yml");

        if (!file.exists()) {
            plugin.saveResource("rewards.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Failed to save rewards.yml: " + exception.getMessage()
            );
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public ConfigurationSection getReward(String id) {
        return config.getConfigurationSection("rewards." + id);
    }

    public Set<String> getRewardIds() {
        ConfigurationSection section = config.getConfigurationSection("rewards");

        return section == null ? Set.of() : section.getKeys(false);
    }

    public boolean exists(String id) {
        return config.contains("rewards." + id);
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    public void delete(String id) {
        config.set("rewards." + id, null);
        save();
    }
}
