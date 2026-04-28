package org.ThienDev.Database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    // Sử dụng Map để lưu trữ: Key là ID thuộc tính (ví dụ: "strength"), Value là cấp độ (Level)
    private final Map<String, Integer> attributes;

    // Constructor cho người chơi mới
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.attributes = new HashMap<>();
    }

    // Constructor dùng khi load dữ liệu cũ từ file data.yml vào object
    public PlayerData(UUID uuid, Map<String, Integer> initialAttributes) {
        this.uuid = uuid;
        this.attributes = new HashMap<>(initialAttributes);
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Lấy cấp độ hiện tại của một thuộc tính.
     * @param id ID của thuộc tính (ví dụ: 'vitality')
     * @return Cấp độ hiện tại, trả về 0 nếu chưa từng nâng.
     */
    public int getAttributeLevel(String id) {
        return attributes.getOrDefault(id, 0);
    }

    /**
     * Tăng cấp độ cho thuộc tính.
     * @param id ID của thuộc tính
     * @param amount Số cấp độ muốn cộng thêm (thường là 1)
     */
    public void addAttributeLevel(String id, int amount) {
        int currentLevel = getAttributeLevel(id);
        attributes.put(id, currentLevel + amount);
    }

    /**
     * Đặt trực tiếp cấp độ cho thuộc tính (dùng cho lệnh set hoặc debug).
     */
    public void setAttributeLevel(String id, int level) {
        attributes.put(id, level);
    }

    /**
     * Trả về bản sao của Map thuộc tính để bảo vệ dữ liệu gốc (Encapsulation).
     */
    public Map<String, Integer> getAttributes() {
        return new HashMap<>(attributes);
    }
}