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

public class DeleteHomeCommand implements CommandExecutor {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public DeleteHomeCommand(DatabaseManager databaseManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /" + label + " <название>"));
            return true;
        }

        String homeName = args[0];
        boolean success = databaseManager.deleteHome(player.getUniqueId(), homeName);

        if (success) {
            String message = configManager.getMessage("home-deleted",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
        } else {
            String message = configManager.getMessage("home-not-found",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
        }

        return true;
    }
}
