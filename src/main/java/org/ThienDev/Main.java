package org.ThienDev;

import org.ThienDev.Commands.AttributeCommand;
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

        // 1. Quản lý cấu hình
        saveDefaultConfig();

        // 2. Khởi tạo Database trước để các Manager khác có dữ liệu dùng
        this.databaseManager = new DatabaseManager(this);

        // 3. Khởi tạo GuiManager sau Database
        this.guiManager = new GuiManager(this);

        // 4. Register Commands và Events
        if (getCommand("MyAttribute") != null) {
            getCommand("MyAttribute").setExecutor(new AttributeCommand(this));
        }
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("MyAttribute (Database Mode) đã khởi chạy thành công!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
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