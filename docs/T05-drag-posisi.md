# T05: Drag dan penyimpanan posisi

Status: TODO
Dependensi: T04, T06

## Tujuan

- Handle bisa digeser vertikal setelah long press dan posisinya tersimpan.

## Lingkup

- Dikerjakan: long press, drag sumbu Y, clamp, simpan dan muat posisi.
- Tidak dikerjakan: penyesuaian rotasi dan cutout (T07).

## Langkah implementasi

- Di `GestureController`, `onLongPress` masuk mode drag: haptic singkat, handle sedikit membesar dan opasitas naik.
- Saat `ACTION_MOVE` dalam mode drag, ubah `lp.y` sesuai `rawY` dikurangi offset awal, panggil `updateViewLayout` (batasi frekuensi, cukup tiap event move).
- Kunci sumbu X (tidak ada perpindahan horizontal).
- Clamp `lp.y` ke rentang Y valid sesuai `01-ui-spec.md` bagian 4 (insets atas dan bawah, tinggi panel, margin 8dp).
- Saat `ACTION_UP` atau `ACTION_CANCEL`: keluar mode drag, hitung `yRatio = (y - minY) / (maxY - minY)` lalu simpan ke `Prefs` (bukan nilai px).
- Saat service connect: muat `yRatio` dari `Prefs` (default 0.4), ubah ke `y` dengan rentang saat ini, lalu clamp.
- Saat panel expanded, drag tetap memindahkan seluruh panel.

## File terkait

- `overlay/GestureController.kt`, `Prefs.kt`, `VButtonService.kt`

## Acceptance criteria

- Tahan handle lalu geser, handle mengikuti jari secara vertikal tanpa lag terlihat.
- Handle tidak bisa keluar dari area layar yang aman.
- Posisi bertahan setelah service dimatikan dan dihidupkan, serta setelah reboot.

## Verifikasi

- Geser ke atas dan bawah maksimum, cek clamp.
- Restart service dan reboot, posisi sama.

## Catatan

- (isi oleh worker)
