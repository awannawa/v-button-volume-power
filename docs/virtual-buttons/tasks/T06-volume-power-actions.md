# T06: Volume and power actions

Status: TODO
Dependencies: T03

## Goal

- The panel buttons actually change the volume and control power.

## Scope

- In scope: `Actions`, volume auto-repeat, haptics, power tap and long press.
- Out of scope: extra buttons (T11).

## Implementation steps

- Create `Actions.kt` that takes a `Context` and the `AccessibilityService`. Keep its interface stable because T11 builds on it.
- Volume: `audioManager.adjustSuggestedStreamVolume(ADJUST_RAISE or ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)`.
- Auto-repeat: on `ACTION_DOWN`, run the action once, then repeat about every 150 ms after an initial delay of about 400 ms using a Handler. Stop on `ACTION_UP` or `ACTION_CANCEL`.
- Power tap: `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`.
- Power long press (about 500 ms): `performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)`.
- Check and log the boolean result of `performGlobalAction` with tag `VButton`.
- Haptics: `view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)`, honoring the haptics setting from `Prefs`.
- After a volume action, reset the auto-collapse timer (from T04) so the panel does not close while the user is still pressing.
- Wire the buttons through the `ButtonSpec` list created in T03.

## Related files

- `Actions.kt`, `overlay/PanelView.kt`

## Acceptance criteria

- Vol + and Vol - change the volume and show the system volume indicator.
- Holding a volume button repeats the change until it is released.
- Power tap locks the screen. Power long press opens the power menu.
- No handler keeps running after a button is released.

## Verification

- Test while music is playing and in ringer mode, and confirm the changed volume matches the active stream.
- Test on the lock screen and inside a fullscreen app (video, game).
- Note down any behavior on Funtouch OS that differs from what is expected.

## Notes

- (to be filled in by the worker)
