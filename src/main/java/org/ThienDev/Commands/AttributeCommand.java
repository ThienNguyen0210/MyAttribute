package org.ThienDev.Commands;

import org.ThienDev.Api.AttributeAPI;
import org.ThienDev.Utils.LangManager;
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
                sender.sendMessage(LangManager.get("console-only-text"));
                return true;
            }
            plugin.getGuiManager().openMainGui((Player) sender);
            return true;
        }

        // /attr stats <player> <stats> <value> <second>
        if (args[0].equalsIgnoreCase("stats")) {

            if (sender instanceof Player && !sender.hasPermission("myattribute.stats")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            if (args.length < 5) {
                sender.sendMessage(LangManager.get("stats-usage"));
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
                sender.sendMessage(LangManager.get("stats-invalid-value"));
                return true;
            }

            try {
                seconds = Integer.parseInt(args[4]);
                if (seconds <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(LangManager.get("stats-invalid-second"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(LangManager.get("player-not-found", "player", targetName));
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
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(LangManager.get("reset-usage"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(LangManager.get("player-not-found", "player", args[1]));
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

            target.sendMessage(LangManager.get("reset-success-target", "points", String.valueOf(usedPoints)));
            if (!sender.equals(target)) {
                sender.sendMessage(LangManager.get("reset-success-sender", "player", target.getName(), "points", String.valueOf(usedPoints)));
            }

            return true;
        }

        // /attr permstats <player> <stat> <value>  (thêm % để là permanent %)
        // Cộng dồn (hoặc trừ nếu value âm) vĩnh viễn vào 1 stat, lưu DB, áp dụng ngay
        // và áp dụng lại mỗi lần player join (không hết hạn, không cần BukkitRunnable).
        if (args[0].equalsIgnoreCase("permstats")) {

            if (sender instanceof Player && !sender.hasPermission("myattribute.permstats")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(LangManager.get("permstats-usage"));
                sender.sendMessage(LangManager.get("permstats-usage-hint"));
                return true;
            }

            String targetName = args[1];
            String statName    = args[2];
            String rawValue    = args[3];
            boolean isPercent  = rawValue.endsWith("%");
            double value;

            try {
                value = Double.parseDouble(isPercent ? rawValue.replace("%", "") : rawValue);
            } catch (NumberFormatException e) {
                sender.sendMessage(LangManager.get("permstats-invalid-value"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(LangManager.get("player-not-found", "player", targetName));
                return true;
            }

            double newTotal;
            String displayVal = value >= 0 ? "+" + value : String.valueOf(value);

            if (isPercent) {
                // 1. Cộng dồn vào DB percent, lấy về tổng mới
                plugin.getDatabaseManager().addPermPercentStat(target.getUniqueId(), statName, value);
                newTotal = plugin.getDatabaseManager().getPermPercentStat(target.getUniqueId(), statName);

                // 2. Đồng bộ RAM ngay
                AttributeAPI.setPermPercentBonus(target.getUniqueId(), statName, newTotal);
            } else {
                // 1. Cộng dồn vào DB flat, lấy về tổng mới
                plugin.getDatabaseManager().addPermStat(target.getUniqueId(), statName, value);
                newTotal = plugin.getDatabaseManager().getPermStat(target.getUniqueId(), statName);

                // 2. Đồng bộ RAM ngay
                AttributeAPI.setPermBonus(target.getUniqueId(), statName, newTotal);
            }

            // 3. Xoá bonusCache TRƯỚC khi refreshCache
            AttributeAPI.invalidateCache(target.getUniqueId());

            // 4. refreshCache để PlayerCombatCache cập nhật stats mới
            org.ThienNguyen.Listener.CacheListener.refreshCache(target);

            String suffix = isPercent ? "%" : "";
            String sign = value >= 0 ? "+" : "";
            String signValue = sign + value + suffix;
            String totalDisplay = newTotal + suffix;
            sender.sendMessage(LangManager.get("permstats-success-sender",
                    "sign_value", signValue, "stat", statName, "player", target.getName(), "total", totalDisplay));
            if (!sender.equals(target)) {
                target.sendMessage(LangManager.get("permstats-success-target",
                        "sign_value", signValue, "stat", statName, "total", totalDisplay));
            }

            return true;
        }

        // /attr clearperm <player> <stat>
        // Xoá permanent stat của 1 stat cụ thể (không ảnh hưởng các stat permanent khác)
        if (args[0].equalsIgnoreCase("clearperm")) {

            if (sender instanceof Player && !sender.hasPermission("myattribute.clearperm")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(LangManager.get("clearperm-usage"));
                return true;
            }

            String targetName = args[1];
            String statName    = args[2];

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(LangManager.get("player-not-found", "player", targetName));
                return true;
            }

            // 1. Xoá khỏi DB (cả flat lẫn percent)
            plugin.getDatabaseManager().clearPermStat(target.getUniqueId(), statName);
            plugin.getDatabaseManager().clearPermPercentStat(target.getUniqueId(), statName);

            // 2. Xoá khỏi RAM (set về 0 sẽ tự remove khỏi map)
            AttributeAPI.setPermBonus(target.getUniqueId(), statName, 0.0);
            AttributeAPI.setPermPercentBonus(target.getUniqueId(), statName, 0.0);

            // 3. Xoá bonusCache TRƯỚC khi refreshCache
            AttributeAPI.invalidateCache(target.getUniqueId());

            // 4. refreshCache
            org.ThienNguyen.Listener.CacheListener.refreshCache(target);

            sender.sendMessage(LangManager.get("clearperm-success-sender", "stat", statName, "player", target.getName()));
            if (!sender.equals(target)) {
                target.sendMessage(LangManager.get("clearperm-success-target", "stat", statName));
            }

            return true;
        }

        return true;
    }
}