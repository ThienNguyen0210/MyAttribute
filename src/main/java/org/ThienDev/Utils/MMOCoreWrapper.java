package org.ThienDev.Utils;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.entity.Player;

public class MMOCoreWrapper {
    public static int getPoints(Player p) { return PlayerData.get(p).getAttributePoints(); }
    public static void givePoints(Player p, int amount) { PlayerData.get(p).giveAttributePoints(amount); }
    public static boolean hasClass(Player p) { return PlayerData.get(p).getProfess() != null; }
    public static String getPlayerClassName(Player player) {
        net.Indyuce.mmocore.api.player.PlayerData data = net.Indyuce.mmocore.api.player.PlayerData.get(player);
        return (data != null && data.getProfess() != null) ? data.getProfess().getName() : "";
    }
}