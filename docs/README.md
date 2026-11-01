# README Worker: virtual-buttons

## Tujuan task

- Membangun app Android native (Kotlin) berisi tombol virtual Volume +, Volume -, dan Power sebagai overlay di tepi kanan layar.
- Target perangkat utama: Vivo T1 Pro. Detail lengkap ada di `00-main-task.md`.

## Wajib dibaca sebelum mulai

- Baca `00-main-task.md` sampai habis (PRD, batasan, risiko, acceptance criteria).
- Baca `01-ui-spec.md` (ikon, token visual, preset ukuran, aturan responsif). File ikon ada di `assets/icons/`.
- Baca file task yang akan dikerjakan di folder `tasks/`.
- Jika project punya `AGENTS.md` atau `SKILL.md`, baca juga dan ikuti aturannya. Path: isi sesuai project.

## Aturan umum

- Satu task dikerjakan sampai selesai dan terverifikasi sebelum pindah ke task berikutnya.
- Jangan menambah dependency di luar yang diizinkan PRD (maksimal `androidx.core:core-ktx`).
- Jangan menambah izin `INTERNET` atau library analytics atau iklan.
- Jangan memakai Compose atau library UI pihak ketiga.
- Jangan membuat timer, polling, atau receiver yang berjalan terus saat idle.
- Kode harus kecil, jelas, dan satu file satu tanggung jawab.
- Semua ukuran dalam dp, jangan hardcode px.
- Jangan mengubah scope task. Jika menemukan masalah di task lain, catat di bagian Catatan, jangan diperbaiki diam-diam.

## Urutan implementasi

- 1. T01 Setup project dan skeleton
- 2. T02 AccessibilityService dan window overlay
- 3. T03 UI handle dan panel
- 4. T04 Gesture expand dan collapse
- 5. T06 Aksi volume dan power
- 6. T05 Drag dan penyimpanan posisi
- 7. T07 Responsif dan gesture exclusion
- 8. T08 Pengaturan dan onboarding
- 9. T09 Ketahanan di Vivo dan performa
- 10. T10 QA dan rilis APK
- 11. T11 Opsional (hanya jika diminta)

Catatan urutan: T06 dikerjakan sebelum T05 agar fungsi inti (volume dan power) bisa dipakai lebih cepat.

## Status tracking

Ubah `[ ]` menjadi `[x]` hanya setelah verifikasi task lulus.

- [ ] T01 Setup project dan skeleton
- [ ] T02 AccessibilityService dan window overlay
- [ ] T03 UI handle dan panel
- [ ] T04 Gesture expand dan collapse
- [ ] T06 Aksi volume dan power
- [ ] T05 Drag dan penyimpanan posisi
- [ ] T07 Responsif dan gesture exclusion
- [ ] T08 Pengaturan dan onboarding
- [ ] T09 Ketahanan di Vivo dan performa
- [ ] T10 QA dan rilis APK
- [ ] T11 Opsional: wake helper dan tombol tambahan

## Verifikasi

- Setiap task punya bagian Acceptance criteria dan Verifikasi. Jalankan semuanya.
- Build harus lulus: `./gradlew assembleDebug`.
- Instal ke perangkat: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Lihat log: `adb logcat -s VButton`.
- Jika tidak bisa diuji di perangkat, tulis jelas bagian mana yang belum teruji di Catatan.

## Riwayat worker dan handover

Tambahkan satu entri per sesi kerja, yang terbaru di bawah.

Template entri:

- Tanggal:
- Worker:
- Task yang dikerjakan:
- Status: selesai / sebagian / terblokir
- Yang diubah (file):
- Yang belum selesai atau perlu dicek:
- Catatan untuk worker berikutnya:

Riwayat:

- (kosong)
