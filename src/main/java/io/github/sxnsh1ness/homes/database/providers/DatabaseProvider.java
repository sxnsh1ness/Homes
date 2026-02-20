package io.github.sxnsh1ness.homes.database.providers;

import io.github.sxnsh1ness.homes.models.Home;
import org.bukkit.Location;

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
    boolean invitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID);
    boolean uninvitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID);
    boolean isInvited(UUID ownerUUID, String homeName, UUID invitedUUID);
    List<UUID> getInvitedPlayers(UUID ownerUUID, String homeName);
    List<Home> getInvitedHomes(UUID invitedUUID);
    void close();
}
