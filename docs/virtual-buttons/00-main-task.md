# PRD and Main Task: Virtual Button (Volume and Power)

## 1. Summary

- Working name: `vbutton`
- Type: native Android app (Kotlin) running as an AccessibilityService with an overlay on the right edge of the screen.
- Function: replaces the broken physical Volume Up, Volume Down, and Power buttons.
- Primary device: Vivo T1 Pro (Android 12, Funtouch OS). It must stay compatible with the owner's other Android devices.
- Principles: no ads, no internet, no tracking, low resource usage, fully customizable.

## 2. Background and problem

- The physical volume and power buttons on the owner's phone are broken, which makes the phone hard to use.
- Virtual button apps on the Play Store are full of ads, cannot be customized, or are unstable on the owner's device.
- The owner wants their own app that fits their usage style and works well on their devices.

## 3. Goals

- G1: Volume up, volume down, and access to power can be done entirely from the screen.
- G2: The UI is almost invisible when unused (only a thin handle on the right edge).
- G3: Runs continuously in the background without draining battery or RAM.
- G4: Responsive in portrait, landscape, and across screen sizes.
- G5: All important behavior is configurable (position, size, opacity, timeout, button actions).

## 4. Non-goals (v1)

- Does not turn the screen on from the off state (not possible via an accessibility service, see Risks and T11).
- No cloud features, accounts, analytics, or ads.
- Not published to the Play Store (distributed as a personal APK).
- No Jetpack Compose or third-party UI libraries.
- No root, no Shizuku.

## 5. Technical targets

- Language: Kotlin.
- minSdk 28 (because `GLOBAL_ACTION_LOCK_SCREEN` and `TAKE_SCREENSHOT` require API 28).
- targetSdk and compileSdk: the latest stable version supported by the Android Studio in use.
- UI: plain Views and Canvas, no Compose, no AppCompat if possible.
- Runtime dependencies: at most `androidx.core:core-ktx`. Zero dependencies is preferred. `junit` is allowed via `testImplementation` for unit tests.
- Manifest permissions: no `INTERNET`. Only `VIBRATE` (optional, for haptics) and the permissions inherent to an accessibility service.

## 6. User scenarios

- S1: The user touches the thin handle on the right, the button panel appears, the user presses Volume +, and the panel closes itself after a few seconds.
- S2: The user long-presses the handle and can drag it up or down to a comfortable position. The position is saved.
- S3: The user taps Power once and the screen locks. Long-pressing Power opens the system power menu.
- S4: The user rotates the phone to landscape. The handle stays on the right edge and remains inside the screen.
- S5: The user holds Volume + or Volume -, and the volume keeps changing until the button is released.

## 7. Functional requirements

- FR-1: After it is enabled, the accessibility service shows an overlay that is always present across all apps.
- FR-2: Collapsed state: only a thin handle attached to the right edge.
- FR-3: Expanded state: a vertical panel attached to the right edge containing Volume +, Volume -, and Power, appearing at the handle's vertical position.
- FR-4: Expanding is triggered by a short tap on the handle or a swipe to the left on the handle.
- FR-5: The panel collapses automatically after N seconds without interaction (default 3 seconds, configurable) or on a tap outside the panel.
- FR-6: A long press on the handle enters drag mode. The handle moves only vertically (Y axis) and the position is saved on release.
- FR-7: Volume +/-: change the volume of the currently active stream and show the system volume indicator. Hold to repeat.
- FR-8: Power tap: lock the screen. Power long press: show the system power menu.
- FR-9: Short haptic feedback when a button is pressed (can be turned off).
- FR-10: A settings screen: service status, size, opacity, auto-collapse timeout, haptics, reset position.
- FR-11: Setting changes apply to the overlay immediately without restarting the service.
- FR-12: Onboarding that guides the user to enable the service and allow restricted settings when needed.

## 8. Non-functional requirements

- NFR-1 (CPU): the service receives no accessibility events and runs no polling or timers while idle.
- NFR-2 (RAM): under 40 MB PSS when idle (measure with `dumpsys meminfo`).
- NFR-3 (Battery): does not appear as a significant battery consumer in battery statistics.
- NFR-4 (Latency): under 100 ms from touch to action (volume changes).
- NFR-5 (Size): release APK under 1 MB if possible (R8 and shrinkResources enabled).
- NFR-6 (Resilience): the service survives reboot, rotation, and hours-long usage sessions on Funtouch OS.
- NFR-7 (Privacy): never reads screen content. `canRetrieveWindowContent=false`.
- NFR-8 (Accessibility): touch targets of at least 48dp for panel buttons, `contentDescription` set.
- NFR-9 (Responsive): the UI follows `01-ui-spec.md` in portrait, landscape, the three size presets, various display sizes, and RTL languages.
- NFR-10 (Design quality): the overlay and settings screen follow `DESIGN.md` and pass its design check (section 5), with no generic AI-generated styling.

## 9. UI/UX specification

