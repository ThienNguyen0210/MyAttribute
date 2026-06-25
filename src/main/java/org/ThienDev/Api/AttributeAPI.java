package org.ThienDev.Api;

import org.ThienDev.Main;
import org.ThienDev.Utils.AttributeHelper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection; // Đây là dòng đúng
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeAPI {
    private static final Map<UUID, Map<String, Double>> tempPercentBonus = new ConcurrentHashMap<>();

    // Cache bonus tính từ attribute pack trong DB
    private static final Map<UUID, Map<String, Double>> bonusCache = new ConcurrentHashMap<>();

    // Temp bonus lưu trong RAM: uuid -> statName -> value
    // Hoàn toàn tách biệt với DB, dùng cho /attr stats tạm thời
    private static final Map<UUID, Map<String, Double>> tempBonus = new ConcurrentHashMap<>();

    // Permanent stats: load từ DB (player_perm_stats) vào RAM khi join/respawn.
    // Cộng/trừ qua lệnh /attr permstats, lưu DB vĩnh viễn.
    private static final Map<UUID, Map<String, Double>> permBonus = new ConcurrentHashMap<>();

    // Permanent percent stats: load từ DB (player_perm_percent_stats) vào RAM khi join/respawn.
    // Cộng/trừ qua lệnh /attr permstats <player> <stat> <value%>, lưu DB vĩnh viễn.
    private static final Map<UUID, Map<String, Double>> permPercentBonus = new ConcurrentHashMap<>();

    // -------------------------------------------------------
    //  PUBLIC API
    // -------------------------------------------------------

    /**
     * Lấy tổng bonus = bonus từ attribute pack (DB) + temp bonus (RAM).
     * Đây là hàm bạn gọi trong damage event, skill event, v.v.
     */
    public static double getPercentBonus(UUID uuid, String statName) {
        double fromTemp = tempPercentBonus
                .getOrDefault(uuid, Map.of())
                .getOrDefault(statName, 0.0);
        double fromPerm = permPercentBonus
                .getOrDefault(uuid, Map.of())
                .getOrDefault(statName, 0.0);
        return fromTemp + fromPerm;
    }
    public static void addTempPercentBonus(UUID uuid, String statName, double percent) {
        tempPercentBonus
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(statName, percent, Double::sum);
    }

    public static void removeTempPercentBonus(UUID uuid, String statName, double percent) {
        Map<String, Double> stats = tempPercentBonus.get(uuid);
        if (stats == null) return;
        stats.merge(statName, -percent, Double::sum);
        stats.computeIfPresent(statName, (k, v) -> v <= 0.0 ? null : v);
        if (stats.isEmpty()) tempPercentBonus.remove(uuid);
    }

    public static void clearTempPercentBonus(UUID uuid) {
        tempPercentBonus.remove(uuid);
    }
    public static double getBonus(UUID uuid, String statName) {
        double fromPack = bonusCache
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(statName, k -> calculateBonus(uuid, statName));

        double fromTemp = tempBonus
                .getOrDefault(uuid, Map.of())
                .getOrDefault(statName, 0.0);

        double fromPerm = permBonus
                .getOrDefault(uuid, Map.of())
                .getOrDefault(statName, 0.0);

        return fromPack + fromTemp + fromPerm;
    }

    // -------------------------------------------------------
    //  TEMP BONUS (RAM only - dành cho /attr stats)
    // -------------------------------------------------------

    /**
     * Cộng temp bonus vào RAM cho player.
     * Không đụng DB, không đụng bonusCache.
     */
    public static void addTempBonus(UUID uuid, String statName, double value) {
        tempBonus
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(statName, value, Double::sum);
    }

    /**
     * Xoá temp bonus đã thêm trước đó (gọi khi buff hết hạn).
     */
    public static void removeTempBonus(UUID uuid, String statName, double value) {
        Map<String, Double> stats = tempBonus.get(uuid);
        if (stats == null) return;

        stats.merge(statName, -value, Double::sum);

        // Dọn sạch nếu về 0 hoặc âm
        stats.computeIfPresent(statName, (k, v) -> v <= 0.0 ? null : v);
        if (stats.isEmpty()) tempBonus.remove(uuid);
    }

    /**
     * Xoá toàn bộ temp bonus của player (ví dụ khi logout).
     */
    public static void clearTempBonus(UUID uuid) {
        tempBonus.remove(uuid);
        tempPercentBonus.remove(uuid);

    }

    // -------------------------------------------------------
    //  PERMANENT BONUS (RAM cache load từ DB - dành cho /attr permstats)
    // -------------------------------------------------------

    /**
     * Set giá trị permanent bonus trong RAM (đồng bộ với DB sau khi DatabaseManager
     * đã ghi). Gọi sau mỗi lần addPermStat/clearPermStat ở DB để RAM luôn khớp DB,
     * KHÔNG cần đọc lại DB mỗi lần (tránh query liên tục trong damage event).
     */
    public static void setPermBonus(UUID uuid, String statName, double value) {
        Map<String, Double> stats = permBonus.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (value == 0.0) {
            stats.remove(statName);
            if (stats.isEmpty()) permBonus.remove(uuid);
        } else {
            stats.put(statName, value);
        }
    }

    /**
     * Nạp toàn bộ permanent stats của 1 player từ DB vào RAM.
     * Gọi lúc PlayerJoinEvent. Ghi đè toàn bộ map cũ của uuid đó (nếu có).
     */
    public static void loadPermBonus(UUID uuid, Map<String, Double> statsFromDb) {
        if (statsFromDb == null || statsFromDb.isEmpty()) {
            permBonus.remove(uuid);
            return;
        }
        permBonus.put(uuid, new ConcurrentHashMap<>(statsFromDb));
    }

    // -------------------------------------------------------
    //  PERMANENT PERCENT BONUS (RAM cache load từ DB - dành cho /attr permstats <value%>)
    // -------------------------------------------------------

    public static void setPermPercentBonus(UUID uuid, String statName, double value) {
        Map<String, Double> stats = permPercentBonus.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (value == 0.0) {
            stats.remove(statName);
            if (stats.isEmpty()) permPercentBonus.remove(uuid);
        } else {
            stats.put(statName, value);
        }
    }

    public static void loadPermPercentBonus(UUID uuid, Map<String, Double> statsFromDb) {
        if (statsFromDb == null || statsFromDb.isEmpty()) {
            permPercentBonus.remove(uuid);
            return;
        }
        permPercentBonus.put(uuid, new ConcurrentHashMap<>(statsFromDb));
    }

    /**
     * Xoá toàn bộ permanent bonus của player khỏi RAM (ví dụ lúc logout, tránh leak memory).
     * KHÔNG đụng tới DB - chỉ dọn RAM.
     */
    public static void unloadPermBonus(UUID uuid) {
        permBonus.remove(uuid);
        permPercentBonus.remove(uuid);
    }

    // -------------------------------------------------------
    //  PACK BONUS CACHE (tính từ DB)
    // -------------------------------------------------------

    public static void invalidateCache(UUID uuid) {
        if (uuid != null) bonusCache.remove(uuid);
    }

    public static void clearAllCache() {
        bonusCache.clear();
    }

    // -------------------------------------------------------
    //  PRIVATE
    // -------------------------------------------------------

    private static double calculateBonus(UUID uuid, String statName) {
        Main plugin = Main.getInstance();
        if (plugin == null) return 0.0;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("attributes");
        if (section == null) return 0.0;

        Player player = Bukkit.getPlayer(uuid);
        String className = "";

        // Gọi qua Helper - nơi duy nhất được phép chứa logic của plugin ngoài
        if (player != null) {
            className = AttributeHelper.getPlayerClassName(player).toLowerCase();
        }

        double total = 0.0;
        for (String packId : section.getKeys(false)) {
            int level = plugin.getDatabaseManager().getAttributeLevel(uuid, packId);
            if (level <= 0) continue;

            ConfigurationSection pack = section.getConfigurationSection(packId);
            if (pack == null) continue;

            double baseValue = pack.getDouble(statName, 0.0);
            double classBonus = !className.isEmpty()
                    ? pack.getDouble("class-bonus." + className + "." + statName, 0.0)
                    : 0.0;

            total += (baseValue + classBonus) * level;
        }
        return total;
    }
}