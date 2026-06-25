package org.ThienDev.Listener;

import org.ThienDev.Api.AttributeAPI;
import org.ThienDev.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Nạp permanent stats (player_perm_stats trong DB) vào RAM lúc player join,
 * và dọn RAM lúc player quit để tránh leak memory.
 *
 * Không cần load lại lúc respawn: RAM không bị xoá khi respawn nên dữ liệu
 * vẫn còn nguyên, không cần đọc DB lại.
 */
public class PermStatsListener implements Listener {

    private final Main plugin;

    public PermStatsListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Đọc DB nên chạy async để tránh lag chính lúc player join
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Double> stats = plugin.getDatabaseManager().getAllPermStats(uuid);
            Map<String, Double> percentStats = plugin.getDatabaseManager().getAllPermPercentStats(uuid);

            // Quay lại main thread để ghi vào RAM (AttributeAPI dùng ConcurrentHashMap nên
            // về lý thuyết an toàn ở thread khác, nhưng để đồng bộ với refreshCache thì
            // nên gọi trên main thread cho chắc).
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                AttributeAPI.loadPermBonus(uuid, stats);
                AttributeAPI.loadPermPercentBonus(uuid, percentStats);

                // Báo cho plugin combat bên ngoài (CacheListener) biết stat đã sẵn sàng
                org.bukkit.entity.Player player = event.getPlayer();
                if (player.isOnline()) {
                    try {
                        org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                    } catch (Throwable ignored) {
                        // CacheListener thuộc plugin khác, có thể chưa load xong hoặc không tồn tại
                    }
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Chỉ dọn RAM, không đụng DB - permanent stats vẫn còn nguyên trong database
        AttributeAPI.unloadPermBonus(event.getPlayer().getUniqueId());
    }
}