# PRD dan Main Task: Virtual Button (Volume dan Power)

## 1. Ringkasan

- Nama kerja: `vbutton`
- Jenis: app Android native (Kotlin), berjalan sebagai AccessibilityService dengan overlay di tepi kanan layar.
- Fungsi: pengganti tombol fisik Volume Up, Volume Down, dan Power yang rusak.
- Perangkat utama: Vivo T1 Pro (Android 12, Funtouch OS), harus tetap kompatibel dengan perangkat Android lain milik pengguna.
- Prinsip: tanpa iklan, tanpa internet, tanpa tracking, irit resource, bisa dikustomisasi penuh.

## 2. Latar belakang dan masalah

- Tombol fisik volume dan power pada HP pengguna rusak sehingga HP sulit dipakai.
- App virtual button yang ada di Play Store penuh iklan, tidak bisa dikustomisasi, atau tidak stabil di perangkat pengguna.
- Pengguna ingin app sendiri yang sesuai dengan gaya pakainya dan bekerja baik di perangkatnya.

## 3. Tujuan

- G1: Volume naik, turun, dan akses power bisa dilakukan penuh lewat layar.
- G2: UI hampir tidak terlihat saat tidak dipakai (hanya handle tipis di tepi kanan).
- G3: Berjalan terus di background tanpa menguras baterai dan RAM.
- G4: Responsif di portrait, landscape, dan berbagai ukuran layar.
- G5: Seluruh perilaku penting bisa diatur pengguna (posisi, ukuran, opasitas, timeout, aksi tombol).

## 4. Non-goals (v1)

- Tidak menyalakan layar dari kondisi mati (tidak bisa lewat accessibility service, lihat bagian Risiko dan T11).
- Tidak ada fitur cloud, akun, analytics, atau iklan.
- Tidak dirilis ke Play Store (distribusi lewat APK pribadi).
- Tidak memakai Jetpack Compose atau library UI pihak ketiga.
- Tidak ada root, tidak ada Shizuku.

## 5. Target teknis

- Bahasa: Kotlin.
- minSdk 28 (karena `GLOBAL_ACTION_LOCK_SCREEN` dan `TAKE_SCREENSHOT` butuh API 28).
- targetSdk dan compileSdk: versi stabil terbaru di Android Studio yang dipakai.
- UI: View biasa dan Canvas, tanpa Compose, tanpa AppCompat jika memungkinkan.
- Dependency: maksimal `androidx.core:core-ktx`. Lebih baik nol dependency.
- Izin manifest: tidak ada `INTERNET`. Hanya `VIBRATE` (opsional untuk haptic) dan izin bawaan service accessibility.

## 6. Skenario pengguna

- S1: Pengguna menyentuh handle tipis di kanan, panel tombol muncul, pengguna menekan Volume +, panel menutup sendiri setelah beberapa detik.
- S2: Pengguna menahan handle, handle bisa digeser naik turun ke posisi yang nyaman, posisi tersimpan.
- S3: Pengguna menekan Power sekali, layar terkunci. Menahan Power membuka menu power (matikan dan restart).
- S4: Pengguna memutar HP ke landscape, handle tetap menempel di kanan dan posisinya tetap di dalam layar.
- S5: Pengguna menahan Volume + atau Volume -, volume berubah berulang sampai tombol dilepas.

## 7. Requirement fungsional

