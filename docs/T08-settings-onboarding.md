# T08: Settings and onboarding

Status: TODO
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

- (to be filled in by the worker)
