package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.config.PluginMessages;
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

    public SetHomeCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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
            PluginMessages.send(player, "name-too-long");
            return true;
        }

        if (!homeName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
            PluginMessages.send(player, "invalid-name");
            return true;
        }

        int playerLimit = LuckPermsHelper.getHighestLimit(player);

        int homeCount = databaseManager.getHomeCount(player.getUniqueId());
        boolean homeExists = databaseManager.getHome(player.getUniqueId(), homeName) != null;

        // Если лимит не безлимитный (-1) и дом новый (не обновление)
        if (playerLimit != -1 && homeCount >= playerLimit && !homeExists) {
            PluginMessages.send(player, "home-limit-reached", "{limit}", String.valueOf(playerLimit));
            return true;
        }

        // Создание или обновление дома
        boolean success = databaseManager.createHome(player.getUniqueId(), homeName, player.getLocation());

        if (success) {
            PluginMessages.send(player, "home-set", "{name}", homeName);
        } else {
            databaseManager.deleteHome(player.getUniqueId(), homeName);
            databaseManager.createHome(player.getUniqueId(), homeName, player.getLocation());
            PluginMessages.send(player, "home-updated", "{name}", homeName);
        }
        return true;
    }
}
