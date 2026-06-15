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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GuiListener implements Listener {

    private final Main plugin;

    public GuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String rawTitle = plugin.getGuiManager().getGuiConfig().getString("menu.title");
        if (rawTitle == null || rawTitle.isEmpty()) return;

        // Lấy prefix tiêu đề để nhận diện GUI
        String titlePrefix = ChatColor.translateAlternateColorCodes('&',
                rawTitle.split("\\{points\\}")[0].trim());

        if (!event.getView().getTitle().startsWith(titlePrefix)) return;

        // Chặn mọi hành vi lấy item hoặc bỏ item vào GUI
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        String attrId = plugin.getGuiManager().getAttributeIdAtSlot(event.getSlot());
        if (attrId != null) {
            // Xác định số lượng điểm muốn nâng dựa trên kiểu click
            int amount = 1;
            if (event.isShiftClick()) {
                amount = event.isLeftClick() ? 100 : 1000;
            } else if (event.isRightClick()) {
                amount = 10;
            }

            // Gọi hàm xử lý nâng cấp với số lượng đã định
            handleUpgrade(player, attrId, amount);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        // Kiểm tra nếu là refresh thì thoát luôn, không trả đồ
        if (plugin.getGuiManager().isReOpening(player.getUniqueId())) {
            return;
        }

        String rawTitle = plugin.getGuiManager().getGuiConfig().getString("menu.title");
        if (rawTitle == null || rawTitle.isEmpty()) return;

        String titlePrefix = ChatColor.translateAlternateColorCodes('&',
                rawTitle.split("\\{points\\}")[0].trim());

        if (e.getView().getTitle().startsWith(titlePrefix)) {
            if (plugin.getConfig().getBoolean("gui.clear-inventory-on-open", false)) {
                // Chỉ trả đồ khi thực sự đóng GUI (không phải click nâng điểm)
                plugin.getGuiManager().restorePlayerInventory(player);
            }
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (plugin.getConfig().getBoolean("gui.clear-inventory-on-open", false)) {
            plugin.getGuiManager().restorePlayerInventory(e.getPlayer());
        }
    }

    private void handleUpgrade(Player player, String attrId, int amount) {
        boolean isSkillAPI = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SkillAPI");
        boolean isMMOCore = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MMOCore");

        int currentAP = 0;

        // Lấy điểm từ nguồn khả dụng
        if (isSkillAPI) {
            currentAP = org.ThienDev.Utils.SkillAPIWrapper.getPoints(player);
        } else if (isMMOCore) {
            currentAP = org.ThienDev.Utils.MMOCoreWrapper.getPoints(player);
        }

        if (currentAP <= 0) {
            player.sendMessage("§c[!] Bạn không còn điểm tiềm năng (AP) để nâng cấp!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Kiểm tra Class
        if (isSkillAPI && !org.ThienDev.Utils.SkillAPIWrapper.hasClass(player)) {
            player.sendMessage("§c[!] Bạn phải chọn Class (SkillAPI) trước!");
            return;
        } else if (isMMOCore && !org.ThienDev.Utils.MMOCoreWrapper.hasClass(player)) {
            player.sendMessage("§c[!] Bạn phải chọn Class (MMOCore) trước!");
            return;
        }

        int finalAmount = Math.min(amount, currentAP);

        // Trừ điểm dựa trên plugin
        if (isSkillAPI) {
            org.ThienDev.Utils.SkillAPIWrapper.givePoints(player, -finalAmount);
        } else if (isMMOCore) {
            org.ThienDev.Utils.MMOCoreWrapper.givePoints(player, -finalAmount);
        }

        // Cập nhật Database
        plugin.getDatabaseManager().addAttributeLevel(player.getUniqueId(), attrId, finalAmount);
        org.ThienDev.Api.AttributeAPI.invalidateCache(player.getUniqueId());

        // Task xử lý giao diện
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                org.ThienNguyen.Listener.CacheListener.refreshCache(player);
            } catch (Exception ignored) {}

            plugin.getGuiManager().setReOpening(player.getUniqueId(), true);
            plugin.getGuiManager().openMainGui(player, false);
            plugin.getGuiManager().setReOpening(player.getUniqueId(), false);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }, 1L);
    }
}