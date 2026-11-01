# Virtual Buttons (vbutton)

Aplikasi Android pribadi yang menggantikan tombol fisik **Volume +/−** dan **Power**
yang rusak, dengan tombol virtual berupa overlay tipis di tepi kanan layar.
Dibuat khusus untuk **Vivo T1 Pro** (Android 12, Funtouch OS) dan kompatibel dengan
perangkat Android lain milik pemilik (Android 9 / API 28 ke atas).

Aplikasi berjalan sebagai `AccessibilityService` yang menampilkan overlay di atas
semua aplikasi. Layar dikunci saat ditekan, volume berubah dengan indikator sistem,
dan semuanya bisa diatur dari layar Settings bawaan aplikasi.

## Prinsip (tidak dapat dikompromikan)

- **Tanpa INTERNET permission** — diverifikasi langsung di APK (`aapt dump permissions` kosong).
- Tanpa iklan, tanpa analitik, tanpa pelacakan.
- **Tidak pernah membaca isi layar** (`canRetrieveWindowContent="false"`).
- Hemat resource: tidak ada timer, polling, receiver, maupun thread saat idle.
- Distribusi pribadi (APK sideload), tidak ke Play Store. Tanpa root, tanpa Shizuku.

## Fitur

- **Handle** tipis (5dp, area sentuh 24dp) di tepi kanan layar, selalu ada di semua aplikasi.
- **Tap atau swipe kiri** pada handle membuka panel berisi 3 tombol: Volume +, Volume −, Power.
- **Power**: tap = lock screen, tahan ±0,5 detik = menu power sistem.
- **Tahan Volume +/−**: volume terus berubah sampai dilepas (repeat otomatis).
- **Long-press handle** lalu geser: pindahkan posisi vertikal; posisi tersimpan
  (sebagai rasio layar, jadi aman setelah rotasi atau ganti perangkat).
- Panel menutup otomatis setelah 3 detik tanpa interaksi (bisa diatur 1–10 detik),
  atau saat menyentuh di luar panel.
- **Settings**: ukuran tombol (S/M/L), opasitas handle, timeout auto-collapse, haptic
  on/off, reset posisi — semua berlaku langsung tanpa restart service.
- Panduan enable + jalur "Allow restricted settings" (Android 13+ untuk APK sideload),
  plus panduan keep-alive khusus Funtouch OS (Autostart, baterai unrestricted, lock di
  Recents) dan peringatan jika service terdeteksi mati.

## Spesifikasi teknis

| | |
|---|---|
| Bahasa | Kotlin, UI pakai View/Canvas biasa (tanpa Compose, tanpa library UI) |
| minSdk / targetSdk / compileSdk | 28 / 35 / 35 |
| Build | Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21 |
| Dependensi runtime | **Nol** (junit hanya untuk test) |
| Ukuran APK release | ±62 KB (R8 + shrinkResources aktif) |
| Package / nama | `com.awannawa.vbutton` / "Virtual Buttons" |
| Unit test | 42 (JVM murni: clamp posisi, konversi rasio, preset compact, fling, repeat policy) |

## Struktur project

```
AGENTS.md                      hukum proses & maintainability (untuk worker/agent)
DESIGN.md                      design system terkunci (token warna, bentuk, motion)
assets/icons/                  ikon sumber: svg/ dan android/ (VectorDrawable)
docs/virtual-buttons/
  00-main-task.md              PRD: kebutuhan, NFR, risiko, acceptance criteria
  01-ui-spec.md                spesifikasi ikon, ukuran preset S/M/L, aturan responsive
  README.md                    panduan worker + status task + catatan handover
  tasks/T01..T11               spesifikasi tiap task (T11 opsional, belum dikerjakan)
  INSTALL.md                   cara pasang & enable di HP
  DEVICE-QA.md                 checklist QA manual di device (14 langkah)
app/src/main/java/com/awannawa/vbutton/
  VButtonService.kt            AccessibilityService: pemilik window overlay
  Actions.kt                   satu-satunya gerbang ke AudioManager / performGlobalAction
  Prefs.kt                     satu-satunya pemilik key SharedPreferences
  SettingsActivity.kt          layar settings + onboarding + panduan Vivo
  overlay/                     OverlayView, HandleView, PanelView, GestureController,
                               SafeArea, ButtonSpec, Tokens (satu sumber token visual)
```

