# T11: Optional: wake helper and extra buttons

Status: TODO (only worked on when requested)
Dependencies: T10
Required reading: `DESIGN.md`, `docs/virtual-buttons/01-ui-spec.md`
Required skills: andrej-karpathy-skills, hallmark (see `AGENTS.md`)

## Goal

- Reduce reliance on the physical button when the screen is off, and add buttons that can be customized.

## Scope

- In scope: a charger-based wake helper, extra buttons, a button list setting.
- Out of scope: continuous sensor monitoring (it drains the battery).

## Implementation steps

- Wake helper: from the service, register a receiver dynamically for `ACTION_POWER_CONNECTED` (do not rely on a manifest receiver for this broadcast on modern Android). When it fires, acquire a wake lock with `SCREEN_BRIGHT_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP` and a timeout of a few seconds. It must be possible to turn it off in settings, and it must be tested because the behavior depends on the device.
- Extra buttons: Mute (`ADJUST_TOGGLE_MUTE`), Screenshot (`GLOBAL_ACTION_TAKE_SCREENSHOT`), Home, Back, Recents (`GLOBAL_ACTION_HOME`, `BACK`, `RECENTS`), Notifications, Quick Settings.
- Add the new buttons by adding `ButtonSpec` items. Do not modify `PanelView` internals.
- Draw the new icons in the same style as `01-ui-spec.md` section 1.
- Settings: choose which buttons appear in the panel and in what order (at most 5 so the panel stays short). Keep the panel height under the gesture exclusion limit.
- Side option: handle on the right (default) or on the left.

## Related files

- `Actions.kt`, `overlay/PanelView.kt`, `SettingsActivity.kt`, `Prefs.kt`

## Acceptance criteria

- The wake helper turns the screen on when a charger is plugged in on a Vivo T1 Pro, and can be turned off.
- The button list in the panel follows the settings and stays responsive.
- The PRD resource targets are still met.

## Verification

- Run the design check in `DESIGN.md` section 5 and record the result under Notes.
- Test plugging in a charger while the screen is off.
- Re-measure RAM and battery as in T09.

## Notes

- (to be filled in by the worker)
