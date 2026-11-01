# T04: Expand and collapse gestures

Status: TODO
Dependencies: T03

## Goal

- The handle can be opened with a short tap or a swipe to the left, and the panel closes automatically.

## Scope

- In scope: tap, left fling, auto-collapse, tap outside the panel.
- Out of scope: long press and drag (T05).

## Implementation steps

- Create a `GestureController` that wraps a `GestureDetector` for the handle.
- `onSingleTapUp` calls `setExpanded(true)`.
- `onFling` with `velocityX < 0` and enough horizontal travel calls `setExpanded(true)`.
- While expanded, run `Handler.postDelayed(collapse, timeout)` with the timeout from `Prefs` (default 3000 ms). Any touch on the panel resets the timer. Expose the reset as a function so T06 can call it.
- Remove the callback when collapsed and when the view is detached (avoid leaks).
- Tap outside the panel: use `FLAG_WATCH_OUTSIDE_TOUCH` and handle `ACTION_OUTSIDE` to collapse.
- Make sure touches on the handle are not passed on as clicks to the app underneath.
- Use `ViewConfiguration` for touch thresholds, not fixed px values.

## Related files

- `overlay/GestureController.kt`, `overlay/OverlayView.kt`

## Acceptance criteria

- A short tap and a left swipe both open the panel.
- The panel closes automatically after the timeout without interaction.
- A tap outside the panel closes it.
- No callbacks remain after the service is turned off.

## Verification

- Manually test 20 taps and 20 swipes, all of which must succeed.
- Turn off the service while the panel is open and confirm there is no crash in logcat.

## Notes

- (orchestrator, 2026-11-01) Lint ClickableViewAccessibility on HandleView is real a11y debt: TalkBack cannot activate the handle (no performClick). Deferred to T07/T08 — NFR-8 requires 48dp targets + contentDescription, not activation. The OverlayView instance of the same warning is a false positive (ACTION_OUTSIDE receiver, no click semantics).
- (orchestrator, 2026-11-01) T06 access: OverlayView.resetAutoCollapse() is called from within OverlayView when wiring ButtonSpec actions; VButtonService.overlayView stays private. Fling predicate requires horizontal dominance (abs(velocityX) > abs(velocityY)) per FR-4 intent; |vx|==|vy| stays closed (documented in GestureController).
