# T08: Settings and onboarding

Status: done (2026-11-01, orchestrator-audited)
Dependencies: T07
Required reading: `DESIGN.md`, `docs/virtual-buttons/01-ui-spec.md`
Required skills: andrej-karpathy-skills, hallmark (see `AGENTS.md`)

## Goal

- A settings screen and a clear activation guide, where changes take effect on the overlay immediately.

## Scope

- In scope: SettingsActivity, service status, settings, onboarding, live updates.
- Out of scope: the detailed Vivo battery guide (T09).

## Implementation steps

- Status screen: show whether the service is active. Detect it through `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` or `VButtonService.instance != null`.
- An "Enable" button opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- Restricted Settings onboarding: step-by-step text (App info, three-dot menu, Allow restricted settings) shown when the service cannot be enabled.
- Settings: size (S, M, L), handle opacity, auto-collapse timeout, haptics on or off, reset position.
- Store every value in `Prefs`. The service registers an `OnSharedPreferenceChangeListener` on connect and removes it on destroy, then applies changes to the overlay through `applyConfig()`.
- Build the UI with simple views (LinearLayout, Switch, SeekBar) and no extra libraries.
- Follow the Settings screen rules in `DESIGN.md` section 3: platform theme, a plain list, platform widgets, no custom palette, literal sentence-case copy, and onboarding as plain numbered steps.

## Related files

- `SettingsActivity.kt`, `Prefs.kt`, `VButtonService.kt`, `res/layout/activity_settings.xml`

## Acceptance criteria

- The service status is shown correctly when active and when inactive.
- Changing a setting is reflected in the overlay immediately without a restart.
- Reset position returns the handle to its default.
- The onboarding helps a user enable the service from scratch.

## Verification

- Run the design check in `DESIGN.md` section 5 and record the result under Notes.
- Test from a clean install through to an active service.
- Change every setting while the overlay is visible.

## Notes

- (orchestrator, 2026-11-01) Implemented and reviewed clean. Settings: plain list, platform widgets, DayNight via values-v29 (DeviceDefault fallback on 28), status = instance OR exact secure-setting match (refresh in onResume), Enable button, Size radios S/M/L, opacity SeekBar 30-100 live readout, timeout SeekBar 1-10s (ruled bounds), Haptics Switch, Reset position, always-on numbered onboarding with (Android 13+) restricted-settings step + one Vivo sentence. Listener paired register/unregister -> applyConfig; applyConfig reads opacity+preset fresh; mid-drag alpha retarget guard (T05 finding) closed; timeout read-at-schedule confirmed. 42 unit tests.
- (orchestrator, 2026-11-01) Deferred minors: stale doc comments Tokens.kt:34/77 ("arrives in T08"/"T07/T08 make selectable" now past tense — read-only boundary, fix in final review); "M" case maps via Tokens.activePreset default instead of PRESET_M directly (test locks the coupling); redundant refreshUi in onCreate; step-4 copy repeats "Turn the service on"; no duplicate-listener guard on reconnect (near-impossible, idempotent anyway).
