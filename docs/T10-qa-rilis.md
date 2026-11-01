# T10: QA dan rilis APK

Status: TODO
Dependensi: T09

## Tujuan

- APK release yang sudah teruji, bertanda tangan, dan siap dipakai harian.

## Lingkup

- Dikerjakan: pengujian menyeluruh, perbaikan bug, signing, build release, dokumentasi singkat.
- Tidak dikerjakan: fitur baru.

## Langkah implementasi

- Jalankan seluruh Acceptance criteria global di `00-main-task.md` (AC-1 sampai AC-8) dan centang satu per satu di Catatan.
- Uji di dalam app yang sering dipakai: browser, chat, video fullscreen, game, kamera.
- Uji di layar kunci, saat panggilan masuk, saat keyboard terbuka.
- Buat keystore pribadi (simpan di luar repo) dan konfigurasi signing release.
- Build `assembleRelease`, cek ukuran APK dan izin manifest.
- Tulis `docs/virtual-buttons/INSTALL.md` singkat: cara instal, cara mengaktifkan service, cara mengatasi restricted settings, cara menonaktifkan jika bermasalah.

## File terkait

- `build.gradle.kts`, `INSTALL.md`

## Acceptance criteria

- AC-1 sampai AC-8 semuanya lulus.
- APK release terinstal bersih dan berfungsi.
- Tidak ada izin `INTERNET`.

## Verifikasi

- Instal APK release di perangkat bersih dan jalankan checklist.

## Catatan

- (isi oleh worker)
