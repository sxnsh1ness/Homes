package io.github.sxnsh1ness.homes.gui;

import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.database.Home;
import io.github.sxnsh1ness.homes.utils.CooldownManager;
import io.github.sxnsh1ness.homes.utils.LuckPermsHelper;
import io.github.sxnsh1ness.homes.utils.TeleportManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class HomeGUI implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;
    private final CooldownManager cooldownManager;

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    @Getter
    private final Map<UUID, String> pendingRename = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 28; // 4 ряда по 7 предметов

    public HomeGUI(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager, TeleportManager teleportManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.teleportManager = teleportManager;
        this.cooldownManager = cooldownManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player, int page) {
        List<Home> homes = databaseManager.getHomes(player.getUniqueId());
        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);

        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text("Мои дома (Страница " + (page + 1) + "/" + totalPages + ")")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD));

        // Добавляем дома на текущую странице
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, homes.size());

        for (int i = startIndex; i < endIndex; i++) {
            Home home = homes.get(i);
            int slot = i - startIndex;
            gui.setItem(slot, createHomeItem(home));
        }

        // Добавляем кнопку создания дома
        int playerLimit = LuckPermsHelper.getHighestLimit(player, configManager);
        String limitDisplay = playerLimit == -1 ? "∞" : String.valueOf(playerLimit);

        if (playerLimit == -1 || homes.size() < playerLimit) {
            ItemStack createButton = new ItemStack(Material.EMERALD);
            ItemMeta meta = createButton.getItemMeta();
            meta.displayName(Component.text("+ Создать новый дом")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(
                    Component.text(""),
                    Component.text("Ваши дома: " + homes.size() + "/" + limitDisplay)
                            .color(NamedTextColor.GRAY),
                    Component.text(""),
                    Component.text("Закройте это меню и используйте:")
                            .color(NamedTextColor.YELLOW),
                    Component.text("/sethome <название>")
                            .color(NamedTextColor.GOLD)
            ));
            createButton.setItemMeta(meta);
            gui.setItem(49, createButton);
        } else {
            ItemStack limitReached = new ItemStack(Material.BARRIER);
            ItemMeta meta = limitReached.getItemMeta();
            meta.displayName(Component.text("Лимит достигнут")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(
                    Component.text(""),
                    Component.text("Вы достигли максимума: " + limitDisplay)
                            .color(NamedTextColor.GRAY),
                    Component.text("Удалите дом, чтобы создать новый")
                            .color(NamedTextColor.YELLOW)
            ));
            limitReached.setItemMeta(meta);
            gui.setItem(49, limitReached);
        }

        // Навигация
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            meta.displayName(Component.text("← Предыдущая страница")
                    .color(NamedTextColor.YELLOW));
            prevPage.setItemMeta(meta);
            gui.setItem(45, prevPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            meta.displayName(Component.text("Следующая страница →")
                    .color(NamedTextColor.YELLOW));
            nextPage.setItemMeta(meta);
            gui.setItem(53, nextPage);
        }

        // Кнопка закрытия
        ItemStack close = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Закрыть")
                .color(NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        gui.setItem(50, close);

        player.openInventory(gui);
    }

    private ItemStack createHomeItem(Home home) {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(home.getName())
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("Мир: " + home.getWorldName())
                .color(NamedTextColor.GRAY));
        lore.add(Component.text(String.format("Координаты: %.0f, %.0f, %.0f",
                        home.getX(), home.getY(), home.getZ()))
                .color(NamedTextColor.GRAY));
        lore.add(Component.text(""));
        lore.add(Component.text("ЛКМ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Телепортироваться")
                        .color(NamedTextColor.GRAY)));
        lore.add(Component.text("ПКМ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Переименовать")
                        .color(NamedTextColor.GRAY)));
        lore.add(Component.text("SHIFT + ПКМ")
                .color(NamedTextColor.RED)
                .append(Component.text(" - Удалить")
                        .color(NamedTextColor.GRAY)));

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().title().toString();
        if (!title.contains("Мои дома")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        // Навигация
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            openGUI(player, currentPage - 1);
            return;
        }

        if (slot == 53 && clicked.getType() == Material.ARROW) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            openGUI(player, currentPage + 1);
            return;
        }

        // Закрыть
        if (slot == 50 && clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.closeInventory();
            return;
        }

        // Создать дом
        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            player.closeInventory();
            player.sendMessage(Component.text("Используйте команду: /sethome <название>")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Взаимодействие с домом
        if (clicked.getType() == Material.RED_BED) {
            Component displayName = clicked.getItemMeta().displayName();
            if (displayName == null) return;

            String homeName = ((net.kyori.adventure.text.TextComponent) displayName).content();
            Home home = databaseManager.getHome(player.getUniqueId(), homeName);

            if (home == null) {
                player.sendMessage(Component.text("Дом не найден!")
                        .color(NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            // ЛКМ - Телепортация
            if (event.isLeftClick() && !event.isShiftClick()) {
                player.closeInventory();

                // Проверка кулдауна
                if (!cooldownManager.hasCooldown(player)) {
                    int remainingTime = cooldownManager.getRemainingCooldown(player);
                    String message = configManager.getMessage("command-on-cooldown",
                            Map.of("time", String.valueOf(remainingTime)));
                    player.sendMessage(Component.text(message));
                    return;
                }

                // Проверка мира
                if (home.getLocation().getWorld() == null) {
                    String message = configManager.getMessage("world-not-found",
                            Map.of("world", home.getWorldName()));
                    player.sendMessage(Component.text(message));
                    return;
                }

                // Устанавливаем кулдаун если нужно
                boolean applyOnCommand = configManager.getConfig().getBoolean("cooldown.apply-on-command", true);
                if (applyOnCommand) {
                    cooldownManager.setCooldown(player);
                }

                teleportManager.teleportWithDelay(player, home.getLocation(), homeName);
            }
            // ПКМ - Переименовать
            else if (event.isRightClick() && !event.isShiftClick()) {
                player.closeInventory();
                pendingRename.put(player.getUniqueId(), homeName);
                player.sendMessage(Component.text("Введите новое название дома в чат:")
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("(или напишите 'отмена' для отмены)")
                        .color(NamedTextColor.GRAY));
            }
            // SHIFT + ПКМ - Удалить
            else if (event.isRightClick() && event.isShiftClick()) {
                boolean success = databaseManager.deleteHome(player.getUniqueId(), homeName);

                if (success) {
                    String message = configManager.getMessage("home-deleted",
                            Map.of("name", homeName));
                    player.sendMessage(Component.text(message));
                    openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0));
                } else {
                    player.sendMessage(Component.text("Ошибка удаления дома!")
                            .color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().title().toString();
        if (!title.contains("Мои дома")) return;

        playerPages.remove(player.getUniqueId());
    }

}
