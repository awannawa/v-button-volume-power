# T01: Setup project dan skeleton

Status: TODO
Dependensi: tidak ada

## Tujuan

- Project Android Kotlin yang bisa build dan terinstal, siap menjadi dasar task berikutnya.

## Lingkup

- Dikerjakan: struktur project, Gradle, manifest, resource dasar, konfigurasi release kecil.
- Tidak dikerjakan: service, overlay, atau logika apa pun.

## Langkah implementasi

- Buat project Empty Views Activity (Kotlin), tentukan package dan nama app (lihat Keputusan terbuka di PRD).
- Atur `minSdk 28`, `targetSdk` dan `compileSdk` ke versi stabil terbaru.
- Hapus dependency yang tidak perlu (AppCompat, Material, ConstraintLayout) jika tidak dipakai. Sisakan paling banyak `core-ktx`.
- Aktifkan R8 pada release: `isMinifyEnabled = true`, `isShrinkResources = true`.
- Pastikan manifest tidak memiliki izin `INTERNET`.
- Tambahkan `SettingsActivity` kosong sebagai launcher.
- Siapkan ikon launcher sederhana (adaptive icon vector).
- Buat `Prefs.kt` kosong (object dengan nama file SharedPreferences) sebagai placeholder.

## File terkait

- `build.gradle.kts` (app dan root), `AndroidManifest.xml`, `SettingsActivity.kt`, `Prefs.kt`

## Acceptance criteria

- Build debug dan release lulus.
- App terinstal dan membuka layar kosong tanpa crash.
- Manifest tidak berisi `INTERNET`.

## Verifikasi

- `./gradlew assembleDebug assembleRelease`
- `adb install -r ...` lalu buka app.
- `aapt dump permissions` pada APK, pastikan tidak ada `INTERNET`.

## Catatan

- (isi oleh worker)
