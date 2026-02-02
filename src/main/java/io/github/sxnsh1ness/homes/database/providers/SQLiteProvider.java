package io.github.sxnsh1ness.homes.database.providers;

import io.github.sxnsh1ness.homes.database.Home;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLiteProvider implements DatabaseProvider {

    private final JavaPlugin plugin;
    private Connection connection;

    public SQLiteProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + new File(dataFolder, "homes.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createTables();
            plugin.getLogger().info("SQLite база данных успешно инициализирована!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инициализации SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS homes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL," +
                "pitch REAL NOT NULL," +
                "UNIQUE(player_uuid, name)" +
                ")";
        String invitesSql = "CREATE TABLE IF NOT EXISTS home_invites (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "owner_uuid TEXT NOT NULL," +
                "home_name TEXT NOT NULL," +
                "invited_uuid TEXT NOT NULL," +
                "UNIQUE(owner_uuid, home_name, invited_uuid)," +
                "FOREIGN KEY(owner_uuid, home_name) REFERENCES homes(player_uuid, name) ON DELETE CASCADE" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(invitesSql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка создания таблиц SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean createHome(UUID playerUUID, String name, Location location) {
        String sql = "INSERT INTO homes (player_uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, name.toLowerCase());
            pstmt.setString(3, location.getWorld().getName());
            pstmt.setDouble(4, location.getX());
            pstmt.setDouble(5, location.getY());
            pstmt.setDouble(6, location.getZ());
            pstmt.setFloat(7, location.getYaw());
            pstmt.setFloat(8, location.getPitch());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean deleteHome(UUID playerUUID, String name) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, name.toLowerCase());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean renameHome(UUID playerUUID, String oldName, String newName) {
        String sql = "UPDATE homes SET name = ? WHERE player_uuid = ? AND name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newName.toLowerCase());
            pstmt.setString(2, playerUUID.toString());
            pstmt.setString(3, oldName.toLowerCase());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Home getHome(UUID playerUUID, String name) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, name.toLowerCase());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Home(
                        playerUUID,
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения дома: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Home> getHomes(UUID playerUUID) {
        List<Home> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE player_uuid = ? ORDER BY name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                homes.add(new Home(
                        playerUUID,
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения списка домов: " + e.getMessage());
        }
        return homes;
    }

    @Override
    public int getHomeCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка подсчета домов: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean invitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID) {
        String sql = "INSERT INTO home_invites (owner_uuid, home_name, invited_uuid) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, homeName.toLowerCase());
            pstmt.setString(3, invitedUUID.toString());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean uninvitePlayer(UUID ownerUUID, String homeName, UUID invitedUUID) {
        String sql = "DELETE FROM home_invites WHERE owner_uuid = ? AND home_name = ? AND invited_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, homeName.toLowerCase());
            pstmt.setString(3, invitedUUID.toString());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean isInvited(UUID ownerUUID, String homeName, UUID invitedUUID) {
        String sql = "SELECT COUNT(*) FROM home_invites WHERE owner_uuid = ? AND home_name = ? AND invited_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, homeName.toLowerCase());
            pstmt.setString(3, invitedUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка проверки приглашения: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<UUID> getInvitedPlayers(UUID ownerUUID, String homeName) {
        List<UUID> invitedPlayers = new ArrayList<>();
        String sql = "SELECT invited_uuid FROM home_invites WHERE owner_uuid = ? AND home_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setString(2, homeName.toLowerCase());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                invitedPlayers.add(UUID.fromString(rs.getString("invited_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения списка приглашённых: " + e.getMessage());
        }
        return invitedPlayers;
    }

    @Override
    public List<Home> getInvitedHomes(UUID invitedUUID) {
        List<Home> homes = new ArrayList<>();
        String sql = "SELECT h.* FROM homes h " +
                "INNER JOIN home_invites hi ON h.player_uuid = hi.owner_uuid AND h.name = hi.home_name " +
                "WHERE hi.invited_uuid = ? ORDER BY h.name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, invitedUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                homes.add(new Home(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения домов с приглашениями: " + e.getMessage());
        }
        return homes;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite соединение закрыто!");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка закрытия SQLite: " + e.getMessage());
        }
    }
}
