package org.ThienDev.Commands;

import org.ThienDev.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AttributeCommand implements CommandExecutor {
    private final Main plugin;

    public AttributeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 1. Nếu không nhập gì (/attr), mở GUI cho người chơi
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cChỉ người chơi mới có thể mở giao diện!");
                return true;
            }
            plugin.getGuiManager().openMainGui(player);
            return true;
        }

        // 2. Lệnh /attr reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("myattribute.admin")) {
                sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                return true;
            }
            plugin.getGuiManager().reloadConfig();
            sender.sendMessage("§a[MyAttribute] Đã tải lại cấu hình GUI.yml!");
            return true;
        }

// 3. Lệnh /attr reset <player>
        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("myattribute.admin")) {
                sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cSử dụng: /attr reset <tên_người_chơi>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cNgười chơi này không online!");
                return true;
            }

            // --- BẮT ĐẦU QUY TRÌNH HOÀN ĐIỂM ---

            // Bước 1: Tính toán tổng điểm đã nâng từ Database SQLite của bạn
            // Giả sử mỗi Level bạn tốn 1 điểm AP (Attribute Point).
            // Nếu config của bạn tốn nhiều hơn, hãy nhân với hệ số đó.
            int totalPointsToRefund = 0;

            // Bạn cần tạo hàm này trong DatabaseManager để SUM(level) của UUID đó
            // Ví dụ: SELECT SUM(level) FROM player_attributes WHERE uuid = ?
            totalPointsToRefund = plugin.getDatabaseManager().getTotalLevels(target.getUniqueId());

            // Bước 2: Tác động vào Fabled/SkillAPI
            if (Bukkit.getPluginManager().isPluginEnabled("Fabled") || Bukkit.getPluginManager().isPluginEnabled("SkillAPI")) {
                try {
                    com.sucy.skill.api.player.PlayerData data = com.sucy.skill.SkillAPI.getPlayerData(target);
                    if (data != null) {
                        // Xóa các điểm đã cộng trong bảng thuộc tính của Fabled
                        data.refundAttributes();

                        // Cộng lại số điểm AP dựa trên dữ liệu Database SQLite mà mình vừa tính được
                        if (totalPointsToRefund > 0) {
                            // Dùng hàm này từ stub bạn gửi: public void giveAttribPoints(int amount)
                            data.giveAttribPoints(totalPointsToRefund);
                        }

                        // Cập nhật lại chỉ số máu, mana, tốc độ chạy ngay lập tức
                        data.updatePlayerStat(target);

                        target.sendMessage("§a[Fabled] §fĐã thu hồi và hoàn trả §e" + totalPointsToRefund + " §fđiểm tiềm năng.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Lỗi hoàn điểm Fabled: " + e.getMessage());
                }
            }

            // Bước 3: Reset Database SQLite về 0
            plugin.getDatabaseManager().resetAttributes(target.getUniqueId());
            target.sendMessage("§e[MyAttribute] §fToàn bộ chỉ số đã được reset về mặc định!");
            sender.sendMessage("§aĐã reset và hoàn điểm thành công cho §f" + target.getName());
            return true;
        }

        // Nếu gõ lệnh lạ (ví dụ /attr abc)
        sender.sendMessage("§c[MyAttribute] Lệnh không tồn tại. Dùng /attr để mở Menu.");
        return true;
    }
}