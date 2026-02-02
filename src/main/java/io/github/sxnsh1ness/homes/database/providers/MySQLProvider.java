package io.github.sxnsh1ness.homes.database.providers;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.github.sxnsh1ness.homes.database.Home;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLProvider implements DatabaseProvider {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private MysqlDataSource dataSource;

    public MySQLProvider(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void initialize() {
        try {
            dataSource = new MysqlDataSource();

            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "minecraft");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "password");
            boolean useSSL = config.getBoolean("database.mysql.useSSL", false);
            boolean autoReconnect = config.getBoolean("database.mysql.autoReconnect", true);

            dataSource.setServerName(host);
            dataSource.setPort(port);
            dataSource.setDatabaseName(database);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            dataSource.setUseSSL(useSSL);
            dataSource.setAutoReconnect(autoReconnect);
            dataSource.setCharacterEncoding("utf8");

            try (Connection conn = dataSource.getConnection()) {
                plugin.getLogger().info("MySQL подключение успешно установлено!");
            }

            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инициализации MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS homes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(16) NOT NULL," +
                "world VARCHAR(255) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "UNIQUE KEY unique_home (player_uuid, name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        String invitesSql = "CREATE TABLE IF NOT EXISTS home_invites (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "home_name VARCHAR(16) NOT NULL," +
                "invited_uuid VARCHAR(36) NOT NULL," +
                "UNIQUE KEY unique_invite (owner_uuid, home_name, invited_uuid)," +
                "INDEX idx_invited (invited_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(invitesSql);
            plugin.getLogger().info("MySQL таблицы успешно созданы!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка создания таблиц MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean createHome(UUID playerUUID, String name, Location location) {
        String sql = "INSERT INTO homes (player_uuid, name, world, x, y, z, yaw, pitch) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE world=?, x=?, y=?, z=?, yaw=?, pitch=?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, name.toLowerCase());
            pstmt.setString(3, location.getWorld().getName());
            pstmt.setDouble(4, location.getX());
            pstmt.setDouble(5, location.getY());
            pstmt.setDouble(6, location.getZ());
            pstmt.setFloat(7, location.getYaw());
            pstmt.setFloat(8, location.getPitch());
            // Дубликаты для UPDATE
            pstmt.setString(9, location.getWorld().getName());
            pstmt.setDouble(10, location.getX());
            pstmt.setDouble(11, location.getY());
            pstmt.setDouble(12, location.getZ());
            pstmt.setFloat(13, location.getYaw());
            pstmt.setFloat(14, location.getPitch());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка создания дома: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteHome(UUID playerUUID, String name) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, name.toLowerCase());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка удаления дома: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean renameHome(UUID playerUUID, String oldName, String newName) {
        String sql = "UPDATE homes SET name = ? WHERE player_uuid = ? AND name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName.toLowerCase());
            pstmt.setString(2, playerUUID.toString());
            pstmt.setString(3, oldName.toLowerCase());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка переименования дома: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Home getHome(UUID playerUUID, String name) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        plugin.getLogger().info("MySQL соединение закрыто!");
    }
}
