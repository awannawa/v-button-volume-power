# T02: AccessibilityService dan window overlay

Status: TODO
Dependensi: T01

## Tujuan

- Service accessibility aktif yang menampilkan satu view overlay di tepi kanan layar di atas semua app.

## Lingkup

- Dikerjakan: deklarasi service, config XML, pembuatan dan penghapusan window overlay, lifecycle.
- Tidak dikerjakan: tampilan final (T03), gesture (T04), aksi (T06).

## Langkah implementasi

- Buat `res/xml/accessibility_service_config.xml`. Jangan set atribut `accessibilityEventTypes` (default 0 artinya tanpa event). Set `canRetrieveWindowContent="false"`, `canPerformGestures="false"`, `accessibilityFeedbackType="feedbackGeneric"`, `description`.
- Deklarasikan service di manifest dengan `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, intent-filter `android.accessibilityservice.AccessibilityService`, dan meta-data ke config XML.
- Buat `VButtonService : AccessibilityService`. Kosongkan `onAccessibilityEvent` dan `onInterrupt`.
- Di `onServiceConnected`: ambil `WindowManager`, buat `LayoutParams` dengan `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE`, `PixelFormat.TRANSLUCENT`, gravity `END or TOP`, lebar 24dp, tinggi WRAP_CONTENT, lalu `addView` placeholder (kotak berwarna).
- Di `onDestroy` dan `onUnbind`: `removeView` dengan pengecekan aman (try-catch `IllegalArgumentException`).
- Simpan referensi service di object singleton ringan (`VButtonService.instance`) untuk dipakai SettingsActivity, set null saat destroy.

## File terkait

- `VButtonService.kt`, `accessibility_service_config.xml`, `AndroidManifest.xml`, `strings.xml`

## Acceptance criteria

- Service muncul di Settings, Accessibility dengan deskripsi.
- Setelah diaktifkan, placeholder tampil di tepi kanan di semua app.
- Menonaktifkan service menghapus placeholder tanpa crash.
- Service tidak menerima event accessibility apa pun.

## Verifikasi

- Aktifkan service, buka beberapa app dan pastikan placeholder tetap ada.
- `adb logcat` tidak menunjukkan crash saat on/off service.
- `adb shell dumpsys accessibility` menunjukkan service tanpa event types.

## Catatan

- (isi oleh worker)
