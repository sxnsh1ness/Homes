package io.github.sxnsh1ness.homes.gui;

import io.github.sxnsh1ness.homes.HomesPlugin;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class InviteGUI implements Listener {

    private final DatabaseManager databaseManager;
    private final HomeGUI homeGUI;

    @Getter
    private final Map<UUID, String> openedHome = new HashMap<>();
    @Getter
    private final Map<UUID, Boolean> pendingInvite = new HashMap<>();

    public InviteGUI(DatabaseManager databaseManager, HomeGUI homeGUI) {
        this.databaseManager = databaseManager;
        this.homeGUI = homeGUI;
        HomesPlugin.getInstance().getServer().getPluginManager().registerEvents(this, HomesPlugin.getInstance());
    }

    public void openInviteMenu(Player player, String homeName) {
        openedHome.put(player.getUniqueId(), homeName);

        List<UUID> invitedPlayers = databaseManager.getInvitedPlayers(player.getUniqueId(), homeName);

        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text("Приглашения: " + homeName)
                        .color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD));

        int slot = 0;
        for (UUID invitedUUID : invitedPlayers) {
            if (slot >= 45) break;

            OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(invitedUUID);
            gui.setItem(slot, createPlayerHead(invitedPlayer));
            slot++;
        }

        ItemStack decoration = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decoration.getItemMeta();
        decorMeta.displayName(Component.text(""));
        decoration.setItemMeta(decorMeta);

        for (int i = 45; i < 54; i++) {
            if (i != 47 && i != 49 && i != 51) {
                gui.setItem(i, decoration);
            }
        }

        ItemStack addButton = new ItemStack(Material.LIME_DYE);
        ItemMeta addMeta = addButton.getItemMeta();
        addMeta.displayName(Component.text("+ Пригласить игрока")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));
        addMeta.lore(Arrays.asList(
                Component.text(""),
                Component.text("Нажмите, чтобы пригласить")
                        .color(NamedTextColor.GRAY),
                Component.text("Введите имя в чат")
                        .color(NamedTextColor.YELLOW)
        ));
        addButton.setItemMeta(addMeta);
        gui.setItem(47, addButton);

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("← Назад")
                .color(NamedTextColor.YELLOW));
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.displayName(Component.text("Закрыть")
                .color(NamedTextColor.RED));
        closeButton.setItemMeta(closeMeta);
        gui.setItem(51, closeButton);

        player.openInventory(gui);
    }

    private ItemStack createPlayerHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName() != null ? player.getName() : "Unknown")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("Статус: " + (player.isOnline() ? "В сети" : "Не в сети"))
                .color(player.isOnline() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        lore.add(Component.text(""));
        lore.add(Component.text("ПКМ")
                .color(NamedTextColor.RED)
                .append(Component.text(" - Убрать приглашение")
                        .color(NamedTextColor.GRAY)));

        meta.lore(lore);
        head.setItemMeta(meta);

        return head;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().title().toString();
        if (!title.contains("Приглашения:")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String homeName = openedHome.get(player.getUniqueId());
        if (homeName == null) return;

        // Кнопка добавления
        if (clicked.getType() == Material.LIME_DYE) {
            pendingInvite.put(player.getUniqueId(), true);
            player.closeInventory();
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("═══════════════════════════════════")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("  ✉ Приглашение в дом '" + homeName + "'")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("═══════════════════════════════════")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("Введите имя игрока для приглашения:")
                    .color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("(или напишите 'отмена' для отмены)")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            openedHome.remove(player.getUniqueId());
            homeGUI.openGUI(player, 0);
            return;
        }

        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            openedHome.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && event.isRightClick()) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            OfflinePlayer targetPlayer = meta.getOwningPlayer();

            if (targetPlayer != null) {
                boolean success = databaseManager.uninvitePlayer(player.getUniqueId(), homeName, targetPlayer.getUniqueId());

                if (success) {
                    player.sendMessage(Component.text("Приглашение игрока '" + targetPlayer.getName() + "' удалено!", NamedTextColor.GREEN));

                    if (targetPlayer.isOnline()) {
                        ((Player) targetPlayer).sendMessage(Component.text("Ваше приглашение в дом '" + homeName + "' было удалено!", NamedTextColor.YELLOW));
                    }
                    openInviteMenu(player, homeName);
                } else {
                    player.sendMessage(Component.text("Ошибка удаления приглашения!", NamedTextColor.RED));
                }
            }
        }
    }
}
