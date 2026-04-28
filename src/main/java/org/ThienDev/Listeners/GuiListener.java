package org.ThienDev.Listeners;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;
import org.ThienDev.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
    private final Main plugin;

    public GuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String rawTitle = plugin.getGuiManager().getGuiConfig().getString("menu.title");
        if (rawTitle == null) return;

        String title = ChatColor.translateAlternateColorCodes('&', rawTitle);
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String attrId = plugin.getGuiManager().getAttributeIdAtSlot(event.getSlot());
        if (attrId != null) {
            handleUpgrade(player, attrId);
        }
    }

    private void handleUpgrade(Player player, String attrId) {
        PlayerData data = SkillAPI.getPlayerData(player);
        if (data == null || data.getAttributePoints() < 1) {
            player.sendMessage("§c[!] Bạn không đủ điểm AP!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 1. Lấy Class từ Fabled
        if (data.getMainClass() == null) {
            player.sendMessage("§c[!] Bạn phải chọn Class trước!");
            return;
        }
        String className = data.getMainClass().getData().getName().toLowerCase();

        // 2. Lấy Config của thuộc tính
        ConfigurationSection attrConfig = plugin.getConfig().getConfigurationSection("attributes." + attrId);
        if (attrConfig == null) return;

        // 3. TÍNH TOÁN GIÁ TRỊ THỰC (Để in ra message)
        double baseH = attrConfig.getDouble("health", 0);
        double bonusH = attrConfig.getDouble("class-bonus." + className + ".health", 0);
        double totalPerLv = baseH + bonusH; // 2 + 4 = 6

        // 4. Lưu Database
        data.giveAttribPoints(-1);
        plugin.getDatabaseManager().addAttributeLevel(player.getUniqueId(), attrId, 1);
        int newLevel = plugin.getDatabaseManager().getAttributeLevel(player.getUniqueId(), attrId);



        // 6. IN GIÁ TRỊ BONUS RA CHAT
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.sendMessage("§a§l[+] NÂNG CẤP THÀNH CÔNG!");
        player.sendMessage("§fChỉ số gốc: §e+" + baseH + " HP");
        if (bonusH > 0) {
            player.sendMessage("§6§l+ Bonus Class " + className.toUpperCase() + ": §e+" + bonusH + " HP");
        }
        player.sendMessage("§b§l=> Tổng cộng mỗi điểm: §d+" + totalPerLv + " HP");

        // Refresh GUI
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openMainGui(player));
    }
}