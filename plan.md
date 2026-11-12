# MASTER PLAN: ZenLauncher — launcher Android tối giản

## 1. Tổng quan

| Hạng mục | Chi tiết |
|---|---|
| Tên | ZenLauncher (`com.zen.launcher`) |
| Mục tiêu | Giao diện điện thoại đơn giản, ít xao nhãng, nhẹ trên máy yếu |
| Đối tượng | Máy RAM 3–4GB, người muốn giảm dùng điện thoại |
| Nền tảng | minSdk 26 (Android 8.0), compileSdk/targetSdk 36 |
| Ngôn ngữ / UI | Kotlin 100%, XML + ViewBinding |
| Phát hành | **Google Play** → không dùng AccessibilityService |
| Dung lượng | < 5MB |

## 2. Module

### Module 1: Launcher (đợt 1 — đang làm)
- Màn Home: đồng hồ + ô tìm kiếm + danh sách app **chỉ text** (không load icon)
- Lấy app bằng `LauncherApps.getActivityList()` (đủ app nhiều activity, Work profile)
- Cập nhật khi cài/gỡ app bằng `LauncherApps.Callback` (đăng ký lúc runtime)
- Tìm kiếm: gõ để lọc, Enter mở app đầu tiên
- Ẩn app: nhấn giữ → Ẩn; màn Cài đặt để hiện lại
- Đặt làm Home: intent-filter `HOME` + nút mở màn chọn Home mặc định
- Lưu trạng thái ẩn bằng SharedPreferences (không Room)

### Module 2: Thông báo & DND (đợt 2)
- `NotificationListenerService`: `cancelNotification` cho app không nằm trong whitelist
- DND priority mode qua `NotificationManager.setInterruptionFilter` (quyền `ACCESS_NOTIFICATION_POLICY`) — thay cho "chỉ nhận cuộc gọi"

### Module 3: Hiển thị (đợt 3)
- Grayscale: `Settings.Secure` `accessibility_display_daltonizer_enabled=1`, `accessibility_display_daltonizer=0`
- Tắt animation: `Settings.Global` `window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` = 0
- Cả hai cần `WRITE_SECURE_SETTINGS`, user cấp 1 lần: `adb shell pm grant com.zen.launcher android.permission.WRITE_SECURE_SETTINGS`
- App hiện màn hướng dẫn lệnh này khi chưa có quyền

### Module 4: Focus Mode (đợt 4)
- Lịch khóa app theo giờ
- Khi mở app từ ZenLauncher trong giờ khóa → chặn ngay tại launcher (không cần quyền)
- Mở app bằng đường khác: `UsageStatsManager` (quyền `PACKAGE_USAGE_STATS`) + màn chặn qua `SYSTEM_ALERT_WINDOW`; chỉ kiểm tra khi màn hình bật
- Chốt cơ chế chi tiết khi bắt đầu đợt 4

### Đã bỏ (và lý do)
| Tính năng | Lý do |
|---|---|
| Dọn RAM | `trimMemory`/`getMyMemoryState` chỉ tác động app mình; Play coi "RAM cleaner" là gây hiểu lầm |
| Chặn auto-start | Không có API công khai, phải Shizuku + appops |
| Shizuku | `pm grant WRITE_SECURE_SETTINGS` đủ cho grayscale + animation |
| Room cache list app | `LauncherApps` là nguồn dữ liệu đúng; cache gây lệch |
| BootReceiver | Hệ thống tự mở Home và tự bind NotificationListener |
| Accessibility (Recent/Grayscale/chặn app) | Phát hành Play; grayscale không làm được bằng Accessibility |
| Widget "One-tap Optimize", Paging, tầng domain | Không cần cho ~200 app |

## 3. Tech stack

- AGP 8.x + Kotlin, Gradle wrapper 8.14
- `androidx.core:core-ktx`, `androidx.appcompat`, `androidx.recyclerview`, `androidx.activity` (OnBackPressedCallback)
- Không Material, không Room, không coroutine library ngoài — list ~200 app dùng thread nền đơn giản
- Test: JUnit 4 cho logic thuần (lọc/sắp xếp/ẩn)
- Release: `isMinifyEnabled = true`, `isShrinkResources = true`

## 4. Cấu trúc (đợt 1)

```
app/src/main/java/com/zen/launcher/
├── AppEntry.kt          // data class + hàm lọc/sắp xếp thuần (có unit test)
├── AppRepository.kt     // LauncherApps → List<AppEntry>, callback cài/gỡ
├── HiddenApps.kt        // SharedPreferences set các app ẩn
├── HomeActivity.kt      // màn Home
├── AppListAdapter.kt
└── SettingsActivity.kt  // hiện lại app ẩn, nút đặt Home mặc định
app/src/test/.../AppEntryTest.kt
```

## 5. Lộ trình

| Đợt | Nội dung | Thời gian |
|---|---|---|
| 1 | Launcher (Module 1) | 2 tuần |
| 2 | Thông báo + DND | 1 tuần |
| 3 | Grayscale + animation | 1 tuần |
| 4 | Focus Mode | 2 tuần |
| 5 | Test máy thật, LeakCanary (debug), Privacy Policy, Play listing | 1 tuần |
| — | Dự phòng | 1 tuần |

## 6. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Play từ chối `QUERY_ALL_PACKAGES` | Không dùng; `LauncherApps` không cần quyền này |
| Play review quyền `PACKAGE_USAGE_STATS` / overlay (đợt 4) | Giải thích rõ trong listing, chỉ xin khi user bật Focus |
| User không chạy được lệnh adb (đợt 3) | Tính năng tùy chọn, app vẫn dùng bình thường |
| Máy cũ lag khi load list | Không load icon, load ở thread nền |

## 7. Checklist đợt 1
- [x] Build `assembleDebug` thành công, unit test pass
- [ ] Hiển thị list app dạng text, bấm mở được
- [ ] Tìm kiếm lọc đúng, Enter mở app đầu
- [ ] Nhấn giữ → ẩn; Cài đặt → hiện lại
- [ ] Cài/gỡ app → list tự cập nhật
- [ ] Đặt làm Home, Back trên Home không làm gì
