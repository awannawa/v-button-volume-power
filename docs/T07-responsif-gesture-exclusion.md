# T07: Responsif dan gesture exclusion

Status: TODO
Dependensi: T05

## Tujuan

- Overlay stabil di semua orientasi dan tidak bentrok dengan gesture sistem.

## Lingkup

- Dikerjakan: rotasi, cutout, system bars, gesture exclusion, multi-window dasar.
- Tidak dikerjakan: perangkat lipat atau tablet khusus.

## Langkah implementasi

- Terapkan seluruh Aturan responsif di `01-ui-spec.md` bagian 4: insets, posisi berbasis rasio, mode kompak, `Gravity.RIGHT`, dan offset X dari inset kanan.
- Override `onConfigurationChanged` di service, hitung ulang ukuran layar (pakai `WindowMetrics` di API 30 ke atas, fallback `DisplayMetrics`) lalu clamp ulang `lp.y` dan update layout.
- Pada `OverlayView`, set `systemGestureExclusionRects` di `onLayout` dengan rect seluruh view (perhatikan batas sekitar 200dp tinggi per sisi, jaga tinggi handle dan panel di bawah batas).
- Atur `layoutInDisplayCutoutMode` agar overlay tidak terpotong cutout di landscape.
- Hindari tumpang tindih dengan status bar dan navigation bar (gunakan insets).
- Uji dengan navigasi gesture dan navigasi 3 tombol.
- Pastikan handle tidak muncul di atas keyboard dengan cara yang menutupi tombol penting (cukup uji, perbaiki hanya jika bermasalah).

## File terkait

- `VButtonService.kt`, `overlay/OverlayView.kt`

## Acceptance criteria

- Rotasi portrait dan landscape tidak membuat overlay keluar layar atau terpotong.
- Swipe kiri dari handle tidak memicu gesture Back saat navigasi gesture aktif.
- Panel dan handle tetap bisa disentuh di semua orientasi.
- Ketiga preset ukuran (S, M, L) tampil benar. Pada layar pendek, mode kompak aktif dan panel tetap muat.
- Dengan bahasa sistem RTL, handle tetap di sisi kanan.
- Di landscape mode 3 tombol, overlay tidak menimpa navigation bar.

## Verifikasi

- Putar HP berkali-kali dalam kondisi handle di posisi atas, tengah, dan bawah.
- Uji dengan kedua mode navigasi.
- Jalankan Checklist uji tampilan di `01-ui-spec.md` bagian 5.

## Catatan

- (isi oleh worker)
