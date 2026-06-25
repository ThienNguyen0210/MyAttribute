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

                // Bảng lưu permanent stats (set thẳng giá trị, cộng/trừ qua lệnh /attr permstats)
                // Hoàn toàn tách biệt với player_attributes (level) và tempBonus (RAM)
                statement.execute("CREATE TABLE IF NOT EXISTS player_perm_stats (" +
                        "uuid VARCHAR(36) NOT NULL," +
                        "stat_name TEXT NOT NULL," +
                        "value REAL DEFAULT 0," +
                        "PRIMARY KEY (uuid, stat_name))");

                // Bảng lưu permanent percent stats (%, cộng/trừ qua /attr permstats <player> <stat> <value%>)
                statement.execute("CREATE TABLE IF NOT EXISTS player_perm_percent_stats (" +
                        "uuid VARCHAR(36) NOT NULL," +
                        "stat_name TEXT NOT NULL," +
                        "value REAL DEFAULT 0," +
                        "PRIMARY KEY (uuid, stat_name))");
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

    // -------------------------------------------------------
    //  PERMANENT STATS (set thẳng giá trị, cộng/trừ qua lệnh,
    //  lưu DB vĩnh viễn, áp dụng khi join/respawn)
    // -------------------------------------------------------

    /**
     * Lấy giá trị permanent stat hiện tại của 1 player cho 1 stat.
     * Trả về 0 nếu chưa từng có.
     */
    public double getPermStat(UUID uuid, String statName) {
        String sql = "SELECT value FROM player_perm_stats WHERE uuid = ? AND stat_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, statName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("value");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi getPermStat (" + uuid + "): " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Cộng dồn (hoặc trừ nếu amount âm) vào permanent stat của player.
     * Dùng cho lệnh /attr permstats <player> <stat> <value>.
     */
    public void addPermStat(UUID uuid, String statName, double amount) {
        double current = getPermStat(uuid, statName);
        double newValue = current + amount;

        String sql = "INSERT INTO player_perm_stats(uuid, stat_name, value) VALUES(?, ?, ?) " +
                "ON CONFLICT(uuid, stat_name) DO UPDATE SET value = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statName);
            ps.setDouble(3, newValue);
            ps.setDouble(4, newValue);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi addPermStat: " + e.getMessage());
        }
    }

    /**
     * Xoá permanent stat của 1 player cho 1 stat cụ thể.
     * Dùng cho lệnh /attr clearperm <player> <stat>.
     */
    public void clearPermStat(UUID uuid, String statName) {
        String sql = "DELETE FROM player_perm_stats WHERE uuid = ? AND stat_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi clearPermStat: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ permanent stats của 1 player (dùng để load vào RAM lúc join/respawn).
     */
    public java.util.Map<String, Double> getAllPermStats(UUID uuid) {
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        String sql = "SELECT stat_name, value FROM player_perm_stats WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("stat_name"), rs.getDouble("value"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi getAllPermStats (" + uuid + "): " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------
    //  PERMANENT PERCENT STATS (%, lưu DB vĩnh viễn)
    // -------------------------------------------------------

    public double getPermPercentStat(UUID uuid, String statName) {
        String sql = "SELECT value FROM player_perm_percent_stats WHERE uuid = ? AND stat_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("value");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Loi getPermPercentStat (" + uuid + "): " + e.getMessage());
        }
        return 0.0;
    }

    public void addPermPercentStat(UUID uuid, String statName, double amount) {
        double current = getPermPercentStat(uuid, statName);
        double newValue = current + amount;

        String sql = "INSERT INTO player_perm_percent_stats(uuid, stat_name, value) VALUES(?, ?, ?) " +
                "ON CONFLICT(uuid, stat_name) DO UPDATE SET value = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statName);
            ps.setDouble(3, newValue);
            ps.setDouble(4, newValue);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Loi addPermPercentStat: " + e.getMessage());
        }
    }

    public void clearPermPercentStat(UUID uuid, String statName) {
        String sql = "DELETE FROM player_perm_percent_stats WHERE uuid = ? AND stat_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Loi clearPermPercentStat: " + e.getMessage());
        }
    }

    public java.util.Map<String, Double> getAllPermPercentStats(UUID uuid) {
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        String sql = "SELECT stat_name, value FROM player_perm_percent_stats WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("stat_name"), rs.getDouble("value"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Loi getAllPermPercentStats (" + uuid + "): " + e.getMessage());
        }
        return result;
    }
}