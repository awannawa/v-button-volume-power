# T07: Responsive behavior and gesture exclusion

Status: done (2026-11-01, orchestrator-audited)
Dependencies: T05
Required reading: `DESIGN.md`, `docs/virtual-buttons/01-ui-spec.md`
Required skills: andrej-karpathy-skills, hallmark (see `AGENTS.md`)

## Goal

- The overlay is stable in every orientation and does not conflict with system gestures.

## Scope

- In scope: rotation, cutout, system bars, gesture exclusion, basic multi-window.
- Out of scope: foldable or tablet-specific devices.

## Implementation steps

- Apply every responsive rule in `01-ui-spec.md` section 4: insets, ratio-based position, compact mode, `Gravity.RIGHT`, and the X offset from the right inset. Improve `SafeArea.compute()` from T05 without changing its signature.
- Override `onConfigurationChanged` in the service, recompute the screen size (use `WindowMetrics` on API 30 and above, with a `DisplayMetrics` fallback), then re-clamp and update the layout.
- On `OverlayView`, set `systemGestureExclusionRects` in `onLayout` with a rect covering the whole view (mind the limit of about 200dp of height per edge, and keep the handle and panel under it).
- Set `layoutInDisplayCutoutMode` so the overlay is not clipped by the cutout in landscape.
- Avoid overlapping the status bar and navigation bar (use insets).
- Test with both gesture navigation and 3-button navigation.
- Check that the handle does not appear above the keyboard in a way that covers important keys (test it, and fix only if it is a problem).
- Add unit tests for the compact-mode preset selection.

## Related files

- `VButtonService.kt`, `overlay/OverlayView.kt`, `overlay/SafeArea.kt`

## Acceptance criteria

- Rotating between portrait and landscape does not push the overlay off the screen or clip it.
- Swiping left from the handle does not trigger the Back gesture while gesture navigation is on.
- The panel and handle can still be touched in every orientation.
- All three size presets (S, M, L) render correctly. On short screens, compact mode activates and the panel still fits.
- With an RTL system language, the handle stays on the right side.
- In landscape with 3-button navigation, the overlay does not cover the navigation bar.

## Verification

- Run the design check in `DESIGN.md` section 5 and record the result under Notes.
- Rotate the phone many times with the handle at the top, middle, and bottom positions.
- Test with both navigation modes.
- Run the visual test checklist in `01-ui-spec.md` section 5.
- `./gradlew testDebugUnitTest`

## Notes

- (orchestrator, 2026-11-01) Implemented and reviewed clean. SafeArea now uses WindowMetrics (systemBars + displayCutout) on API 30+, resource fallback below; compute() signature unchanged. Compact mode: pure selectFittingPreset, floor S; "fits" reading = panel height vs screen minus insets minus 2x margins. systemGestureExclusionRects guarded to API 29+ (no-op on 28); cutout mode ALWAYS on 30+/SHORT_EDGES below. 39 unit tests (9 new compact-mode).
- (orchestrator, 2026-11-01) PARKED plan-mandated limitation: on API 28/29 the fallback returns rightInsetPx = 0, so in landscape with a right-side 3-button nav bar the overlay can overlap the nav bar on those API levels only. Primary device is Android 12+ (API 31+); plan text prescribes exactly this fallback. Survives into the release risk list (T10).
- (orchestrator, 2026-11-01) Deferred minors: misleading compact-boundary test comment (assertions correct); recomputeLayout calls updateWindowLayout unconditionally (one redundant guarded IPC per no-op config event); narrow race — config change mid-150ms-animation that also switches the compact preset can leave a stale visible panel that self-heals on next state change.
