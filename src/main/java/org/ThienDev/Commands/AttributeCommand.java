package org.ThienDev.Commands;

import org.ThienDev.Api.AttributeAPI;
import org.ThienDev.Main;
import org.ThienDev.Utils.MMOCoreWrapper;
import org.ThienDev.Utils.SkillAPIWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AttributeCommand implements CommandExecutor {

    private final Main plugin;

    public AttributeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /attr — mở GUI cho người chơi
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole không mở được GUI! Dùng: /attr stats <player> <stats> <value> <second>");
                return true;
            }
            plugin.getGuiManager().openMainGui((Player) sender);
            return true;
        }

        // /attr stats <player> <stats> <value> <second>
        if (args[0].equalsIgnoreCase("stats")) {

            if (sender instanceof Player && !sender.hasPermission("myattribute.stats")) {
                sender.sendMessage("§cBạn không có quyền dùng lệnh này!");
                return true;
            }

            if (args.length < 5) {
                sender.sendMessage("§eUsage: /attr stats <player> <stats> <value> <second>");
                return true;
            }

            String targetName = args[1];
            String statName   = args[2];
            String rawValue   = args[3];
            boolean isPercent = rawValue.endsWith("%");
            double value;
            int seconds;

            try {
                value = Double.parseDouble(isPercent ? rawValue.replace("%", "") : rawValue);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c<value> phải là số! Ví dụ: 10 hoặc 10% hoặc 10.5");
                return true;
            }

            try {
                seconds = Integer.parseInt(args[4]);
                if (seconds <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage("§c<second> phải là số nguyên dương! Ví dụ: 30");
                return true;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("§cKhông tìm thấy người chơi §e" + targetName + " §ctrên server!");
                return true;
            }

            // 1. Cộng bonus vào RAM
            if (isPercent) {
                AttributeAPI.addTempPercentBonus(target.getUniqueId(), statName, value);
            } else {
                AttributeAPI.addTempBonus(target.getUniqueId(), statName, value);
            }

            // 2. Xoá bonusCache TRƯỚC khi refreshCache
            AttributeAPI.invalidateCache(target.getUniqueId());

            // 3. refreshCache để PlayerCombatCache cập nhật stats mới
            org.ThienNguyen.Listener.CacheListener.refreshCache(target);

            String displayVal = isPercent ? value + "%" : String.valueOf(value);


            // Tự động xoá buff sau <seconds> giây
            final double finalValue = value;
            final boolean finalIsPercent = isPercent;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 1. Xoá bonus
                    if (finalIsPercent) {
                        AttributeAPI.removeTempPercentBonus(target.getUniqueId(), statName, finalValue);
                    } else {
                        AttributeAPI.removeTempBonus(target.getUniqueId(), statName, finalValue);
                    }

                    // 2. Xoá bonusCache TRƯỚC khi refreshCache
                    AttributeAPI.invalidateCache(target.getUniqueId());

                    // 3. refreshCache
                    if (target.isOnline()) {
                        org.ThienNguyen.Listener.CacheListener.refreshCache(target);
                    }
                }
            }.runTaskLater(plugin, (long) seconds * 20L);

            return true;
        }

        // /attr reset <player>
        if (args[0].equalsIgnoreCase("reset")) {

            if (sender instanceof Player && !sender.hasPermission("myattribute.reset")) {
                sender.sendMessage("§cBạn không có quyền dùng lệnh này!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§eUsage: /attr reset <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cKhông tìm thấy người chơi §e" + args[1] + " §ctrên server!");
                return true;
            }

            // 1. Lấy tổng số điểm đã dùng TRƯỚC khi reset
            int usedPoints = plugin.getDatabaseManager().getTotalLevels(target.getUniqueId());

            // 2. Reset toàn bộ attribute trong database
            plugin.getDatabaseManager().resetAttributes(target.getUniqueId());

            // 3. Hoàn điểm lại cho player qua SkillAPI hoặc MMOCore
            if (usedPoints > 0) {
                if (Bukkit.getPluginManager().isPluginEnabled("SkillAPI")) {
                    try {
                        SkillAPIWrapper.givePoints(target, usedPoints);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Lỗi hoàn điểm SkillAPI cho " + target.getName() + ": " + t.getMessage());
                    }
                } else if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
                    try {
                        MMOCoreWrapper.givePoints(target, usedPoints);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Lỗi hoàn điểm MMOCore cho " + target.getName() + ": " + t.getMessage());
                    }
                }
            }

            // 4. Xoá cache để stat được tính lại từ đầu
            AttributeAPI.invalidateCache(target.getUniqueId());

            // 5. Refresh PlayerCombatCache nếu đang dùng
            try {
                org.ThienNguyen.Listener.CacheListener.refreshCache(target);
            } catch (Throwable ignored) {}

            target.sendMessage("§aToàn bộ attribute của bạn đã được reset. Bạn nhận lại §e" + usedPoints + " §ađiểm!");
            if (!sender.equals(target)) {
                sender.sendMessage("§aĐã reset attribute của §e" + target.getName() + "§a. Hoàn lại §e" + usedPoints + " §ađiểm.");
            }

            return true;
        }

        return true;
    }
}