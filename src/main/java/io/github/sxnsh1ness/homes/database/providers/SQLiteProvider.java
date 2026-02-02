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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
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
