# T09: Vivo resilience and performance

Status: TODO
Dependencies: T08

## Goal

- The service stays alive for a long time on Funtouch OS and meets the resource targets in the PRD.

## Scope

- In scope: battery and autostart guidance, dead-service detection, profiling, optimization.
- Out of scope: new features.

## Implementation steps

- Add a guide section in SettingsActivity: enable Autostart, set Battery to unrestricted, and lock the app in Recent apps. Provide a button that opens `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS` or the app info page as a first step (do not assume a Vivo-specific intent exists).
- Show a warning in SettingsActivity if the service is detected as inactive although it was active before.
- Audit the code: no timers, polling, receivers, or threads run while idle. Every Handler callback is removed when it is not in use.
- Measure RAM: `adb shell dumpsys meminfo <package>`, target under 40 MB PSS when idle.
- Measure battery: `adb shell dumpsys batterystats` after several hours of normal use.
- Make sure the `ACTION_MOVE` path makes no repeated allocations (no new objects per event).
- Test survival after a reboot, after power saving mode is enabled, and after several hours with the screen off.

## Related files

- `SettingsActivity.kt`, `VButtonService.kt`, the whole overlay

## Acceptance criteria

- The service stays active after at least 24 hours of normal use on a Vivo T1 Pro, once the guide has been followed.
- Idle RAM is under the target.
- There is no conspicuous battery consumption.

## Verification

- Record the `dumpsys meminfo` and `batterystats` results under Notes.

## Notes

- (to be filled in by the worker)
