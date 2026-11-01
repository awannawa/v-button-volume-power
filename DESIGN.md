# DESIGN.md: virtual-buttons

The locked design system for the overlay and the settings screen. Its purpose is to keep the UI deliberate and free of generic AI-generated styling ("AI slop"). Place it at the project root, next to `AGENTS.md`.

## How to use this file

- Every visual value in code (color, radius, spacing, duration, text size) must come from this file or from `docs/virtual-buttons/01-ui-spec.md`. Never improvise a value mid-implementation.
- If a value you need is missing, stop and ask the orchestrator. The orchestrator updates this file first, then you continue.
- This file wins over any skill or generic default when they disagree.

## 1. Design intent

- This is a utility, not a brand. It should feel like part of the operating system.
- Invisible until needed: a quiet handle, a compact panel, nothing that asks for attention.
- One consistent visual language: one neutral palette, one icon style, one corner radius family, one motion curve.
- Function over decoration. If an element does not help the user press a button faster, remove it.

## 2. Locked tokens

Color

- Handle fill: `#FF202124`
- Handle border: 1dp `#66FFFFFF`
- Panel fill: `#CC202124` (about 80 percent opaque, semi-transparent)
- Panel border: 1dp `#33FFFFFF`
- Icon: `#FFFFFFFF`, applied through `imageTintList`
- Pressed highlight: `#33FFFFFF` circle behind the icon
- No other colors in the overlay and no accent color.
- Reason for dark fill with a light border: it stays visible on both white and black app backgrounds.

Shape

- Handle: fully rounded ends.
- Panel: 16dp corner radius.
- Pressed highlight: circle.
- No other radii and no mixed radii.

Spacing

- 4dp base grid. Panel padding 6dp vertical and 4dp horizontal, gap 4dp, screen-edge margin 8dp.
- Sizes and presets are defined in `docs/virtual-buttons/01-ui-spec.md`.

Elevation and effects

- None. No shadow, blur, glow, gradient, or glass effect.

Typography (settings screen only, the overlay has no text)

- Typeface: the platform default (Roboto). No bundled fonts.
- Sizes: screen title 20sp, section title 14sp medium, body 16sp, helper text 14sp.
- Sentence case. No all caps, no emoji, no exclamation marks.

Motion

- Open and close: `translationX`, 150 ms, `DecelerateInterpolator`.
- Press: the highlight appears instantly and fades out in 100 ms.
- Drag state: handle `scaleX` 1.5 and `alpha` 1.0 while dragging, back to normal on release in 100 ms.
- No idle or looping animation, no bounce or overshoot.

Icons

- Stroke icons from `assets/icons/`: 24 grid, stroke 2, round caps and joins, always the same weight.
- No emoji, no mix of filled and outlined icons, no icons from other sets.

## 3. Settings screen rules

- Platform default theme with day and night support. No custom palette beyond the tokens above.
- A plain vertical list with one row per setting. No card grids, no hero header, no illustrations.
- Platform widgets only (Switch, SeekBar, Button). Do not redraw or restyle them.
- Copy is literal and short: say what the setting does. No marketing tone and no invented claims.
- Onboarding is plain numbered steps with short text. No carousel, mascot, or progress theatrics.
- Service status is plain text (Active or Inactive). No decorative badge or pulsing dot.

## 4. Anti-slop ban list

- No gradients (especially purple to blue), glassmorphism, blur, neon glow, or colored shadows.
- No accent color that is not in the tokens, and no default framework palette left in place.
- No emoji or Unicode symbols used as icons.
- No decorative elements: sparkles, stripes, patterns, divider lines between panel buttons.
- No text labels, badges, counters, or tooltips on the overlay.
- No invented content: fake stats, taglines, or friendly filler copy such as "Welcome!" or "Let's get started".
- No re-drawn system chrome: no custom fake switches or sliders, and no custom volume slider. The volume indicator is the system one (`FLAG_SHOW_UI`).
- No mid-implementation token improvisation. A value that is not in this file or `01-ui-spec.md` is not allowed.
- No more than the buttons defined by the PRD (T11 may add more only through its settings).
- No attention-grabbing motion and no animation while idle.
- No mixed corner radii, inconsistent paddings, or one-off sizes.
- No dark patterns: no nagging dialogs, upsell screens, or rating prompts.

## 5. Design check (run before marking a UI task done)

Answer each item yes or no. Every answer must be yes.

- Every color, radius, spacing, duration, and text size in the diff exists in section 2 or in `01-ui-spec.md`.
- No gradient, shadow, blur, glow, or glass effect was added.
- No emoji or Unicode symbol is used as an icon.
- The overlay has no text, badge, counter, or tooltip.
- The handle and panel are clearly readable on a white app and on a black app (checked with screenshots).
- Nothing animates while idle.
- Every overlay button is at least 48dp and has a `contentDescription`.
- The settings screen uses platform widgets in a plain list, with no custom-drawn controls.
- All user-visible copy is literal, short, sentence case, and free of invented claims.
- A Hallmark-style audit of the changed UI files has no remaining violations that apply to native Android.
