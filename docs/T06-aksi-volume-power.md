# T06: Aksi volume dan power

Status: TODO
Dependensi: T03

## Tujuan

- Tombol di panel benar-benar mengubah volume dan mengontrol power.

## Lingkup

- Dikerjakan: `Actions`, auto-repeat volume, haptic, power tap dan long press.
- Tidak dikerjakan: tombol tambahan (T11).

## Langkah implementasi

- Buat `Actions.kt` yang menerima `Context` dan `AccessibilityService`.
- Volume: `audioManager.adjustSuggestedStreamVolume(ADJUST_RAISE atau ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)`.
- Auto-repeat: pada `ACTION_DOWN` jalankan aksi sekali, lalu repeat tiap sekitar 150 ms setelah jeda awal sekitar 400 ms dengan Handler. Hentikan pada `ACTION_UP` atau `ACTION_CANCEL`.
- Power tap: `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`.
- Power long press (sekitar 500 ms): `performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)`.
- Haptic: `view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)`, hormati pengaturan haptic dari `Prefs`.
- Setelah aksi volume, reset timer auto-collapse (agar panel tidak menutup saat pengguna masih menekan).

## File terkait

- `Actions.kt`, `overlay/PanelView.kt`

## Acceptance criteria

- Vol + dan Vol - mengubah volume dan menampilkan indikator volume sistem.
- Menahan tombol volume mengulang perubahan sampai dilepas.
- Power tap mengunci layar. Power long press membuka menu power.
- Tidak ada handler yang tertinggal berjalan setelah tombol dilepas.

## Verifikasi

- Uji saat memutar musik dan saat mode dering, volume yang berubah sesuai stream aktif.
- Uji di layar terkunci dan di dalam app fullscreen (video, game).
- Catat jika perilaku di Funtouch OS berbeda dari yang diharapkan.

## Catatan

- (isi oleh worker)
