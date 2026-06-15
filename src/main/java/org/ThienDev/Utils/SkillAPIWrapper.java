package org.ThienDev.Utils;
import com.sucy.skill.SkillAPI;
import org.bukkit.entity.Player;

public class SkillAPIWrapper {
    public static int getPoints(Player p) { return SkillAPI.getPlayerData(p).getAttributePoints(); }
    public static void givePoints(Player p, int amount) { SkillAPI.getPlayerData(p).giveAttribPoints(amount); }
    public static boolean hasClass(Player p) { return SkillAPI.getPlayerData(p).getMainClass() != null; }
    public static String getPlayerClassName(Player player) {
        com.sucy.skill.api.player.PlayerData data = com.sucy.skill.SkillAPI.getPlayerData(player);
        return (data != null && data.getMainClass() != null) ? data.getMainClass().getData().getName() : "";
    }
}