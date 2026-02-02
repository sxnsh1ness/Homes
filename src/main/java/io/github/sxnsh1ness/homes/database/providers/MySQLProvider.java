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

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
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
    public void close() {
        plugin.getLogger().info("MySQL соединение закрыто!");
    }
}
