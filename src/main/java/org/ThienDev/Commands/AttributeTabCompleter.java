package org.ThienDev.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer cho /attr.
 * Đã cập nhật danh sách STAT_NAMES đồng bộ với hệ thống stats của server.
 */
public class AttributeTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "stats", "permstats", "clearperm", "reset"
    );

    // Danh sách stats đã được cập nhật chính xác theo data hệ thống
    private static final List<String> STAT_NAMES = Arrays.asList(
            "damage", "health", "armor", "pve_damage", "pvp_damage", "pve_defense", "pvp_defense",
            "critical_chance", "critical_damage", "lifesteal", "dodge_rate", "block_rate", "penetration",
            "level_require", "true_damage", "thorns", "class_require", "max_mana", "mana_regen",
            "exp_bonus", "attack_speed", "movement_speed", "health_regen", "armor_pen", "all_damage",
            "all_defense", "bow_damage", "knockback_resistance", "death_damage", "durability",
            "magic_damage", "magic_defense", "Accuracy", "critical_damage_reduction", "damage_reduction"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // /attr <tab>
            completions.addAll(filter(SUBCOMMANDS, args[0]));
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("stats") || sub.equals("permstats") || sub.equals("clearperm") || sub.equals("reset")) {
                // /attr <sub> <tab> -> tên player online
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                completions.addAll(filter(playerNames, args[1]));
            }
            return completions;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            // reset không cần nhập stat name vì nó sẽ reset toàn bộ dữ liệu của player đó
            if (sub.equals("stats") || sub.equals("permstats") || sub.equals("clearperm")) {
                // /attr <sub> <player> <tab> -> tên stat
                completions.addAll(filter(STAT_NAMES, args[2]));
            }
            return completions;
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            // clearperm chỉ xóa một stat cụ thể nên args[3] không cần nhận thêm value nữa
            if (sub.equals("stats")) {
                completions.addAll(filter(Arrays.asList("10", "25", "50", "-10", "10%"), args[3]));
            } else if (sub.equals("permstats")) {
                // permstats hỗ trợ cả flat và % vĩnh viễn
                completions.addAll(filter(Arrays.asList("10", "25", "50", "-10", "10%", "25%", "-5%"), args[3]));
            }
            return completions;
        }

        if (args.length == 5) {
            String sub = args[0].toLowerCase();
            if (sub.equals("stats")) {
                // /attr stats <player> <stat> <value> <tab> -> gợi ý thời gian (giây)
                completions.addAll(filter(Arrays.asList("10", "30", "60", "300"), args[4]));
            }
            return completions;
        }

        return completions;
    }

    private List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}