- Collapsed: a thin bar, 5dp wide and 72dp tall when visible, with rounded corners and a default opacity of 60 percent (configurable). The touch area is 24dp wide so it is easy to hit.
- Expanded: a vertical semi-transparent card, 56 to 64dp wide, containing 3 buttons of 48 to 56dp stacked vertically (Vol +, Vol -, Power) depending on the size preset, attached to the right edge at the handle's Y position.
- Style: minimalist, monochrome vector icons, no text, rounded corners, no heavy shadows. Locked visual tokens and the anti-AI-slop rules are in `DESIGN.md` at the project root. Size presets and responsive rules are in `01-ui-spec.md`. Icon files are in `assets/icons/`.
- Animation: slide in from the right (`translationX`) in about 150 ms, no heavy animation or blur.
- Drag mode: the handle grows slightly and its opacity increases to show it is active, with a short haptic when entering drag mode.
- The Y position is stored as a ratio of the safe area (not px) and then clamped so the panel never leaves the screen or covers the status bar or navigation bar.

## 10. Short architecture

- `VButtonService` (AccessibilityService): owns the WindowManager, adds and removes the overlay, and performs global actions.
- `OverlayView` (FrameLayout): contains `HandleView` and `PanelView` and manages the collapsed or expanded state.
- `GestureController`: a GestureDetector for tap, left fling, long press, and drag.
- `Actions`: a wrapper around `AudioManager` and `performGlobalAction`.
- `Prefs`: SharedPreferences for position and settings, with a listener so the overlay can update live.
- `SettingsActivity`: the settings UI and onboarding.
- Window: `TYPE_ACCESSIBILITY_OVERLAY`, flag `FLAG_NOT_FOCUSABLE`, gravity `RIGHT or TOP` (not `END`, so it does not move to the left in RTL languages), and a narrow width while collapsed.

## 11. Suggested project structure

- `app/src/main/java/.../VButtonService.kt`
- `app/src/main/java/.../overlay/OverlayView.kt`
- `app/src/main/java/.../overlay/GestureController.kt`
- `app/src/main/java/.../Actions.kt`
- `app/src/main/java/.../Prefs.kt`
- `app/src/main/java/.../SettingsActivity.kt`
- `app/src/main/res/xml/accessibility_service_config.xml`
- `app/src/main/res/drawable/` (vector icons)

## 12. Risks and mitigations

- R1: The Back gesture on the right edge conflicts with a swipe to the left. Mitigation: `systemGestureExclusionRects` on the overlay (limit of about 200dp height per edge) and a 24dp handle touch area.
- R2: Funtouch OS kills the service in the background. Mitigation: guidance for Autostart, unrestricted battery usage, and locking the app in Recents, plus detecting a dead service in SettingsActivity.
- R3: Android 13 and newer block accessibility for sideloaded APKs (Restricted Settings). Mitigation: onboarding explains App info, the three-dot menu, then Allow restricted settings.
- R4: The overlay cannot turn on a screen that is off. Mitigation: Vivo's built-in double-tap to wake, plugging in a charger, fingerprint, or the optional wake helper in T11.
- R5: The overlay blocks touches in other apps. Mitigation: the collapsed window is only as wide as the handle's touch area and is never fullscreen.
- R6: `GLOBAL_ACTION_POWER_DIALOG` and lock screen may behave differently on Vivo. Mitigation: test directly on the device and provide a fallback or a setting for the action.
- R7: A service bug makes the phone hard to use. Mitigation: a small overlay area, and the service can be disabled via Settings, Accessibility.

## 13. Global acceptance criteria

- AC-1: After the service is enabled, the handle appears on the right edge in all apps and remains after a reboot.
- AC-2: A tap or a left swipe opens the panel, and auto-collapse works according to the timeout.
- AC-3: A long press followed by a drag moves the handle vertically, and the position is kept after the service restarts.
- AC-4: Vol +/- change the volume and show the system indicator, and holding repeats the change.
- AC-5: Power tap locks the screen and Power long press opens the power menu.
- AC-6: Rotating to landscape does not push the handle or panel off the screen.
- AC-7: The final manifest has no `INTERNET` permission.
- AC-8: Measurable targets NFR-1 through NFR-5 are met.
- AC-9: The overlay and settings screen pass the design check in `DESIGN.md` section 5.

## 14. Child task list

- T01: Project setup and skeleton
- T02: AccessibilityService and overlay window
- T03: Handle and panel UI
- T04: Expand and collapse gestures
- T05: Drag and position persistence
- T06: Volume and power actions
- T07: Responsive behavior and gesture exclusion
- T08: Settings and onboarding
- T09: Vivo resilience and performance
- T10: QA and APK release
- T11: Optional: wake helper and extra buttons

Details are in the `tasks/` folder. The worker guide is in `README.md`. The icon and responsive specs are in `01-ui-spec.md`. The visual rules are in `DESIGN.md`.

## 15. Open decisions

- (Done) Button icons: custom SVG and VectorDrawable in `assets/icons/`.
- Whether the Mute button belongs in v1 or only in T11.
- Final package name and app name.
- Whether a left-side mode (handle on the left edge) should be a setting or the app stays right-only.
