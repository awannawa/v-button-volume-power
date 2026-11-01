# T05: Drag and position persistence

Status: TODO
Dependencies: T04, T06
Required reading: `DESIGN.md`, `docs/virtual-buttons/01-ui-spec.md`
Required skills: andrej-karpathy-skills, hallmark (see `AGENTS.md`)

## Goal

- The handle can be moved vertically after a long press and its position is saved.

## Scope

- In scope: long press, Y-axis drag, clamping, saving and loading the position.
- Out of scope: rotation and cutout adjustments (T07).

## Implementation steps

- In `GestureController`, `onLongPress` enters drag mode: a short haptic, and the handle grows slightly with higher opacity (see the drag state in `DESIGN.md`).
- On `ACTION_MOVE` while in drag mode, change `lp.y` from `rawY` minus the initial offset and call `updateViewLayout` (allocate no new objects per event).
- Lock the X axis (no horizontal movement).
- Clamp `lp.y` to the valid Y range from `01-ui-spec.md` section 4 (top and bottom insets, panel height, 8dp margin). Put this in a single function `SafeArea.compute()`. T07 will improve the same function, so keep its signature stable.
- On `ACTION_UP` or `ACTION_CANCEL`: leave drag mode, compute `yRatio = (y - minY) / (maxY - minY)`, and save it to `Prefs` (not a px value).
- When the service connects: load `yRatio` from `Prefs` (default 0.4), convert it to `y` using the current range, then clamp.
- While the panel is expanded, dragging still moves the whole panel.
- Put the clamping and ratio conversion in pure functions with JVM unit tests.

## Related files

- `overlay/GestureController.kt`, `overlay/SafeArea.kt`, `Prefs.kt`, `VButtonService.kt`

## Acceptance criteria

- Hold the handle and drag it, and the handle follows the finger vertically without visible lag.
- The handle cannot leave the safe area of the screen.
- The position persists after the service is turned off and on, and after a reboot.
- Unit tests for the clamping and ratio conversion pass.

## Verification

- Run the design check in `DESIGN.md` section 5 and record the result under Notes.
- Drag to the maximum up and down positions and check the clamping.
- Restart the service and reboot, and confirm the position is the same.
- `./gradlew testDebugUnitTest`

## Notes

- (to be filled in by the worker)