- FR-1: Service accessibility menampilkan overlay yang selalu ada di semua app setelah diaktifkan.
- FR-2: State collapsed: hanya handle tipis menempel di tepi kanan.
- FR-3: State expanded: panel vertikal menempel di tepi kanan berisi Volume +, Volume -, dan Power, muncul di posisi vertikal handle.
- FR-4: Expand dipicu oleh tap singkat pada handle atau swipe ke kiri pada handle.
- FR-5: Collapse otomatis setelah N detik tanpa interaksi (default 3 detik, bisa diatur) atau saat tap di luar panel.
- FR-6: Long press pada handle masuk mode drag, handle hanya bergeser vertikal (sumbu Y), posisi disimpan saat dilepas.
- FR-7: Volume +/-: ubah volume stream yang sedang aktif dan tampilkan indikator volume sistem. Tahan untuk mengulang.
- FR-8: Power tap: kunci layar. Power long press: tampilkan menu power sistem.
- FR-9: Feedback haptic singkat saat tombol ditekan (bisa dimatikan).
- FR-10: Layar pengaturan: status service, ukuran, opasitas, timeout auto-collapse, haptic, reset posisi.
- FR-11: Perubahan pengaturan langsung diterapkan ke overlay tanpa restart service.
- FR-12: Onboarding yang mengarahkan pengguna mengaktifkan service dan mengizinkan restricted settings jika diperlukan.

## 8. Requirement non-fungsional

- NFR-1 (CPU): service tidak menerima event accessibility apa pun dan tidak ada polling atau timer berjalan saat idle.
- NFR-2 (RAM): target di bawah 40 MB PSS saat idle (ukur dengan `dumpsys meminfo`).
- NFR-3 (Baterai): tidak muncul sebagai konsumen baterai signifikan di statistik baterai.
- NFR-4 (Latensi): dari sentuhan ke aksi (volume berubah) di bawah 100 ms.
- NFR-5 (Ukuran): APK release di bawah 1 MB jika memungkinkan (R8 dan shrinkResources aktif).
- NFR-6 (Ketahanan): service bertahan setelah reboot, rotasi, dan sesi pemakaian berjam-jam di Funtouch OS.
- NFR-7 (Privasi): tidak membaca isi layar. `canRetrieveWindowContent=false`.
- NFR-8 (Aksesibilitas): area sentuh minimal 48dp untuk tombol di panel, `contentDescription` terisi.
- NFR-9 (Responsif): tampilan sesuai `01-ui-spec.md` di portrait, landscape, tiga preset ukuran, berbagai display size, dan bahasa RTL.

## 9. Spesifikasi UI/UX

- Collapsed: bar tipis terlihat 5dp lebar dan 72dp tinggi, sudut membulat, opasitas default 60 persen (bisa diatur). Area sentuh 24dp lebar supaya mudah dikenai.
- Expanded: card vertikal semi-transparan, lebar 56 sampai 64dp, berisi 3 tombol 48 sampai 56dp tersusun vertikal (Vol +, Vol -, Power) sesuai preset ukuran, menempel di tepi kanan di posisi Y handle.
- Gaya: minimalis, ikon vector monokrom, tanpa teks, sudut membulat, tanpa bayangan berat. Token visual, preset ukuran, dan aturan responsif lengkap ada di `01-ui-spec.md`. File ikon ada di `assets/icons/`.
- Animasi: slide dari kanan (`translationX`) sekitar 150 ms, tanpa animasi berat atau blur.
- Mode drag: handle sedikit membesar dan opasitas naik sebagai tanda aktif, haptic singkat saat masuk mode drag.
- Posisi Y disimpan sebagai rasio dari area aman (bukan px) lalu di-clamp agar panel tidak keluar dari layar dan tidak menutupi status bar atau navigation bar.

## 10. Arsitektur singkat

- `VButtonService` (AccessibilityService): memegang WindowManager, menambah dan menghapus overlay, menerima aksi global.
- `OverlayView` (FrameLayout): berisi `HandleView` dan `PanelView`, mengelola state collapsed atau expanded.
- `GestureController`: GestureDetector untuk tap, fling kiri, long press, dan drag.
- `Actions`: pembungkus `AudioManager` dan `performGlobalAction`.
- `Prefs`: SharedPreferences untuk posisi dan pengaturan, dengan listener agar overlay bisa live update.
- `SettingsActivity`: UI pengaturan dan onboarding.
- Window: `TYPE_ACCESSIBILITY_OVERLAY`, flag `FLAG_NOT_FOCUSABLE`, gravity `RIGHT or TOP` (bukan `END`, supaya tidak pindah ke kiri di bahasa RTL), lebar sempit saat collapsed.

