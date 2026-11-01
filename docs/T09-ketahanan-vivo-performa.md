# T09: Ketahanan di Vivo dan performa

Status: TODO
Dependensi: T08

## Tujuan

- Service tetap hidup lama di Funtouch OS dan memenuhi target resource di PRD.

## Lingkup

- Dikerjakan: panduan battery dan autostart, deteksi service mati, profiling, optimasi.
- Tidak dikerjakan: fitur baru.

## Langkah implementasi

- Tambahkan bagian panduan di SettingsActivity: aktifkan Autostart, set Battery ke tanpa pembatasan, kunci app di Recent apps. Sediakan tombol yang membuka `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS` atau halaman app info sebagai langkah awal (jangan mengasumsikan intent khusus Vivo yang bisa tidak ada).
- Tampilkan peringatan di SettingsActivity jika service terdeteksi tidak aktif padahal pernah aktif.
- Audit kode: tidak ada timer, polling, receiver, atau thread yang berjalan saat idle. Semua Handler callback dihapus saat tidak dipakai.
- Ukur RAM: `adb shell dumpsys meminfo <package>`, target di bawah 40 MB PSS saat idle.
- Ukur baterai: `adb shell dumpsys batterystats` setelah beberapa jam pemakaian normal.
- Pastikan tidak ada alokasi berulang di jalur `ACTION_MOVE` (hindari objek baru per event).
- Uji bertahan setelah reboot, setelah mode hemat daya aktif, dan setelah beberapa jam layar mati.

## File terkait

- `SettingsActivity.kt`, `VButtonService.kt`, seluruh overlay

## Acceptance criteria

- Service tetap aktif setelah minimal 24 jam pemakaian normal di Vivo T1 Pro setelah panduan diikuti.
- RAM idle di bawah target.
- Tidak ada konsumsi baterai yang mencolok.

## Verifikasi

- Catat hasil `dumpsys meminfo` dan `batterystats` di bagian Catatan.

## Catatan

- (isi oleh worker)
