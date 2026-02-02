package io.github.sxnsh1ness.homes.listeners;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.gui.HomeGUI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final HomeGUI homeGUI;

    public ChatListener(DatabaseManager databaseManager, ConfigManager configManager, HomeGUI homeGUI) {
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.homeGUI = homeGUI;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<UUID, String> pendingRename = homeGUI.getPendingRename();

        if (!pendingRename.containsKey(uuid)) return;

        event.setCancelled(true);

        String oldName = pendingRename.remove(uuid);
        String newName = ((TextComponent) event.message()).content();

        if (newName.equalsIgnoreCase("отмена") || newName.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("§eПереименование отменено."));
            return;
        }

        if (newName.length() > 16) {
            String message = configManager.getMessage("name-too-long");
            player.sendMessage(Component.text(message));
            return;
        }

        if (!newName.matches("[a-zA-Zа-яА-Я0-9_-]+")) {
            String message = configManager.getMessage("invalid-name");
            player.sendMessage(Component.text(message));
            return;
        }

        // Проверка существования
        if (databaseManager.getHome(uuid, newName) != null) {
            String message = configManager.getMessage("home-exists",
                    Map.of("name", newName));
            player.sendMessage(Component.text(message));
            return;
        }

        boolean success = databaseManager.renameHome(player.getUniqueId(), oldName, newName);

        if (success) {
            String message = configManager.getMessage("home-renamed",
                    Map.of("old", oldName, "new", newName));
            player.sendMessage(Component.text(message));
        } else {
            player.sendMessage(Component.text("§cОшибка переименования дома!"));
        }
    }
}
