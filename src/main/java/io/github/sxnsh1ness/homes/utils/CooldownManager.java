package io.github.sxnsh1ness.homes.utils;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final ConfigManager configManager;
    private final Map<UUID, Long> homeCooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.homeCooldowns = new HashMap<>();
    }

    public boolean hasCooldown(Player player) {
        if (!homeCooldowns.containsKey(player.getUniqueId())) {
            return true;
        }

        long cooldownTime = configManager.getConfig().getLong("cooldown.home-command", 3) * 1000;
        long lastUse = homeCooldowns.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastUse) >= cooldownTime;
    }

    public int getRemainingCooldown(Player player) {
        if (!homeCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }

        long cooldownTime = configManager.getConfig().getLong("cooldown.home-command", 3) * 1000;
        long lastUse = homeCooldowns.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        long remaining = cooldownTime - (currentTime - lastUse);

        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public void setCooldown(Player player) {
        homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void resetCooldown(Player player) {
        homeCooldowns.remove(player.getUniqueId());
    }

    public void clearAll() {
        homeCooldowns.clear();
    }
}