## Cara build

Environment build ini (WSL) tidak punya Java/Android SDK sistem — semuanya terpasang
di user space. **Selalu mulai dengan**:

```bash
source ~/tools/android-env.sh
```

(file itu menyiapkan JDK 17, Gradle 8.9, dan `ANDROID_HOME=~/Android/Sdk`).

```bash
# Debug APK
./gradlew assembleDebug          # hasil: app/build/outputs/apk/debug/app-debug.apk

# Release APK (signed)
./gradlew assembleRelease        # hasil: app/build/outputs/apk/release/app-release.apk

# Verifikasi
./gradlew testDebugUnitTest      # 42 unit test
./gradlew lintDebug              # lint, 0 error
aapt dump permissions app/build/outputs/apk/release/app-release.apk   # harus kosong
```

Catatan release signing: build release membaca keystore dari
`/home/awan/keystores/vbutton-signing.properties` (password di dalamnya, chmod 600;
keystore-nya `/home/awan/keystores/vbutton.jks`, alias `vbutton`). Di mesin lain tanpa
file itu, build release otomatis fallback ke debug signing. **Backup folder
`~/keystores/`** — kalau hilang, APK tidak bisa di-update tanpa uninstall dulu.

Build pertama memerlukan internet untuk mengunduh dependensi Gradle/AGP — tapi
**APK hasilnya sendiri tidak membutuhkan internet sama sekali**.

## Cara install & enable

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Atau salin APK-nya ke HP dan buka (sideload). Lalu:

1. Buka aplikasi → tap **Enable** → diarahkan ke Settings → Accessibility → Virtual Buttons → aktifkan.
2. **Android 13+**: jika toggle terblokir — App info → menu tiga titik → **Allow restricted settings** → aktifkan lagi.
3. **Vivo/Funtouch**: ikuti panduan di aplikasi (Autostart, baterai unrestricted, lock di Recents) agar service tidak dimatikan di background.

Detail lengkap: `docs/virtual-buttons/INSTALL.md`.

## Cara pakai

- Sentuh handle di tepi kanan → panel muncul → tekan tombolnya.
- Tap handle sekali atau swipe ke kiri juga membuka panel.
- Long-press handle sampai bergetar lalu geser vertikal untuk memindah posisi.
- Panel menutup sendiri sesuai timeout, atau tap di luar panel untuk menutup.

## Status & keterbatasan yang diketahui

- Layar yang **mati** tidak bisa dinyalakan oleh overlay (batasan platform); solusi:
  double-tap-to-wake bawaan Vivo, charger, atau fingerprint. Fitur wake helper saat
  charger dicolok tersedia sebagai **T11 (opsional, belum dikerjakan — tinggal minta)**.
- Setelah force-stop dari Settings, aplikasi tetap terbaca "Active" (service akan
  hidup lagi saat aplikasi dibuka); peringatan service-mati menangkap kasus
  dicabut/dimatikan Funtouch — pola yang paling umum.
- Di API 28/29 saja, overlay bisa menimpa navigation bar dalam mode landscape
  (batasan fallback inset; T1 Pro Android 12+ tidak terdampak).
- TalkBack tidak bisa mengaktifkan handle (tombol sentuh murni; panel tetap bisa).

## QA

- Otomatis: `./gradlew testDebugUnitTest lintDebug` (42 test, lint 0 error).
- Manual di device: ikuti `docs/virtual-buttons/DEVICE-QA.md` (14 langkah, mencakup
  acceptance criteria AC-1..AC-9 PRD, pengukuran RAM/baterai, dan uji ketahanan
  24 jam di Funtouch OS).
