package io.github.sxnsh1ness.homes.utils;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;

public class LuckPermsHelper {

    private static LuckPerms luckPerms = null;

    public static boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            return true;
        }
        return false;
    }

    public static String getPrimaryGroup(Player player) {
        if (luckPerms == null) {
            return null;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return null;
        }

        return user.getPrimaryGroup();
    }

    public static Collection<String> getAllGroups(Player player) {
        if (luckPerms == null) {
            return null;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return null;
        }

        return user.getNodes().stream()
                .filter(InheritanceNode.class::isInstance)
                .map(InheritanceNode.class::cast)
                .map(InheritanceNode::getGroupName)
                .toList();
    }

    public static int getHighestLimit(Player player, ConfigManager configManager) {
        if (luckPerms == null) {
            return configManager.getDefaultLimit();
        }

        Collection<String> groups = getAllGroups(player);
        if (groups == null || groups.isEmpty()) {
            return configManager.getDefaultLimit();
        }

        int highestLimit = configManager.getDefaultLimit();

        for (String group : groups) {
            int limit = configManager.getLimitForGroup(group);
            if (limit == -1) {
                return -1;
            }
            if (limit > highestLimit) {
                highestLimit = limit;
            }
        }

        return highestLimit;
    }
}
