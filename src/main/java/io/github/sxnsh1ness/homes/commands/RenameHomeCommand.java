package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class RenameHomeCommand implements CommandExecutor {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public RenameHomeCommand(DatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Использование: /renamehome <старое_название> <новое_название>"));
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        if (newName.length() > 16) {
            String message = configManager.getMessage("name-too-long");
            player.sendMessage(Component.text(message));
            return true;
        }

        if (!newName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
            String message = configManager.getMessage("invalid-name");
            player.sendMessage(Component.text(message));
            return true;
        }

        // Проверка, существует ли старый дом
        if (databaseManager.getHome(player.getUniqueId(), oldName) == null) {
            String message = configManager.getMessage("home-not-found",
                    Map.of("name", oldName));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Проверка, не существует ли уже дом с новым названием
        if (databaseManager.getHome(player.getUniqueId(), newName) != null) {
            String message = configManager.getMessage("home-exists",
                    Map.of("name", newName));
            player.sendMessage(Component.text(message));
            return true;
        }

        boolean success = databaseManager.renameHome(player.getUniqueId(), oldName, newName);

        if (success) {
            String message = configManager.getMessage("home-renamed",
                    Map.of("old", oldName, "new", newName));
            player.sendMessage(Component.text(message));
        } else {
            player.sendMessage(Component.text("Ошибка переименования дома!"));
        }

        return true;
    }
}
