package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.utils.LuckPermsHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class SetHomeCommand implements CommandExecutor {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public SetHomeCommand(DatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /sethome <название>", NamedTextColor.RED));
            return true;
        }

        String homeName = args[0];

        if (homeName.length() > 16) {
            String message = configManager.getMessage("name-too-long");
            player.sendMessage(Component.text(message));
            return true;
        }

        if (!homeName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
            String message = configManager.getMessage("invalid-name");
            player.sendMessage(Component.text(message));
            return true;
        }

        int playerLimit = LuckPermsHelper.getHighestLimit(player, configManager);

        int homeCount = databaseManager.getHomeCount(player.getUniqueId());
        boolean homeExists = databaseManager.getHome(player.getUniqueId(), homeName) != null;

        // Если лимит не безлимитный (-1) и дом новый (не обновление)
        if (playerLimit != -1 && homeCount >= playerLimit && !homeExists) {
            String message = configManager.getMessage("home-limit-reached",
                    Map.of("limit", String.valueOf(playerLimit)));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Создание или обновление дома
        boolean success = databaseManager.createHome(player.getUniqueId(), homeName, player.getLocation());

        if (success) {
            String message = configManager.getMessage("home-set",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
        } else {
            databaseManager.deleteHome(player.getUniqueId(), homeName);
            databaseManager.createHome(player.getUniqueId(), homeName, player.getLocation());
            String message = configManager.getMessage("home-updated",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
        }
        return true;
    }
}