## 11. Struktur project yang disarankan

- `app/src/main/java/.../VButtonService.kt`
- `app/src/main/java/.../overlay/OverlayView.kt`
- `app/src/main/java/.../overlay/GestureController.kt`
- `app/src/main/java/.../Actions.kt`
- `app/src/main/java/.../Prefs.kt`
- `app/src/main/java/.../SettingsActivity.kt`
- `app/src/main/res/xml/accessibility_service_config.xml`
- `app/src/main/res/drawable/` (ikon vector)

## 12. Risiko dan mitigasi

- R1: Gesture Back di tepi kanan bentrok dengan swipe ke kiri. Mitigasi: `systemGestureExclusionRects` pada overlay (batas sekitar 200dp tinggi per sisi), area sentuh handle dibuat 24dp.
- R2: Funtouch OS mematikan service di background. Mitigasi: panduan Autostart, battery unrestricted, kunci di Recents, deteksi service mati di SettingsActivity.
- R3: Android 13 ke atas memblokir accessibility untuk APK sideload (Restricted Settings). Mitigasi: onboarding menjelaskan App info, menu titik tiga, lalu Allow restricted settings.
- R4: Layar mati tidak bisa dinyalakan oleh overlay. Mitigasi: double-tap to wake bawaan Vivo, colok charger, fingerprint, atau wake helper opsional di T11.
- R5: Overlay menghalangi sentuhan di app lain. Mitigasi: window collapsed hanya selebar area sentuh handle, tidak pernah fullscreen.
- R6: Perilaku `GLOBAL_ACTION_POWER_DIALOG` dan lock screen bisa beda di Vivo. Mitigasi: uji langsung di perangkat, sediakan fallback atau pengaturan aksi.
- R7: Bug service membuat HP sulit dipakai. Mitigasi: area overlay kecil, service bisa dimatikan lewat Settings, Accessibility.

## 13. Acceptance criteria global

- AC-1: Setelah service diaktifkan, handle muncul di tepi kanan di semua app dan tetap ada setelah reboot.
- AC-2: Tap atau swipe kiri membuka panel, auto-collapse bekerja sesuai timeout.
- AC-3: Long press lalu geser memindahkan handle secara vertikal dan posisinya tersimpan setelah service restart.
- AC-4: Vol +/- mengubah volume dan menampilkan indikator sistem, tahan untuk mengulang.
- AC-5: Power tap mengunci layar, Power long press membuka menu power.
- AC-6: Rotasi ke landscape tidak membuat handle atau panel keluar layar.
- AC-7: Tidak ada izin `INTERNET` di manifest final.
- AC-8: Memenuhi target NFR-1 sampai NFR-5 yang terukur.

## 14. Daftar child task

- T01: Setup project dan skeleton
- T02: AccessibilityService dan window overlay
- T03: UI handle dan panel
- T04: Gesture expand dan collapse
- T05: Drag dan penyimpanan posisi
- T06: Aksi volume dan power
- T07: Responsif dan gesture exclusion
- T08: Pengaturan dan onboarding
- T09: Ketahanan di Vivo dan performa
- T10: QA dan rilis APK
- T11: Opsional: wake helper dan tombol tambahan

Detail ada di folder `tasks/`. Panduan worker ada di `README.md`. Spesifikasi ikon dan responsif ada di `01-ui-spec.md`.

## 15. Keputusan terbuka

- (Selesai) Ikon tombol: SVG dan VectorDrawable sendiri di `assets/icons/`.
- Perlu tombol Mute di panel v1 atau cukup di T11.
- Package name final dan nama app.
- Perlu mode kiri (handle di sisi kiri) sebagai opsi pengaturan atau tetap kanan saja.
