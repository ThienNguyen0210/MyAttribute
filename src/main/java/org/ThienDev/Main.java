package org.ThienDev;

import org.ThienDev.Commands.AttributeCommand;
import org.ThienDev.Commands.AttributeTabCompleter;
import org.ThienDev.Database.DatabaseManager;
import org.ThienDev.Listeners.GuiListener;
import org.ThienDev.Manager.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private GuiManager guiManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Gán instance ĐẦU TIÊN để các Manager khác gọi Main.getInstance() không bị null
        instance = this;

        // In Banner ra Console
        printBanner();

        // 1. Quản lý cấu hình
        saveDefaultConfig();

        // 2. Khởi tạo Database trước để các Manager khác có dữ liệu dùng
        this.databaseManager = new DatabaseManager(this);

        // 3. Khởi tạo GuiManager sau Database
        this.guiManager = new GuiManager(this);

        // 4. Register Commands và Events
        if (getCommand("MyAttribute") != null) {
            getCommand("MyAttribute").setExecutor(new AttributeCommand(this));
            getCommand("MyAttribute").setTabCompleter(new AttributeTabCompleter());
        }

        getServer().getPluginManager().registerEvents(new org.ThienDev.Listener.PermStatsListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getLogger().info("MyAttribute (Database Mode) đã khởi chạy thành công!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    /**
     * Hàm xử lý in banner ASCII Art ra Console hệ thống
     */
    private void printBanner() {
        String name = getDescription().getName();
        String version = getDescription().getVersion();
        String authors = String.join(", ", getDescription().getAuthors());

        getLogger().info("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
        getLogger().info("░  " + name + " v" + version);
        getLogger().info("░  Author: " + authors + " | Paper");
        getLogger().info("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
    }

    public static Main getInstance() {
        return instance;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}