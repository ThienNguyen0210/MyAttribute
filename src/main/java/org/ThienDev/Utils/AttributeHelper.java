package org.ThienDev.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AttributeHelper {
    public static String getPlayerClassName(Player player) {
        // Kiểm tra SkillAPI (Fabled)
        if (Bukkit.getPluginManager().isPluginEnabled("SkillAPI")) {
            return getSkillAPIClassName(player);
        }
        // Kiểm tra MMOCore
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            return getMMOCoreClassName(player);
        }
        return "";
    }

    // JVM chỉ load các method này khi chúng được gọi
    private static String getSkillAPIClassName(Player p) {
        try {
            com.sucy.skill.api.player.PlayerData data = com.sucy.skill.SkillAPI.getPlayerData(p);
            return (data != null && data.getMainClass() != null) ? data.getMainClass().getData().getName() : "";
        } catch (Throwable t) { return ""; }
    }

    private static String getMMOCoreClassName(Player p) {
        try {
            net.Indyuce.mmocore.api.player.PlayerData data = net.Indyuce.mmocore.api.player.PlayerData.get(p);
            return (data != null && data.getProfess() != null) ? data.getProfess().getName() : "";
        } catch (Throwable t) { return ""; }
    }
}