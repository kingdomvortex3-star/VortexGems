package my.kingdomvortex.vortexgems.config;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageConfig {

    private final VortexGems plugin;
    private File file;
    private YamlConfiguration config;

    public MessageConfig(VortexGems plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
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
                    "Failed to save messages.yml: " + exception.getMessage()
            );
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public String get(String path) {
        return config.getString(path, "");
    }

    public String get(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }
    }
