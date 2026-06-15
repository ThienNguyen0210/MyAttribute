package org.ThienDev.Manager;

import org.ThienDev.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiManager {
    private final java.util.Set<UUID> reOpeningPlayers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Main plugin;
    private FileConfiguration guiConfig;
    private File guiFile;

    private final Map<UUID, SavedInventory> savedInventories = new ConcurrentHashMap<>();
    public void setReOpening(UUID uuid, boolean status) {
        if (status) reOpeningPlayers.add(uuid);
        else reOpeningPlayers.remove(uuid);
    }

    public boolean isReOpening(UUID uuid) {
        return reOpeningPlayers.contains(uuid);
    }
    public GuiManager(Main plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        if (guiFile == null) {
            guiFile = new File(plugin.getDataFolder(), "GUI.yml");
        }
        if (!guiFile.exists()) {
            plugin.saveResource("GUI.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public void openMainGui(Player player) {
        openMainGui(player, true); // Mở lần đầu
    }

    public void openMainGui(Player player, boolean isFirstOpen) {
        if (player == null) return;

        int attributePoints = getAttributePoints(player);

        String rawTitle = guiConfig.getString("menu.title", "&8Chỉnh sửa thuộc tính {points}");
        String finalTitle = replacePlaceholders(rawTitle, player);
        finalTitle = ChatColor.translateAlternateColorCodes('&', finalTitle);

        int size = guiConfig.getInt("menu.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, finalTitle);

        boolean clearInventory = plugin.getConfig().getBoolean("gui.clear-inventory-on-open", false);

        // CHỈ CLEAR VÀ LƯU KHI MỞ LẦN ĐẦU TIÊN
        if (clearInventory && isFirstOpen) {
            savePlayerInventory(player);
            clearPlayerInventory(player);
        }

        if (guiConfig.contains("menu.items")) {
            for (String key : guiConfig.getConfigurationSection("menu.items").getKeys(false)) {
                loadGuiItem(inv, key, player);
            }
        }

        player.openInventory(inv);
    }

    private void loadGuiItem(Inventory inv, String key, Player player) {
        try {
            String path = "menu.items." + key;
            int slot = guiConfig.getInt(path + ".slot", -1);
            if (slot < 0 || slot >= inv.getSize()) return;

            // Lấy vật liệu từ config
            Material material = Material.valueOf(
                    guiConfig.getString(path + ".material", "STONE").toUpperCase());

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // 1. Xử lý Tên vật phẩm
                String rawName = guiConfig.getString(path + ".name", "&fItem");
                String name = replacePlaceholders(rawName, player);
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

                // 2. XỬ LÝ LORE THEO CLASS
                String className = getPlayerClassName(player); // Lấy tên Class (ví dụ: Warrior, Archer...)
                List<String> rawLore;

                // Kiểm tra nếu có lore dành riêng cho class này
                String classLorePath = path + ".class_lore." + className;
                if (guiConfig.contains(classLorePath)) {
                    rawLore = guiConfig.getStringList(classLorePath);
                } else {
                    // Nếu không có class_lore cho class cụ thể, dùng lore mặc định
                    rawLore = guiConfig.getStringList(path + ".lore");
                }

                // Render toàn bộ Placeholder trong Lore
                List<String> lore = new ArrayList<>();
                for (String line : rawLore) {
                    // replacePlaceholders sẽ xử lý cả {attr_level}, {real_stat} và PAPI
                    lore.add(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, player)));
                }
                meta.setLore(lore);

                // 3. Hỗ trợ Custom Model Data
                int modelId = guiConfig.getInt(path + ".model", -1);
                if (modelId > 0) {
                    meta.setCustomModelData(modelId);
                }

                // 4. Áp dụng Meta vào item
                item.setItemMeta(meta);
            }

            // Đặt item vào đúng slot trong Inventory
            inv.setItem(slot, item);

        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi load item GUI key '" + key + "': " + e.getMessage());
        }
    }
    private String getPlayerClassName(Player player) {
        // 1. Kiểm tra SkillAPI (Fabled)
        if (Bukkit.getPluginManager().isPluginEnabled("SkillAPI")) {
            try {
                // Sử dụng logic riêng để tránh lỗi class not found
                return getSkillAPIClassName(player);
            } catch (Exception ignored) {}
        }

        // 2. Kiểm tra MMOCore
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            try {
                return getMMOCoreClassName(player);
            } catch (Exception ignored) {}
        }

        return "Không có lớp";
    }

    // Tách riêng method để tránh load class khi không cần thiết
    private String getSkillAPIClassName(Player player) {
        com.sucy.skill.api.player.PlayerData data = com.sucy.skill.SkillAPI.getPlayerData(player);
        return (data != null && data.getMainClass() != null) ? data.getMainClass().getData().getName() : "Không có lớp";
    }

    private String getMMOCoreClassName(Player player) {
        net.Indyuce.mmocore.api.player.PlayerData data = net.Indyuce.mmocore.api.player.PlayerData.get(player);
        return (data != null && data.getProfess() != null) ? data.getProfess().getName() : "Không có lớp";
    }
    /** Phương thức chính thay thế tất cả placeholder */
    private String replacePlaceholders(String text, Player player) {
        if (text == null || player == null) return "";

        int points = getAttributePoints(player);
        String className = getPlayerClassName(player);

        String result = text
                .replace("{points}", String.valueOf(points))
                .replace("{class}", className)
                .replace("{class_name}", className);

        // Sửa dấu \\d+ thành [\\d.]+ để nhận diện được số thập phân (như 0.01)
        Pattern attrPattern = Pattern.compile("\\{attr_level:([a-zA-Z0-9_]+)([\\s]*[\\+\\-\\*\\/][\\s]*[\\d.]+)?\\}");
        Matcher matcher = attrPattern.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String attrId = matcher.group(1);
            String operation = matcher.group(2) != null ? matcher.group(2).trim() : "";

            int level = getAttributeLevel(player, attrId);

            // Sử dụng double để tính toán chính xác với số thập phân
            double finalValue = calculateValue(level, operation);

            // Định dạng hiển thị: Nếu là số nguyên thì hiện số nguyên, nếu lẻ thì hiện 2 chữ số thập phân
            String valueStr = (finalValue == (long) finalValue)
                    ? String.valueOf((long) finalValue)
                    : String.format("%.2f", finalValue);

            matcher.appendReplacement(sb, valueStr);
        }
        matcher.appendTail(sb);
        result = sb.toString();


        // ================== HỖ TRỢ PLACEHOLDERAPI (%...%) ==================
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
        }
        // =================================================================

        return result;
    }

    private double calculateValue(double baseLevel, String operation) {
        if (operation == null || operation.isEmpty()) return baseLevel;

        try {
            String op = operation.replaceAll("\\s+", ""); // bỏ khoảng trắng
            char operator = op.charAt(0);
            double number = Double.parseDouble(op.substring(1));

            return switch (operator) {
                case '+' -> baseLevel + number;
                case '-' -> baseLevel - number;
                case '*' -> baseLevel * number;
                case '/' -> (number != 0) ? baseLevel / number : baseLevel;
                default -> baseLevel;
            };
        } catch (Exception e) {
            return baseLevel;
        }
    }

    private int getAttributePoints(Player player) {
        // 1. Ưu tiên SkillAPI (Fabled)
        if (Bukkit.getPluginManager().isPluginEnabled("SkillAPI")) {
            try {
                com.sucy.skill.api.player.PlayerData data = com.sucy.skill.SkillAPI.getPlayerData(player);
                if (data != null && data.getAttributePoints() > 0) {
                    return data.getAttributePoints();
                }
            } catch (Exception ignored) {}
        }

        // 2. Nếu không có SkillAPI hoặc không có điểm, kiểm tra MMOCore
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            try {
                net.Indyuce.mmocore.api.player.PlayerData data = net.Indyuce.mmocore.api.player.PlayerData.get(player);
                return data.getAttributePoints();
            } catch (Exception ignored) {}
        }

        return 0;
    }
    private int getAttributeLevel(Player player, String attrId) {
        if (attrId == null || attrId.isEmpty()) return 0;
        return plugin.getDatabaseManager().getAttributeLevel(player.getUniqueId(), attrId);
    }

    private void clearPlayerInventory(Player player) {
        // Chỉ xóa các ô trong túi đồ chính (từ slot 0 đến 35)
        // Giữ lại 4 ô giáp (slots 36-39) và ô tay trái (slot 40)
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, null);
        }

        // Nếu bạn muốn chắc chắn không đụng vào giáp, đừng gọi các hàm setArmorContents(null)
        player.updateInventory();
    }

    private void savePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        SavedInventory saved = new SavedInventory(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getExtraContents()
        );
        savedInventories.put(uuid, saved);
    }

    public void restorePlayerInventory(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        SavedInventory saved = savedInventories.remove(uuid);

        if (saved == null) return;

        // Restore main inventory (36 slot)
        ItemStack[] savedMain = saved.getMainInventory();
        for (int i = 0; i < 36; i++) {
            if (i < savedMain.length) {
                player.getInventory().setItem(i, savedMain[i]);
            } else {
                player.getInventory().setItem(i, null);
            }
        }

        // Restore giáp và offhand (không bị clear từ đầu nên vẫn giữ nguyên)
        player.getInventory().setArmorContents(saved.getArmorContents());
        player.getInventory().setExtraContents(saved.getExtraContents());

        player.updateInventory();
    }

    public String getAttributeIdAtSlot(int clickedSlot) {
        if (guiConfig.contains("menu.items")) {
            for (String key : guiConfig.getConfigurationSection("menu.items").getKeys(false)) {
                if (guiConfig.getInt("menu.items." + key + ".slot") == clickedSlot) {
                    return guiConfig.getString("menu.items." + key + ".attribute_id");
                }
            }
        }
        return null;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    private static class SavedInventory {
        private final ItemStack[] main;
        private final ItemStack[] armor;
        private final ItemStack[] extra;

        public SavedInventory(ItemStack[] main, ItemStack[] armor, ItemStack[] extra) {
            this.main = main != null ? main.clone() : new ItemStack[0];
            this.armor = armor != null ? armor.clone() : new ItemStack[4];
            this.extra = extra != null ? extra.clone() : new ItemStack[1];
        }

        public ItemStack[] getMainInventory() { return main; }
        public ItemStack[] getArmorContents() { return armor; }
        public ItemStack[] getExtraContents() { return extra; }
    }
}