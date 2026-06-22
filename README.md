# 🧬 MyAttribute

**MyAttribute** là plugin Minecraft cho phép người chơi **đầu tư điểm tiềm năng (Attribute Points)** vào các thuộc tính RPG tùy chỉnh (sức mạnh, máu, chí mạng...) thông qua một GUI trực quan. Dữ liệu được lưu trữ bền vững bằng **SQLite**, và tích hợp trực tiếp với hệ thống class/level của **SkillAPI (Fabled)** hoặc **MMOCore**.

---

## ✨ Tính năng chính

- **GUI nâng cấp trực quan** — người chơi click vào item trong menu để cộng điểm vào thuộc tính, không cần lệnh phức tạp.
- **Lưu trữ bền vững bằng SQLite** — dữ liệu không mất khi restart server, không phụ thuộc file YAML dễ hỏng.
- **Tích hợp class-based** — hiển thị lore khác nhau theo từng class (Warrior, Archer...), tự động lấy điểm AP và kiểm tra class từ SkillAPI hoặc MMOCore.
- **Placeholder linh động trong GUI** — hỗ trợ cú pháp `{attr_level:id}` kèm phép tính `+ - * /`, cùng với PlaceholderAPI nếu có cài.
- **Hệ thống buff tạm thời qua lệnh** — admin có thể cộng stat tạm thời (flat hoặc %) cho người chơi trong khoảng thời gian nhất định, tự động hết hạn.
- **Tùy chọn "dọn túi đồ" khi mở GUI** — tránh người chơi vô tình kéo thả item lung tung, tự động trả lại nguyên trạng khi đóng GUI.

---

## 📋 Yêu cầu

