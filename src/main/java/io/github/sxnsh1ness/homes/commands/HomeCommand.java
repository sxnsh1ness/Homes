package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.database.Home;
import io.github.sxnsh1ness.homes.utils.CooldownManager;
import io.github.sxnsh1ness.homes.utils.TeleportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
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


        // Проверка подкоманд
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "invite":
                    return handleInvite(player, args);
                case "uninvite":
                    return handleUninvite(player, args);
                case "visit":
                    return handleVisit(player, args);
                case "list":
                    if (player.hasPermission("homes.admin")) {
                        return handleAdminList(player, args);
                    }
                    break;
                case "tp":
                case "teleport":
                    if (player.hasPermission("homes.admin")) {
                        return handleAdminTeleport(player, args);
                    }
                    break;
            }
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /home <название>"));
            player.sendMessage(Component.text("Или используйте: /home invite|uninvite|visit", NamedTextColor.GRAY));
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

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Использование: /home invite <игрок> <дом>", NamedTextColor.YELLOW));
            return true;
        }

        String targetPlayerName = args[1];
        String homeName = args[2];

        // Проверка существования дома
        Home home = databaseManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            String message = configManager.getMessage("home-not-found",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Поиск игрока
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Игрок '" + targetPlayerName + "' не найден или не в сети!", NamedTextColor.RED));
            return true;
        }

        // Проверка, что не приглашаешь себя
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Вы не можете пригласить себя!", NamedTextColor.RED));
            return true;
        }

        // Проверка, не приглашён ли уже
        if (databaseManager.isInvited(player.getUniqueId(), homeName, targetPlayer.getUniqueId())) {
            player.sendMessage(Component.text("Игрок '" + targetPlayer.getName() + "' уже приглашён в дом '" + homeName + "'!", NamedTextColor.YELLOW));
            return true;
        }

        // Приглашение
        boolean success = databaseManager.invitePlayer(player.getUniqueId(), homeName, targetPlayer.getUniqueId());

        if (success) {
            player.sendMessage(Component.text("✓ Вы пригласили игрока '" + targetPlayer.getName() + "' в дом '" + homeName + "'!", NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text("✉ Игрок '" + player.getName() + "' пригласил вас в свой дом '" + homeName + "'!", NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text("Используйте /home visit " + player.getName() + " " + homeName, NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Ошибка приглашения игрока!", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleUninvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Использование: /home uninvite <игрок> <дом>", NamedTextColor.YELLOW));
            return true;
        }

        String targetPlayerName = args[1];
        String homeName = args[2];

        // Проверка существования дома
        Home home = databaseManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            String message = configManager.getMessage("home-not-found",
                    Map.of("name", homeName));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Поиск игрока (может быть офлайн)
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        // Проверка, приглашён ли игрок
        if (!databaseManager.isInvited(player.getUniqueId(), homeName, targetPlayer.getUniqueId())) {
            player.sendMessage(Component.text("Игрок '" + targetPlayerName + "' не приглашён в дом '" + homeName + "'!", NamedTextColor.YELLOW));
            return true;
        }

        // Удаление приглашения
        boolean success = databaseManager.uninvitePlayer(player.getUniqueId(), homeName, targetPlayer.getUniqueId());

        if (success) {
            player.sendMessage(Component.text("✓ Вы удалили приглашение игрока '" + targetPlayerName + "' из дома '" + homeName + "'!", NamedTextColor.GREEN));

            // Уведомляем игрока если он онлайн
            if (targetPlayer.isOnline()) {
                ((Player) targetPlayer).sendMessage(Component.text("Игрок '" + player.getName() + "' удалил ваше приглашение в дом '" + homeName + "'!", NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("Ошибка удаления приглашения!", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleVisit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Использование: /home visit <игрок> <дом>", NamedTextColor.YELLOW));
            return true;
        }

        String ownerName = args[1];
        String homeName = args[2];

        // Поиск владельца дома
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);

        // Проверка существования дома
        Home home = databaseManager.getHome(owner.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(Component.text("Дом '" + homeName + "' игрока '" + ownerName + "' не найден!", NamedTextColor.RED));
            return true;
        }

        // Проверка приглашения
        if (!databaseManager.isInvited(owner.getUniqueId(), homeName, player.getUniqueId())) {
            player.sendMessage(Component.text("Вы не приглашены в дом '" + homeName + "' игрока '" + ownerName + "'!", NamedTextColor.RED));
            return true;
        }

        // Проверка кулдауна
        if (!cooldownManager.hasCooldown(player)) {
            int remainingTime = cooldownManager.getRemainingCooldown(player);
            String message = configManager.getMessage("command-cooldown",
                    Map.of("time", String.valueOf(remainingTime)));
            player.sendMessage(Component.text(message));
            return true;
        }

        Location location = home.getLocation();

        // Проверка мира
        if (location.getWorld() == null) {
            String message = configManager.getMessage("world-not-found",
                    Map.of("world", home.getWorldName()));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Устанавливаем кулдаун если нужно
        boolean applyOnCommand = configManager.getConfig().getBoolean("cooldown.apply-on-command", true);
        if (applyOnCommand) {
            cooldownManager.setCooldown(player);
        }

        // Телепортация
        player.sendMessage(Component.text("Телепортация в дом '" + homeName + "' игрока '" + ownerName + "'...", NamedTextColor.GREEN));
        teleportManager.teleportWithDelay(player, location, ownerName + ":" + homeName);

        return true;
    }

    // /home list <игрок> - админская команда
    private boolean handleAdminList(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Использование: /home list <игрок>", NamedTextColor.YELLOW));
            return true;
        }

        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        List<Home> homes = databaseManager.getHomes(targetPlayer.getUniqueId());

        if (homes.isEmpty()) {
            player.sendMessage(Component.text("У игрока '" + targetPlayerName + "' нет домов!", NamedTextColor.YELLOW));
            return true;
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Дома игрока: " + targetPlayerName, NamedTextColor.AQUA));
        player.sendMessage(Component.text("  Всего: " + homes.size(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD));

        for (Home home : homes) {
            int invitedCount = databaseManager.getInvitedPlayers(targetPlayer.getUniqueId(), home.getName()).size();
            player.sendMessage(Component.text("  • ", NamedTextColor.YELLOW)
                    .append(Component.text(home.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" | " + home.getWorldName(), NamedTextColor.GRAY))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Приглашено: " + invitedCount, NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("    " + String.format("%.0f, %.0f, %.0f", home.getX(), home.getY(), home.getZ()), NamedTextColor.DARK_GRAY));
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Используйте /home tp " + targetPlayerName + " <дом>", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        return true;
    }

    // /home tp <игрок> <дом> - админская команда
    private boolean handleAdminTeleport(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Использование: /home tp <игрок> <дом>", NamedTextColor.YELLOW));
            return true;
        }

        String targetPlayerName = args[1];
        String homeName = args[2];

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        // Проверка существования дома
        Home home = databaseManager.getHome(targetPlayer.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(Component.text("Дом '" + homeName + "' игрока '" + targetPlayerName + "' не найден!", NamedTextColor.RED));
            return true;
        }

        Location location = home.getLocation();

        // Проверка мира
        if (location.getWorld() == null) {
            String message = configManager.getMessage("world-not-found",
                    Map.of("world", home.getWorldName()));
            player.sendMessage(Component.text(message));
            return true;
        }

        // Админы телепортируются мгновенно без кулдауна и задержки
        player.teleport(location);
        player.sendMessage(Component.text("✓ Вы телепортировались в дом '" + homeName + "' игрока '" + targetPlayerName + "'!", NamedTextColor.GREEN));

        return true;
    }
}
