package io.github.sxnsh1ness.homes.database;

import io.github.sxnsh1ness.homes.database.providers.DatabaseProvider;
import io.github.sxnsh1ness.homes.database.providers.MySQLProvider;
import io.github.sxnsh1ness.homes.database.providers.SQLiteProvider;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private DatabaseProvider provider;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        String databaseType = config.getString("database.type", "SQLITE").toUpperCase();

        switch (databaseType) {
            case "MYSQL":
                plugin.getLogger().info("Инициализация MySQL базы данных...");
                provider = new MySQLProvider(plugin, config);
                break;
            case "SQLITE":
            default:
                plugin.getLogger().info("Инициализация SQLite базы данных...");
                provider = new SQLiteProvider(plugin);
                break;
        }

        provider.initialize();
    }

    public boolean createHome(UUID playerUUID, String name, Location location) {
        return provider.createHome(playerUUID, name, location);
    }

    public boolean deleteHome(UUID playerUUID, String name) {
        return provider.deleteHome(playerUUID, name);
    }

    public boolean renameHome(UUID playerUUID, String oldName, String newName) {
        return provider.renameHome(playerUUID, oldName, newName);
    }

    public Home getHome(UUID playerUUID, String name) {
        return provider.getHome(playerUUID, name);
    }

    public List<Home> getHomes(UUID playerUUID) {
        return provider.getHomes(playerUUID);
    }

    public int getHomeCount(UUID playerUUID) {
        return provider.getHomeCount(playerUUID);
    }

    public boolean invitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID) {
        return provider.invitePlayer(ownerUUID, homeName, invitedUUID);
    }

    public boolean uninvitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID) {
        return provider.uninvitePlayer(ownerUUID, homeName, invitedUUID);
    }

    public boolean isInvited(UUID ownerUUID, String homeName, UUID invitedUUID) {
        return provider.isInvited(ownerUUID, homeName, invitedUUID);
    }

    public List<UUID> getInvitedPlayers(UUID ownerUUID, String homeName) {
        return provider.getInvitedPlayers(ownerUUID, homeName);
    }

    public List<Home> getInvitedHomes(UUID invitedUUID) {
        return provider.getInvitedHomes(invitedUUID);
    }

    public void close() {
        if (provider != null) {
            provider.close();
        }
    }
}