- Server Spigot/Paper **1.16+**
- **Một trong hai** plugin sau (để cấp & trừ Attribute Points):
  - [SkillAPI (Fabled)](https://www.spigotmc.org/resources/fabled-skillapi.93822/), **hoặc**
  - [MMOCore](https://www.mmoitems.net/)
- (Tùy chọn) [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — để dùng thêm placeholder ngoài các placeholder có sẵn của plugin

> ⚠️ Nếu cả hai plugin trên đều không được cài, hệ thống AP sẽ luôn trả về 0 — người chơi sẽ không nâng cấp được gì.

---

## 📦 Cài đặt

1. Thả file `.jar` vào thư mục `plugins/`.
2. Khởi động lại server để plugin tự tạo:
   - `plugins/MyAttribute/config.yml`
   - `plugins/MyAttribute/GUI.yml`
   - `plugins/MyAttribute/Data.sqlite` (database lưu cấp độ thuộc tính)
3. Chỉnh sửa `config.yml` và `GUI.yml` theo nhu cầu server (xem hướng dẫn bên dưới).
4. Không cần lệnh reload — chỉ cần restart hoặc đảm bảo GUI được mở lại sau khi sửa `GUI.yml`.

---

## 🎮 Lệnh

| Lệnh | Quyền | Mô tả |
|---|---|---|
| `/MyAttribute` | — (mọi người chơi) | Mở GUI nâng cấp thuộc tính |
| `/MyAttribute stats <player> <stat> <value> <seconds>` | `myattribute.stats` | Cộng buff tạm thời cho người chơi, tự động hết sau `<seconds>` giây |

### Ví dụ lệnh buff tạm thời

```
/MyAttribute stats Steve strength 10 30      → +10 strength (flat) trong 30 giây
/MyAttribute stats Steve crit_chance 15% 60  → +15% crit_chance trong 60 giây
```

> Buff này hoàn toàn nằm trong RAM, **không lưu vào database** — mất khi hết thời gian hoặc khi server restart.

---

## ⚙️ Cấu hình GUI (`GUI.yml`)

```yaml
menu:
  title: "&8Chỉnh sửa thuộc tính {points}"
  size: 27
  items:
    strength_item:
      slot: 11
      material: IRON_SWORD
      attribute_id: "strength"
      name: "&cSức Mạnh"
      lore:
        - "&7Cấp độ hiện tại: &e{attr_level:strength}"
        - "&7Sát thương tăng thêm: &a+{attr_level:strength * 2}%"
        - "&7Điểm còn lại: &b{points}"
      class_lore:
        Warrior:
          - "&6[Warrior] &7Thuộc tính cốt lõi của bạn!"
          - "&7Cấp độ: &e{attr_level:strength}"
      model: 1001   # tùy chọn — Custom Model Data
```

### Placeholder hỗ trợ trong `name` / `lore`

| Placeholder | Ý nghĩa |
|---|---|
| `{points}` | Số điểm AP hiện có của người chơi |
| `{class}` / `{class_name}` | Tên class hiện tại (SkillAPI/MMOCore) |
| `{attr_level:<id>}` | Cấp độ hiện tại của thuộc tính `<id>` |
| `{attr_level:<id> + N}` | Cấp độ + N (cũng hỗ trợ `-`, `*`, `/`, nhận số thập phân) |
| `%placeholder_papi%` | Mọi placeholder từ PlaceholderAPI, nếu đã cài plugin này |

### `class_lore` (lore theo class)

Nếu muốn item hiển thị lore khác nhau tùy theo class người chơi, thêm khối `class_lore.<TênClass>` — tên class phải khớp chính xác (không phân biệt được hoa/thường tùy theo cách SkillAPI/MMOCore trả về tên). Nếu người chơi không thuộc class nào khớp, item sẽ dùng `lore` mặc định.

---

## ⚙️ Cấu hình chung (`config.yml`)

```yaml
gui:
  clear-inventory-on-open: false
  # true = tự động dọn sạch túi đồ chính (36 slot đầu) khi mở GUI lần đầu,
  # và trả lại nguyên trạng khi đóng GUI hoặc người chơi rời server.
  # Giáp và ô offhand KHÔNG bị ảnh hưởng.

attributes:
  strength:
    damage: 1.5              # giá trị base mỗi cấp độ cho stat "damage"
    class-bonus:
      warrior:
        damage: 0.5           # bonus thêm riêng cho class Warrior, cộng vào base mỗi cấp
  vitality:
    max-health: 1.0
```

Cách tính tổng bonus cho mỗi stat:
```
total = Σ [ (base + class_bonus_nếu_đúng_class) × cấp_độ_hiện_tại ]
```
(tính tổng qua tất cả attribute pack mà người chơi có cấp độ > 0)

---

## 🔌 Tích hợp & API cho dev khác

`AttributeAPI` cung cấp các hàm tĩnh để plugin khác (ví dụ hệ thống combat) truy vấn bonus của người chơi:

```java
double bonus = AttributeAPI.getBonus(uuid, "damage");          // bonus từ DB (attribute pack) + temp bonus RAM
double percentBonus = AttributeAPI.getPercentBonus(uuid, "crit_chance"); // % bonus tạm thời

AttributeAPI.invalidateCache(uuid); // gọi sau khi cập nhật cấp độ trong DB, để tính lại bonus
```

> ⚠️ `getBonus()` có cache nội bộ theo từng `(uuid, statName)` — luôn gọi `invalidateCache(uuid)` sau khi thay đổi cấp độ thuộc tính trong database, nếu không bonus cũ sẽ vẫn được dùng.

Plugin cũng gọi `org.ThienNguyen.Listener.CacheListener.refreshCache(player)` sau mỗi lần nâng cấp hoặc buff — đây là điểm tích hợp với **MyItem Core**, để đồng bộ cache combat ngay khi stat thay đổi.

---

## ❓ Câu hỏi thường gặp

**Vì sao điểm AP của tôi luôn là 0?**
Kiểm tra server có cài SkillAPI hoặc MMOCore không, và người chơi đã chọn class chưa — plugin yêu cầu phải có class mới nâng cấp được.

**Vì sao GUI hiện "Không có lớp"?**
Người chơi chưa chọn class trong SkillAPI/MMOCore, hoặc cả hai plugin đó chưa được cài/enable.

**Buff từ `/MyAttribute stats` có mất khi restart server không?**
Có. Buff tạm thời chỉ lưu trong RAM, không ghi vào database — đây là tính năng có chủ đích để dùng cho hiệu ứng tạm thời (potion, event...).

---

## 📜 Bản quyền

Tác giả: **Thiện Dev (ThienNguyen)**
_(điền giấy phép cụ thể cho dự án — ví dụ giữ credit khi chia sẻ, không phân phối lại có phí, v.v.)_
