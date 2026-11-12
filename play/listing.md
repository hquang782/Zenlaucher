# Hồ sơ Google Play – ZenLauncher

Tài nguyên trong thư mục này:

| File | Dùng cho | Yêu cầu của Play |
|---|---|---|
| `icon-512.png` | App icon | 512×512 PNG, Play tự bo góc |
| `feature-graphic.png` | Feature graphic | 1024×500 |
| `screenshot-1-home.png`, `screenshot-2-settings.png` | Ảnh chụp điện thoại | ≥ 2 ảnh, cạnh dài ≤ 2 lần cạnh ngắn (đã cắt 720×1440) |
| `icon.svg` | Nguồn của icon (Stitch, mẫu "Abstract Cairn") | — |

Chính sách quyền riêng tư: `docs/privacy.html`. Sau khi bật GitHub Pages (Settings › Pages › branch `main`, thư mục `/docs`), URL sẽ là
`https://hquang782.github.io/Zenlaucher/privacy.html`.

## Thông tin cửa hàng – Tiếng Việt (vi-VN)

**Tên ứng dụng** (≤ 30 ký tự): `ZenLauncher: Tối giản`

**Mô tả ngắn** (≤ 80 ký tự):
`Màn hình chính chỉ có chữ, ít xao nhãng. Chế độ Zen chặn thông báo không cần.`

**Mô tả đầy đủ:**

```
ZenLauncher là màn hình chính tối giản, giúp bạn dùng điện thoại có chủ đích hơn.

• Chỉ có chữ, không icon, không màu mè: bạn mở đúng app mình cần, không bị cuốn vào lướt vô định.
• Home gọn gàng: đồng hồ, ngày tháng và tối đa 5 app ghim. Nhấn giữ để ghim, chuyển lên/xuống.
• Vuốt lên để xem tất cả app. Tìm kiếm không cần dấu: gõ "dien thoai" vẫn ra "Điện thoại". Nhấn Enter để mở app đầu tiên.
• Ẩn app bạn muốn tránh; mở lại trong Cài đặt Zen.
• Hỗ trợ hồ sơ công việc (Work profile).

CHẾ ĐỘ ZEN
Một chạm trên Home để bật:
• Không làm phiền (chỉ ưu tiên) được bật tự động.
• Thông báo từ app không nằm trong danh sách cho phép bị gạt bỏ ngay.
• Cuộc gọi, báo thức và thông báo đang chạy (nhạc, chỉ đường) luôn được giữ.
• Home cho biết đã chặn bao nhiêu thông báo.
Tắt Chế độ Zen thì Không làm phiền trở về như trước.

NHẸ VÀ RIÊNG TƯ
• Dưới 2MB, chạy mượt trên máy yếu.
• Không quảng cáo, không theo dõi, không cần Internet. Không dữ liệu nào rời khỏi máy.
• Chế độ Zen cần quyền Truy cập thông báo, chỉ để biết app nào gửi thông báo. Nội dung thông báo không được đọc hay lưu lại.
```

## Store listing – English (en-US)

**App name**: `ZenLauncher: Minimal Launcher`

**Short description**:
`A text-only home screen with fewer distractions. Zen mode mutes the noise.`

**Full description:**

```
ZenLauncher is a minimalist home screen that helps you use your phone with intention.

• Text only — no icons, no colors pulling at your attention. Open what you came for, nothing more.
• A calm home: clock, date and up to 5 pinned apps. Long-press to pin, move up or move down.
• Swipe up for all apps. Search ignores accents, and Enter opens the first match.
• Hide apps you want to avoid; bring them back in Zen Settings.
• Work profile support.

ZEN MODE
One tap on the home screen:
• Do Not Disturb (priority only) turns on.
• Notifications from apps not on your allowed list are dismissed at once.
• Calls, alarms and ongoing notifications (music, navigation) always get through.
• Home shows how many notifications were blocked.
Turn Zen mode off and Do Not Disturb goes back to how it was.

LIGHT AND PRIVATE
• Under 2 MB, smooth on low-end phones.
• No ads, no tracking, no Internet needed. No data leaves your device.
• Zen mode needs Notification access only to see which app posted a notification. Notification content is never read or stored.
```

## Phân loại

- Danh mục: **Personalization** (Cá nhân hóa)
- Thẻ: Launcher, Digital wellbeing
- Email liên hệ: email nhà phát triển trong Play Console (bắt buộc)

## An toàn dữ liệu (Data safety)

| Câu hỏi | Trả lời |
|---|---|
| Ứng dụng có thu thập hoặc chia sẻ dữ liệu người dùng bắt buộc không? | **Không**. Mọi dữ liệu chỉ được xử lý trên máy, không truyền ra ngoài |
| Dữ liệu được mã hoá khi truyền? | Không áp dụng (không truyền dữ liệu) |
| Người dùng có thể yêu cầu xoá dữ liệu? | Không áp dụng. Gỡ app là xoá hết |

Theo định nghĩa của Play, "thu thập" là truyền dữ liệu ra khỏi thiết bị. Danh sách app và thông tin thông báo chỉ xử lý trên máy, nên không thuộc diện phải khai báo.

## Xếp hạng nội dung (IARC)

Trả lời "Không" cho mọi câu về bạo lực, nội dung tình dục, ngôn ngữ, chất kích thích, cờ bạc, tương tác người dùng, chia sẻ vị trí, mua hàng trong app. Kết quả dự kiến: 3+ / Everyone.

## Đối tượng mục tiêu

18+ (hoặc 13+). Không nhắm tới trẻ em, để tránh phải tuân theo chính sách Families.

## Quyền nhạy cảm

| Quyền | Cần khai báo? | Ghi chú |
|---|---|---|
| Truy cập thông báo (`NotificationListenerService`) | Không có form riêng | Do người dùng tự cấp trong Cài đặt Android; app có mô tả trong màn Cài đặt Zen và trong chính sách quyền riêng tư |
| `EXPAND_STATUS_BAR` | Không | Quyền thường |
| `QUERY_ALL_PACKAGES`, Accessibility | Không dùng | Danh sách app lấy qua `<queries>` LAUNCHER và `LauncherApps` |

## Ký ứng dụng

- Upload key: `~/keystores/zenlauncher-upload.jks` (alias `upload`, chứng chỉ `CN=ZenLauncher`). Mật khẩu nằm trong `keystore.properties` ở gốc dự án (đã gitignore).
- **Sao lưu cả hai file này ra nơi an toàn** (trình quản lý mật khẩu hoặc ổ ngoài). Nếu mất, phải xin Google đặt lại upload key.
- Bật **Play App Signing** khi tạo app (mặc định với AAB): Google giữ khoá ký thật, bạn chỉ giữ upload key.
- Build: `./gradlew bundleRelease` tạo ra `app/build/outputs/bundle/release/app-release.aab`.

## Checklist trước khi gửi xét duyệt

- [ ] Bật GitHub Pages cho `docs/` (repo phải public) và điền URL chính sách quyền riêng tư
- [ ] Tạo app trong Play Console với package `com.zen.launcher` (nếu bị trùng, phải đổi `applicationId` trước lần tải lên đầu tiên)
- [ ] Tải `app-release.aab` lên kênh Internal testing, cài thử từ Play
- [ ] Điền thông tin cửa hàng, An toàn dữ liệu, Xếp hạng nội dung, Đối tượng mục tiêu
- [ ] Tài khoản cá nhân tạo sau 11/2023: cần kiểm thử kín với ≥ 12 người trong 14 ngày trước khi phát hành chính thức
