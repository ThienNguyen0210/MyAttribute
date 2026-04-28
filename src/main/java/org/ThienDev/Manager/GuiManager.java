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

public class GuiManager {
    private final Main plugin;
    private FileConfiguration guiConfig;
    private File guiFile;

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
        String title = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("menu.title", "GUI"));
        int size = guiConfig.getInt("menu.size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Load items từ config (logic mở rộng)
        if (guiConfig.contains("menu.items")) {
            for (String key : guiConfig.getConfigurationSection("menu.items").getKeys(false)) {
                int slot = guiConfig.getInt("menu.items." + key + ".slot");
                Material material = Material.valueOf(guiConfig.getString("menu.items." + key + ".material").toUpperCase());
                String name = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("menu.items." + key + ".name"));

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);

                List<String> lore = new ArrayList<>();
                for (String line : guiConfig.getStringList("menu.items." + key + ".lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);

                inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
    }
    public String getAttributeIdAtSlot(int clickedSlot) {
        if (guiConfig.contains("menu.items")) {
            for (String key : guiConfig.getConfigurationSection("menu.items").getKeys(false)) {
                int slot = guiConfig.getInt("menu.items." + key + ".slot");
                if (slot == clickedSlot) {
                    return guiConfig.getString("menu.items." + key + ".attribute_id");
                }
            }
        }
        return null;
    }
    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }
}