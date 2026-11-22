
package my.kingdomvortex.vortexgems.config;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class ConfigManager {

    private final VortexGems plugin;

    private File categoriesFile;
    private File rewardsFile;
    private File messagesFile;

    private FileConfiguration categories;
    private FileConfiguration rewards;
    private FileConfiguration messages;

    public ConfigManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void load() {
        categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        categories = YamlConfiguration.loadConfiguration(categoriesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public FileConfiguration getCategories() {
        return categories;
    }

    public FileConfiguration getRewards() {
        return rewards;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public void saveCategories() {
        save(categories, categoriesFile);
    }

    public void saveRewards() {
        save(rewards, rewardsFile);
    }

    public void saveMessages() {
        save(messages, messagesFile);
    }

    public void reloadCategories() {
        categories = YamlConfiguration.loadConfiguration(categoriesFile);
    }

    public void reloadRewards() {
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void save(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Failed to save " + file.getName() + ": " + exception.getMessage()
            );
        }
    }
}
