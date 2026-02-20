package io.github.sxnsh1ness.homes.listeners;

import io.github.sxnsh1ness.homes.HomesPlugin;
import io.github.sxnsh1ness.homes.config.PluginMessages;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.gui.HomeGUI;
import io.github.sxnsh1ness.homes.gui.InviteGUI;
import io.github.sxnsh1ness.homes.utils.LuckPermsHelper;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final DatabaseManager databaseManager;
    private final HomeGUI homeGUI;
    @Setter
    private InviteGUI inviteGUI;

    public ChatListener(DatabaseManager databaseManager, HomeGUI homeGUI) {
        this.databaseManager = databaseManager;
        this.homeGUI = homeGUI;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<UUID, String> pendingRename = homeGUI.getPendingRename();
        Map<UUID, Boolean> pendingCreate = homeGUI.getPendingCreate();

        // Проверка на приглашение из InviteGUI
        if (inviteGUI != null) {
            Map<UUID, Boolean> pendingInvite = inviteGUI.getPendingInvite();
            Map<UUID, String> openedHome = inviteGUI.getOpenedHome();

            if (pendingInvite.containsKey(uuid)) {
                event.setCancelled(true);
                pendingInvite.remove(uuid);

                String targetPlayerName = ((TextComponent) event.message()).content();
                String homeName = openedHome.get(uuid);

                // Отмена
                if (targetPlayerName.equalsIgnoreCase("отмена") || targetPlayerName.equalsIgnoreCase("cancel")) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("Приглашение отменено.")
                            .color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text(""));

                    // Открываем меню обратно
                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> inviteGUI.openInviteMenu(player, homeName));
                    return;
                }

                // Поиск игрока
                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("Игрок '" + targetPlayerName + "' не найден или не в сети!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text(""));

                    // Открываем меню обратно
                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> inviteGUI.openInviteMenu(player, homeName));
                    return;
                }

                // Проверка, что не приглашаешь себя
                if (targetPlayer.getUniqueId().equals(uuid)) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("Вы не можете пригласить себя!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text(""));

                    // Открываем меню обратно
                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> inviteGUI.openInviteMenu(player, homeName));
                    return;
                }

                // Проверка, не приглашён ли уже
                if (databaseManager.isInvited(uuid, homeName, targetPlayer.getUniqueId())) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("Игрок '" + targetPlayer.getName() + "' уже приглашён!")
                            .color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text(""));

                    // Открываем меню обратно
                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> inviteGUI.openInviteMenu(player, homeName));
                    return;
                }

                // Приглашение
                boolean success = databaseManager.invitePlayer(uuid, homeName, targetPlayer.getUniqueId());

                if (success) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("✓ Игрок '" + targetPlayer.getName() + "' приглашён в дом '" + homeName + "'!")
                            .color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text(""));

                    targetPlayer.sendMessage(Component.text(""));
                    targetPlayer.sendMessage(Component.text("✉ Игрок '" + player.getName() + "' пригласил вас в свой дом '" + homeName + "'!")
                            .color(NamedTextColor.GREEN));
                    targetPlayer.sendMessage(Component.text("Используйте /home visit " + player.getName() + " " + homeName)
                            .color(NamedTextColor.GRAY));
                    targetPlayer.sendMessage(Component.text(""));

                    // Открываем обновленное меню
                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> inviteGUI.openInviteMenu(player, homeName));
                } else {
                    player.sendMessage(Component.text("Ошибка приглашения игрока!")
                            .color(NamedTextColor.RED));
                }

                return;
            }
        }

        // Проверка на переименование
        if (pendingRename.containsKey(uuid)) {
            event.setCancelled(true);

            String oldName = pendingRename.remove(uuid);
            String newName = ((TextComponent) event.message()).content();

            // Отмена
            if (newName.equalsIgnoreCase("отмена") || newName.equalsIgnoreCase("cancel")) {
                player.sendMessage(Component.text("§eПереименование отменено."));
                return;
            }

            // Проверка длины
            if (newName.length() > 16) {
                PluginMessages.send(player, "name-too-long");
                return;
            }

            // Проверка символов
            if (!newName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
                PluginMessages.send(player, "invalid-name");
                return;
            }

            if (databaseManager.getHome(uuid, newName) != null) {

                PluginMessages.send(player, "home-exists", "{name}", newName);
                return;
            }

            boolean success = databaseManager.renameHome(uuid, oldName, newName);

            if (success) {
                PluginMessages.send(player, "home-renamed", "{old}", oldName, "{new}", newName);
            } else {
                player.sendMessage(Component.text("§cОшибка переименования дома!"));
            }

            return;
        }

        if (pendingCreate.containsKey(uuid)) {
            event.setCancelled(true);
            pendingCreate.remove(uuid);

            String homeName = ((TextComponent) event.message()).content();

            if (homeName.equalsIgnoreCase("отмена") || homeName.equalsIgnoreCase("cancel")) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text("Создание дома отменено.")
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text(""));
                return;
            }

            if (homeName.length() > 16) {
                PluginMessages.send(player, "name-too-long");
                return;
            }

            if (!homeName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
                PluginMessages.send(player, "invalid-name");
                return;
            }

            int homeCount = databaseManager.getHomeCount(uuid);
            int limit = LuckPermsHelper.getHighestLimit(player);

            if (limit != -1 && homeCount >= limit) {
                PluginMessages.send(player, "home-limit-reached", "{limit}", String.valueOf(limit));
                return;
            }

            if (databaseManager.getHome(uuid, homeName) != null) {
                boolean success = databaseManager.createHome(uuid, homeName, player.getLocation());

                if (success) {
                    player.sendMessage(Component.text(""));
                    PluginMessages.send(player, "home-updated", "{name}", homeName);
                    player.sendMessage(Component.text(""));

                    Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> homeGUI.openGUI(player, 0));
                } else {
                    player.sendMessage(Component.text("§cОшибка обновления дома!"));
                }
                return;
            }

            boolean success = databaseManager.createHome(uuid, homeName, player.getLocation());

            if (success) {
                player.sendMessage(Component.text(""));
                PluginMessages.send(player, "home-set", "{name}", homeName);
                player.sendMessage(Component.text(""));

                Bukkit.getScheduler().runTask(HomesPlugin.getInstance(), () -> homeGUI.openGUI(player, 0));
            } else {
                player.sendMessage(Component.text("§cОшибка создания дома!"));
            }
        }
    }
}
