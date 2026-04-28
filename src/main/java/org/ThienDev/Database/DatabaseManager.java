package org.ThienDev.Database;

import org.ThienDev.Main;
import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final Main plugin;
    private Connection connection;
    private final File dbFile;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "Data.sqlite");
        setupDatabase();
    }

    /**
     * Lấy kết nối hiện tại, nếu bị đóng thì tự động kết nối lại.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("Không tìm thấy Driver SQLite!");
            }
        }
        return connection;
    }

    private void setupDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            // Tạo bảng nếu chưa có
            try (Connection conn = getConnection();
                 Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS player_attributes (" +
                        "uuid VARCHAR(36) NOT NULL," +
                        "attribute_id TEXT NOT NULL," +
                        "level INTEGER DEFAULT 0," +
                        "PRIMARY KEY (uuid, attribute_id))");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi setup database: " + e.getMessage());
        }
    }

    /**
     * Lấy cấp độ của một ID.
     * LƯU Ý: Nếu dùng trong Event gây dame, nên dùng Cache thay vì gọi hàm này.
     */
    public int getAttributeLevel(UUID uuid, String attrId) {
        String sql = "SELECT level FROM player_attributes WHERE uuid = ? AND attribute_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, attrId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("level");
            }
        } catch (SQLException e) {
            // Không dùng e.printStackTrace() để tránh spam log khi lỗi
            plugin.getLogger().warning("Lỗi getAttributeLevel (" + uuid + "): " + e.getMessage());
        }
        return 0;
    }

    public void addAttributeLevel(UUID uuid, String attrId, int amount) {
        int currentLevel = getAttributeLevel(uuid, attrId);
        int newLevel = currentLevel + amount;

        String sql = "INSERT INTO player_attributes(uuid, attribute_id, level) VALUES(?, ?, ?) " +
                "ON CONFLICT(uuid, attribute_id) DO UPDATE SET level = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, attrId);
            ps.setInt(3, newLevel);
            ps.setInt(4, newLevel);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi addAttributeLevel: " + e.getMessage());
        }
    }

    public void resetAttributes(UUID uuid) {
        String sql = "DELETE FROM player_attributes WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi resetAttributes: " + e.getMessage());
        }
    }
    public int getTotalLevels(UUID uuid) {
        String sql = "SELECT SUM(level) as total FROM player_attributes WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}