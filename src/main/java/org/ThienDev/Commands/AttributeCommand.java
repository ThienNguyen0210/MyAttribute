package org.ThienDev.Commands;

import org.ThienDev.Api.AttributeAPI;
import org.ThienDev.Main;
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

        return true;
    }
}