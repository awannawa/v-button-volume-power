# T11: Opsional: wake helper dan tombol tambahan

Status: TODO (hanya dikerjakan jika diminta)
Dependensi: T10

## Tujuan

- Mengurangi ketergantungan pada tombol fisik saat layar mati dan menambah tombol yang bisa dikustomisasi.

## Lingkup

- Dikerjakan: wake helper berbasis charger, tombol tambahan, pengaturan daftar tombol.
- Tidak dikerjakan: pemantauan sensor terus menerus (boros baterai).

## Langkah implementasi

- Wake helper: dari service, daftarkan receiver secara dinamis untuk `ACTION_POWER_CONNECTED` (receiver manifest untuk broadcast ini tidak diandalkan di Android modern). Saat diterima, ambil wake lock `SCREEN_BRIGHT_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP` dengan timeout beberapa detik. Wajib bisa dimatikan di pengaturan, dan perlu diuji karena perilakunya bergantung pada perangkat.
- Tombol tambahan: Mute (`ADJUST_TOGGLE_MUTE`), Screenshot (`GLOBAL_ACTION_TAKE_SCREENSHOT`), Home, Back, Recents (`GLOBAL_ACTION_HOME`, `BACK`, `RECENTS`), Notifications, Quick Settings.
- Pengaturan: pilih tombol mana yang tampil di panel dan urutannya (maksimal 5 agar panel tetap pendek).
- Opsi sisi: handle di kanan (default) atau kiri.

## File terkait

- `Actions.kt`, `overlay/PanelView.kt`, `SettingsActivity.kt`, `Prefs.kt`

## Acceptance criteria

- Wake helper menyalakan layar saat charger dicolok di Vivo T1 Pro, dan bisa dimatikan.
- Daftar tombol di panel mengikuti pengaturan dan tetap responsif.
- Target resource PRD tetap terpenuhi.

## Verifikasi

- Uji colok charger saat layar mati.
- Ukur ulang RAM dan baterai seperti di T09.

## Catatan

- (isi oleh worker)
