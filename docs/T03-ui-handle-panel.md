# T03: UI handle dan panel

Status: TODO
Dependensi: T02

## Tujuan

- Mengganti placeholder dengan `OverlayView` berisi handle tipis dan panel tombol vertikal, lengkap dengan dua state.

## Lingkup

- Dikerjakan: tampilan, ikon, state collapsed dan expanded, animasi, ukuran jendela.
- Tidak dikerjakan: gesture (T04), aksi tombol (T06), drag (T05).

## Langkah implementasi

- Baca `01-ui-spec.md`, lalu salin ikon dari `assets/icons/android/` ke `app/src/main/res/drawable/`.
- Buat `OverlayView : FrameLayout` berisi `HandleView` dan `PanelView`.
- `HandleView`: custom View yang menggambar bar 5dp x 72dp (fill, border, dan sudut sesuai token di `01-ui-spec.md`) di tepi kanan, `alpha` dari pengaturan (default 0.6), area sentuh 24dp lebar.
- `PanelView`: LinearLayout vertikal dengan 3 tombol (Vol +, Vol -, Power) berukuran sesuai preset S, M, atau L, background card sesuai token, ikon `ic_volume_up`, `ic_volume_down`, `ic_power` dengan warna lewat `imageTintList`.
- State: `setExpanded(Boolean)` mengubah visibilitas, ukuran `LayoutParams` (24dp saat collapsed, lebar panel sesuai preset saat expanded), dan memanggil `updateViewLayout`.
- Animasi `translationX` sekitar 150 ms dengan `ViewPropertyAnimator`, tanpa animator berkelanjutan.
- Tambahkan `contentDescription` pada tiap tombol.
- Semua ukuran lewat helper `dp()`.

## File terkait

- `overlay/OverlayView.kt`, `overlay/HandleView.kt`, `overlay/PanelView.kt`, `res/drawable/ic_*.xml`

## Acceptance criteria

- Collapsed hanya menampilkan bar tipis di tepi kanan.
- Expanded menampilkan 3 tombol vertikal menempel di kanan, di posisi Y yang sama dengan handle.
- Perpindahan state mulus dan tidak menutupi area lain selain lebar window.

## Verifikasi

- Panggil `setExpanded` dari tombol debug sementara, cek kedua state di portrait.
- Layout Inspector atau screenshot: lebar window sesuai spesifikasi.

## Catatan

- (isi oleh worker)
