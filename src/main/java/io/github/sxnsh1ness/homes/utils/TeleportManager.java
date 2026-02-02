package io.github.sxnsh1ness.homes.utils;

import io.github.sxnsh1ness.homes.HomesPlugin;
import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.config.PluginMessages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    private final CooldownManager cooldownManager;
    private final Map<UUID, TeleportTask> activeTeleports;

    public TeleportManager(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
        this.activeTeleports = new HashMap<>();
    }

    public void teleportWithDelay(Player player, Location destination, String homeName) {
        // Отменяем предыдущую телепортацию если она есть
        cancelTeleport(player);

        int delay = ConfigManager.getConfig().getInt("teleport.delay", 5);

        // Если задержка 0 или меньше, телепортируем сразу
        if (delay <= 0) {
            teleportImmediately(player, destination, homeName);
            return;
        }

        // Сохраняем начальную позицию
        Location startLocation = player.getLocation().clone();

        PluginMessages.send(player, "teleport-started");

        TeleportTask task = new TeleportTask(player, startLocation, destination, homeName, delay);
        activeTeleports.put(player.getUniqueId(), task);
        task.start();
    }

    private void teleportImmediately(Player player, Location destination, String homeName) {
        player.teleport(destination);

        PluginMessages.send(player, "home-teleported", "{name}", homeName);

        // Устанавливаем кулдаун если нужно
        boolean applyOnCommand = ConfigManager.getConfig().getBoolean("cooldown.apply-on-command", true);
        if (!applyOnCommand) {
            cooldownManager.setCooldown(player);
        }
    }

    public void cancelTeleport(Player player) {
        TeleportTask task = activeTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public boolean hasPendingTeleport(Player player) {
        return activeTeleports.containsKey(player.getUniqueId());
    }

    private class TeleportTask extends BukkitRunnable {
        private final Player player;
        private final Location startLocation;
        private final Location destination;
        private final String homeName;
        private final int totalDelay;
        private int ticksLeft;
        private BukkitTask particleTask;
        private BossBar bossBar;

        public TeleportTask(Player player, Location startLocation, Location destination, String homeName, int delaySeconds) {
            this.player = player;
            this.startLocation = startLocation;
            this.destination = destination;
            this.homeName = homeName;
            this.totalDelay = delaySeconds;
            this.ticksLeft = delaySeconds * 20;
        }

        public void start() {
            createBossBar();

            this.runTaskTimer(HomesPlugin.getInstance(), 0L, 1L);

            if (ConfigManager.getConfig().getBoolean("teleport.show-flame-effects", true)) {
                particleTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline() || !activeTeleports.containsKey(player.getUniqueId())) {
                            this.cancel();
                            return;
                        }

                        Location loc = player.getLocation();
                        for (int i = 0; i < 3; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            double radius = 1.0;
                            double x = loc.getX() + radius * Math.cos(angle);
                            double z = loc.getZ() + radius * Math.sin(angle);
                            double y = loc.getY() + Math.random() * 2;

                            Location particleLoc = new Location(loc.getWorld(), x, y, z);
                            player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }.runTaskTimer(HomesPlugin.getInstance(), 0L, 2L);
            }
        }

        private void createBossBar() {
            String titleTemplate = ConfigManager.getConfig().getString("teleport.bossbar.title", "&6&lТелепортация через {time}с");
            String colorName = ConfigManager.getConfig().getString("teleport.bossbar.color", "YELLOW");
            String styleName = ConfigManager.getConfig().getString("teleport.bossbar.style", "PROGRESS");

            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.YELLOW;
            }

            BossBar.Overlay overlay;
            try {
                overlay = BossBar.Overlay.valueOf(styleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                overlay = BossBar.Overlay.PROGRESS;
            }

            String title = titleTemplate.replace("{time}", String.valueOf(totalDelay)).replace("&", "§");
            bossBar = BossBar.bossBar(
                    Component.text(title),
                    1.0f,
                    color,
                    overlay
            );

            player.showBossBar(bossBar);
        }

        private void updateBossBar(int secondsLeft) {
            if (bossBar == null) return;

            String titleTemplate = ConfigManager.getConfig().getString("teleport.bossbar.title", "&6&lТелепортация через {time}с");
            String title = titleTemplate.replace("{time}", String.valueOf(secondsLeft)).replace("&", "§");

            bossBar.name(Component.text(title));
            bossBar.progress((float) ticksLeft / (totalDelay * 20));
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cleanup();
                return;
            }

            // Проверяем, не сдвинулся ли игрок
            boolean cancelOnMove = ConfigManager.getConfig().getBoolean("teleport.cancel-on-move", true);
            if (cancelOnMove && hasMoved()) {
                PluginMessages.send(player, "teleport-cancelled");
                cleanup();
                return;
            }

            ticksLeft--;

            if (ticksLeft % 20 == 0) {
                int secondsLeft = ticksLeft / 20;
                updateBossBar(secondsLeft);
            }

            if (ticksLeft <= 0) {
                player.teleport(destination);

                PluginMessages.send(player, "home-teleported", "{name}", homeName);

                player.getWorld().spawnParticle(Particle.PORTAL, destination, 50, 0.5, 1, 0.5, 0.1);

                boolean applyOnCommand = ConfigManager.getConfig().getBoolean("cooldown.apply-on-command", true);
                if (!applyOnCommand) {
                    cooldownManager.setCooldown(player);
                }

                cleanup();
            }
        }

        private boolean hasMoved() {
            Location current = player.getLocation();
            return current.getX() != startLocation.getX() ||
                    current.getY() != startLocation.getY() ||
                    current.getZ() != startLocation.getZ();
        }

        private void cleanup() {
            this.cancel();
            if (particleTask != null) {
                particleTask.cancel();
            }
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }
            activeTeleports.remove(player.getUniqueId());
        }
    }
}
