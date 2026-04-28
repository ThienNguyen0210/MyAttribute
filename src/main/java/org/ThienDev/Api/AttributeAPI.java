package org.ThienDev.Api;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;
import org.ThienDev.Main;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class AttributeAPI {
    public static double getBonus(UUID uuid, String statName) {
        Main plugin = Main.getInstance();
        if (plugin == null) return 0.0;

        double total = 0.0;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("attributes");

        if (section != null) {
            Player player = Bukkit.getPlayer(uuid);
            String className = "";
            if (player != null) {
                PlayerData data = SkillAPI.getPlayerData(player);
                if (data != null && data.getMainClass() != null) {
                    className = data.getMainClass().getData().getName().toLowerCase();
                }
            }

            for (String packId : section.getKeys(false)) {
                int level = plugin.getDatabaseManager().getAttributeLevel(uuid, packId);
                if (level <= 0) continue;

                ConfigurationSection pack = section.getConfigurationSection(packId);
                if (pack == null) continue;

                // Lấy gốc (2)
                double baseValue = pack.getDouble(statName, 0.0);

                // Lấy bonus (4) - Phải lấy qua packId hiện tại
                double classBonus = 0.0;
                if (!className.isEmpty()) {
                    classBonus = pack.getDouble("class-bonus." + className + "." + statName, 0.0);
                }

                // (2 + 4) * level = 6
                total += (baseValue + classBonus) * level ;
            }
        }
        return total;
    }
}