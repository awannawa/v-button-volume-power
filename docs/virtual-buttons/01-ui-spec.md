# UI Spec: Icons, Visuals, and Responsive Behavior

This document complements section 9 of `00-main-task.md`. If the two differ, this document wins.

Visual tokens (colors, borders, radii, motion, state visuals) live in `DESIGN.md` and win over this document for anything about look and feel.

## 1. Icons

- Icon source: SVG in `assets/icons/svg/` (viewBox 24x24, stroke 2, round cap and round join, no fill).
- Android does not read SVG directly. Use the equivalent VectorDrawables in `assets/icons/android/` and copy them to `app/src/main/res/drawable/`.
- If you modify an SVG, convert it in Android Studio via New, Vector Asset, Local file (SVG), then confirm the result matches the files in `assets/icons/android/`.
- Icon files are always white (`#FFFFFFFF`). The final color is applied through `imageTintList` so it can be customized from settings.
- v1 icons: `ic_volume_up` (speaker and plus sign), `ic_volume_down` (speaker and minus sign), `ic_power`.
- Extra icons for T11 (mute, screenshot, home, back, recents) follow the same style: 24 grid, stroke 2, 2 padding, round cap.
- Buttons have no text. `contentDescription` is mandatory on every button.

## 2. Visual tokens

- Colors, borders, corner radii, motion, and state visuals are defined only in `DESIGN.md`. Do not copy token values into code comments or other docs.
- Handle geometry: 5dp visible width, 72dp height, 24dp touch width, and `alpha` follows the setting (default 0.6, range 0.3 to 1.0).

## 3. Size presets (units in dp)

- Preset S: button 48, icon 22, panel height 164, panel width 56.
- Preset M (default): button 52, icon 24, panel height 176, panel width 60.
- Preset L: button 56, icon 26, panel height 188, panel width 64.
- Panel height formula: 3 x button + 2 x gap 4 + 2 x vertical padding 6.
- Panel width formula: button + 2 x horizontal padding 4.
- Every panel height stays under 200dp so it fits within the `systemGestureExclusionRects` limit.
- Buttons are at least 48dp (per NFR-8).
- Window width while collapsed: 24dp. While expanded: the panel width of the active preset.

## 4. Responsive rules

- Write every dimension in dp. Compute px from the density at runtime and never cache px across configuration changes.
- Use `Gravity.RIGHT or Gravity.TOP`, not `END`, so the overlay does not jump to the left when the system language is RTL.
- Safe area: get `systemBars` and `displayCutout` insets through `WindowMetrics` (API 30 and above). Fallback for API 28 and 29: `DisplayMetrics` and the `status_bar_height` resource.
- Valid Y range: from `insetTop + 8dp` to `screenHeight - insetBottom - panelHeight - 8dp`.
- Store the position as a ratio `yRatio` (0.0 to 1.0) of the valid Y range, not as px. Restore with `y = minY + yRatio * (maxY - minY)`. This keeps the position sensible after rotation, a display size change, or a move to another device.
- Compact mode: if the valid Y range is smaller than the panel height (for example, short landscape screens), step down one preset at a time until it fits, down to preset S at minimum.
- X offset: set `lp.x` equal to the right inset (a navigation bar on the right side in landscape with 3-button navigation, or a cutout) so the overlay does not cover system areas.
- Large display size or zoom: because everything uses dp, sizes scale up with it. The compact mode rule still applies.
- Font scale has no effect because there is no text.
- Touch thresholds use `ViewConfiguration` (`scaledTouchSlop`, `scaledMinimumFlingVelocity`), not fixed px values.
- Recompute everything above in `onConfigurationChanged` and whenever a setting changes.
- The window is not focusable and is not resized by the keyboard, so it does not jump when the keyboard appears.

## 5. Visual test checklist

- Portrait and landscape, with the handle at the top, middle, and bottom.
- All three size presets (S, M, L).
- Light and dark app backgrounds (the handle and panel must stay clearly visible).
- Gesture navigation and 3-button navigation (in landscape, the overlay must not cover the navigation bar).
- Small and large display size in system settings.
- An RTL language (for example Arabic) to confirm the handle stays on the right.
- Fullscreen apps (video and games) and the lock screen.
