package io.github.sxnsh1ness.homes.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    @Getter
    private FileConfiguration config;
    private Map<String, Integer> groupLimits;
    @Getter
    private int defaultLimit;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        groupLimits = new HashMap<>();

        if (config.contains("group-limits")) {
            for (String group : config.getConfigurationSection("group-limits").getKeys(false)) {
                int limit = config.getInt("group-limits." + group);
                groupLimits.put(group.toLowerCase(), limit);
            }
        }

        defaultLimit = config.getInt("default-limit", 3);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public int getLimitForGroup(String group) {
        if (group == null) {
            return defaultLimit;
        }
        return groupLimits.getOrDefault(group.toLowerCase(), defaultLimit);
    }

    public String getMessage(String key) {
        return translateColors(config.getString("messages." + key, "&cСообщение не найдено: " + key));
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    private String translateColors(String message) {
        return message.replace("&", "§");
    }
}
