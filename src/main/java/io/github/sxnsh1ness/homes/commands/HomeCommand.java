package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.database.Home;
import io.github.sxnsh1ness.homes.utils.CooldownManager;
import io.github.sxnsh1ness.homes.utils.TeleportManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class HomeCommand implements CommandExecutor {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;
    private final CooldownManager cooldownManager;

    public HomeCommand(DatabaseManager databaseManager, ConfigManager configManager, TeleportManager teleportManager, CooldownManager cooldownManager) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.teleportManager = teleportManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /home <название>"));
            return true;
        }

        if (!cooldownManager.hasCooldown(player)) {
            int remainingTime = cooldownManager.getRemainingCooldown(player);
            String message = configManager.getMessage("command-on-cooldown",
                    Map.of("time", String.valueOf(remainingTime)));
            player.sendMessage(Component.text(message));
            return true;
        }

        String homeName = args[0];
        Home home = databaseManager.getHome(player.getUniqueId(), homeName);

        if (home == null) {
            String message = configManager.getMessage("home-not-found",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
            return true;
        }

        Location location = home.getLocation();

        if (location.getWorld() == null) {
            String message = configManager.getMessage("world-not-found",
                    Map.of("world", home.getWorldName()));
            player.sendMessage(Component.text(message));
            return true;
        }

        boolean applyOnCommand = configManager.getConfig().getBoolean("cooldown.apply-on-command", true);
        if (applyOnCommand) {
            cooldownManager.setCooldown(player);
        }

        teleportManager.teleportWithDelay(player, location, homeName);
        return true;
    }
}
