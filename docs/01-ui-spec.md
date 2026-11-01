# UI Spec: Ikon, Visual, dan Responsifitas

Dokumen ini melengkapi `00-main-task.md` bagian 9. Jika ada selisih, dokumen ini yang dipakai.

## 1. Ikon

- Sumber ikon: SVG di `assets/icons/svg/` (viewBox 24x24, stroke 2, round cap dan round join, tanpa fill).
- Android tidak membaca SVG langsung. Pakai VectorDrawable yang setara di `assets/icons/android/`, salin ke `app/src/main/res/drawable/`.
- Jika mengubah SVG, konversi lewat Android Studio: New, Vector Asset, Local file (SVG), lalu cek hasilnya sama dengan file di `assets/icons/android/`.
- Warna di file ikon selalu putih (`#FFFFFFFF`). Warna akhir diatur lewat `imageTintList` agar bisa dikustomisasi dari pengaturan.
- Daftar ikon v1: `ic_volume_up` (speaker dan tanda plus), `ic_volume_down` (speaker dan tanda minus), `ic_power`.
- Ikon tambahan untuk T11 (mute, screenshot, home, back, recents) dibuat dengan gaya yang sama: grid 24, stroke 2, padding 2, round cap.
- Tidak ada teks di tombol. `contentDescription` wajib diisi untuk setiap tombol.

## 2. Token visual

- Handle: fill `#FF202124`, border 1dp `#66FFFFFF`, sudut membulat penuh, lebar terlihat 5dp, tinggi 72dp, `alpha` mengikuti pengaturan (default 0.6, rentang 0.3 sampai 1.0).
- Alasan handle gelap dengan border terang: tetap terlihat di app berlatar putih maupun hitam.
- Panel: fill `#CC202124` (sekitar 80 persen), border 1dp `#33FFFFFF`, radius sudut 16dp, tanpa elevation atau bayangan.
- Ikon: putih, ukuran sesuai preset.
- Tombol ditekan: lingkaran highlight `#33FFFFFF` (ripple sederhana atau state pressed), tanpa animasi panjang.
- Mode drag: handle `scaleX` 1.5 dan `alpha` 1.0 selama drag.
- Animasi buka dan tutup: `translationX` 150 ms dengan `DecelerateInterpolator`.

## 3. Preset ukuran (satuan dp)

- Preset S: tombol 48, ikon 22, tinggi panel 164, lebar panel 56.
- Preset M (default): tombol 52, ikon 24, tinggi panel 176, lebar panel 60.
- Preset L: tombol 56, ikon 26, tinggi panel 188, lebar panel 64.
- Rumus tinggi panel: 3 x tombol + 2 x gap 4 + 2 x padding vertikal 6.
- Rumus lebar panel: tombol + 2 x padding horizontal 4.
- Semua tinggi panel di bawah 200dp supaya muat dalam batas `systemGestureExclusionRects`.
- Tombol minimal 48dp (sesuai NFR-8).
- Lebar window saat collapsed: 24dp. Saat expanded: lebar panel sesuai preset.

## 4. Aturan responsif

- Semua dimensi ditulis dalam dp. Nilai px dihitung dari density saat runtime dan tidak di-cache melewati perubahan konfigurasi.
- Gunakan `Gravity.RIGHT or Gravity.TOP`, bukan `END`, agar overlay tidak pindah ke kiri saat bahasa sistem RTL.
- Area aman: ambil insets `systemBars` dan `displayCutout` lewat `WindowMetrics` (API 30 ke atas). Fallback untuk API 28 dan 29: `DisplayMetrics` dan resource `status_bar_height`.
- Rentang Y valid: dari `insetTop + 8dp` sampai `tinggiLayar - insetBottom - tinggiPanel - 8dp`.
- Simpan posisi sebagai rasio `yRatio` (0.0 sampai 1.0) dari rentang Y valid, bukan px. Muat dengan `y = minY + yRatio * (maxY - minY)`. Dengan begitu posisi tetap masuk akal setelah rotasi, ganti display size, atau pindah perangkat.
- Mode kompak: jika rentang Y valid lebih kecil dari tinggi panel (misalnya landscape), turunkan preset satu tingkat sampai muat, minimal preset S.
- Offset X: set `lp.x` sama dengan inset kanan (navigation bar di sisi kanan saat landscape mode 3 tombol, atau cutout) agar overlay tidak menimpa area sistem.
- Display size atau zoom besar: karena memakai dp, ukuran ikut membesar. Aturan mode kompak tetap berlaku.
- Font scale tidak berpengaruh karena tidak ada teks.
- Ambang sentuh memakai `ViewConfiguration` (`scaledTouchSlop`, `scaledMinimumFlingVelocity`), bukan angka px tetap.
- Hitung ulang semua hal di atas saat `onConfigurationChanged` dan saat pengaturan berubah.
- Window tidak fokus dan tidak ikut di-resize keyboard, sehingga tidak melompat saat keyboard muncul.

## 5. Checklist uji tampilan

- Portrait dan landscape, handle di posisi atas, tengah, dan bawah.
- Tiga preset ukuran (S, M, L).
- Background app terang dan gelap (handle dan panel tetap terlihat jelas).
- Navigasi gesture dan navigasi 3 tombol (di landscape, overlay tidak menimpa navigation bar).
- Display size kecil dan besar di pengaturan sistem.
- Bahasa RTL (misalnya Arab) untuk memastikan handle tetap di kanan.
- App fullscreen (video dan game) dan layar kunci.
