# T04: Gesture expand dan collapse

Status: TODO
Dependensi: T03

## Tujuan

- Handle bisa dibuka dengan tap singkat atau swipe ke kiri, dan panel menutup otomatis.

## Lingkup

- Dikerjakan: tap, fling ke kiri, auto-collapse, tap di luar panel.
- Tidak dikerjakan: long press dan drag (T05).

## Langkah implementasi

- Buat `GestureController` yang membungkus `GestureDetector` untuk handle.
- `onSingleTapUp` memanggil `setExpanded(true)`.
- `onFling` dengan `velocityX < 0` dan perpindahan horizontal yang cukup memanggil `setExpanded(true)`.
- Saat expanded jalankan `Handler.postDelayed(collapse, timeout)` dengan timeout dari `Prefs` (default 3000 ms). Setiap sentuhan di panel mereset timer.
- Hapus callback saat collapsed dan saat view dilepas (hindari leak).
- Tap di luar panel: pakai `FLAG_WATCH_OUTSIDE_TOUCH` dan tangani `ACTION_OUTSIDE` untuk collapse.
- Pastikan sentuhan di handle tidak diteruskan sebagai klik ke app di bawahnya.

## File terkait

- `overlay/GestureController.kt`, `overlay/OverlayView.kt`

## Acceptance criteria

- Tap singkat dan swipe kiri sama-sama membuka panel.
- Panel menutup otomatis setelah timeout tanpa interaksi.
- Tap di luar panel menutup panel.
- Tidak ada callback tertinggal setelah service dimatikan.

## Verifikasi

- Uji manual 20 kali tap dan 20 kali swipe, semua berhasil.
- Matikan service saat panel terbuka, tidak ada crash di logcat.

## Catatan

- (isi oleh worker)
