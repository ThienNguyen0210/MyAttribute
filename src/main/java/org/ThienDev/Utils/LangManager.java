package org.ThienDev.Utils;

import org.ThienDev.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Quản lý file lang.yml — cho phép server custom toàn bộ message của plugin.
 *
 * Cách dùng:
 *   LangManager.get("no-permission")
 *   LangManager.get("permstats-success", "stat", "damage", "sign_value", "+50%", "total", "50%", "player", "Thien")
 *
 * Placeholder dạng {key} trong lang.yml sẽ được thay bằng value tương ứng.
 */
public class LangManager {

    private static FileConfiguration lang;
    private static FileConfiguration defaults;

    private LangManager() {}

    /**
     * Load (hoặc reload) lang.yml. Gọi trong onEnable() và lệnh /attr reload nếu có.
     */
    public static void load(Main plugin) {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");

        // Tạo file mặc định từ resources nếu chưa có
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults từ jar để fallback nếu key bị xoá
        InputStream defaultStream = plugin.getResource("lang.yml");
        if (defaultStream != null) {
            defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            lang.setDefaults(defaults);
        }
    }

    /**
     * Lấy message theo key, dịch màu &, thay placeholder {key} = value.
     *
     * @param key          Key trong lang.yml (ví dụ: "no-permission")
     * @param placeholders Cặp key-value xen kẽ: "player", "Thien", "stat", "damage", ...
     */
    public static String get(String key, String... placeholders) {
        if (lang == null) return "§c[Lang not loaded] " + key;

        String msg = lang.getString(key, defaults != null ? defaults.getString(key, key) : key);
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        // Thay placeholder {key} → value
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }

    /**
     * Lấy message theo key với Map placeholder (tiện khi có nhiều placeholder).
     */
    public static String get(String key, Map<String, String> placeholders) {
        if (lang == null) return "§c[Lang not loaded] " + key;

        String msg = lang.getString(key, defaults != null ? defaults.getString(key, key) : key);
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }
}