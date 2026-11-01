# T08: Pengaturan dan onboarding

Status: TODO
Dependensi: T07

## Tujuan

- Layar pengaturan dan panduan aktivasi yang jelas, dengan perubahan yang langsung berlaku di overlay.

## Lingkup

- Dikerjakan: SettingsActivity, status service, pengaturan, onboarding, live update.
- Tidak dikerjakan: panduan baterai Vivo yang detail (T09).

## Langkah implementasi

- Layar status: tampilkan apakah service aktif. Deteksi lewat `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` atau `VButtonService.instance != null`.
- Tombol "Aktifkan" membuka `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- Onboarding Restricted Settings: teks langkah (App info, menu titik tiga, Allow restricted settings) yang tampil jika service tidak bisa diaktifkan.
- Pengaturan: ukuran (S, M, L), opasitas handle, timeout auto-collapse, haptic on/off, reset posisi.
- Semua nilai disimpan di `Prefs`. Service mendaftarkan `OnSharedPreferenceChangeListener` saat connect dan melepasnya saat destroy, lalu menerapkan perubahan ke overlay.
- UI dibuat dengan view sederhana (LinearLayout, Switch, SeekBar), tanpa library tambahan.

## File terkait

- `SettingsActivity.kt`, `Prefs.kt`, `VButtonService.kt`, `res/layout/activity_settings.xml`

## Acceptance criteria

- Status service tampil benar saat aktif dan tidak aktif.
- Mengubah pengaturan langsung terlihat di overlay tanpa restart.
- Reset posisi mengembalikan handle ke default.
- Onboarding membantu pengguna mengaktifkan service dari nol.

## Verifikasi

- Uji dari kondisi instal bersih sampai service aktif.
- Ubah tiap pengaturan saat overlay tampil.

## Catatan

- (isi oleh worker)
