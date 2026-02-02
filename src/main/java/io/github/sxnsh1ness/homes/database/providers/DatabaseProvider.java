package io.github.sxnsh1ness.homes.database.providers;

import io.github.sxnsh1ness.homes.database.Home;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface DatabaseProvider {
    void initialize();
    void createTables();
    boolean createHome(UUID playerUUID, String name, Location location);
    boolean deleteHome(UUID playerUUID, String name);
    boolean renameHome(UUID playerUUID, String oldName, String newName);
    Home getHome(UUID playerUUID, String name);
    List<Home> getHomes(UUID playerUUID);
    int getHomeCount(UUID playerUUID);
    void close();
}
