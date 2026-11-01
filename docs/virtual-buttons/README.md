# Worker README: virtual-buttons

## Task goal

- Build a native Android app (Kotlin) with virtual Volume +, Volume -, and Power buttons as an overlay on the right edge of the screen.
- Primary target device: Vivo T1 Pro. Full details are in `00-main-task.md`.

## Required reading before you start

- Read `00-main-task.md` completely (PRD, constraints, risks, acceptance criteria).
- Read `01-ui-spec.md` (icons, size presets, responsive rules). Icon files are in `assets/icons/`.
- For UI tasks (T03, T05, T07, T08, T11), also read `DESIGN.md` at the project root (locked visual tokens and anti-AI-slop rules) and load the skills named in `AGENTS.md`.
- Read the task file you are about to work on in the `tasks/` folder.
- Read `AGENTS.md` at the project root and follow it. If the worker prompt names another `SKILL.md`, read that too.

## General rules

- Finish one task and verify it before moving on to the next.
- Do not add runtime dependencies beyond what the PRD allows (at most `androidx.core:core-ktx`). `junit` for unit tests is allowed.
- Do not add the `INTERNET` permission or any analytics or ad library.
- Do not use Compose or third-party UI libraries.
- Do not create timers, polling, or receivers that run while idle.
- Keep code small and clear, with one responsibility per file.
- Use dp for every dimension and never hardcode px.
- Do not change the task scope. If you find a problem in another task, note it under Notes and do not fix it silently.

## Implementation order

- 1. T01 Project setup and skeleton
- 2. T02 AccessibilityService and overlay window
- 3. T03 Handle and panel UI
- 4. T04 Expand and collapse gestures
- 5. T06 Volume and power actions
- 6. T05 Drag and position persistence
- 7. T07 Responsive behavior and gesture exclusion
- 8. T08 Settings and onboarding
- 9. T09 Vivo resilience and performance
- 10. T10 QA and APK release
- 11. T11 Optional (only when requested)

Order note: T06 is done before T05 so the core functions (volume and power) become usable sooner.

## Status tracking

Change `[ ]` to `[x]` only after the task's verification passes.

- [x] T01 Project setup and skeleton
- [x] T02 AccessibilityService and overlay window
- [x] T03 Handle and panel UI
- [x] T04 Expand and collapse gestures
- [x] T06 Volume and power actions
- [x] T05 Drag and position persistence
- [x] T07 Responsive behavior and gesture exclusion
- [x] T08 Settings and onboarding
- [x] T09 Vivo resilience and performance
- [x] T10 QA and APK release
- [ ] T11 Optional: wake helper and extra buttons

## Verification

- Every task has Acceptance criteria and Verification sections. Run all of them.
- The build must pass: `./gradlew assembleDebug`.
- Install on a device: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- View logs: `adb logcat -s VButton`.
- If you cannot test on a device, state clearly in Notes which parts are untested.

## Worker history and handover

Add one entry per work session, newest at the bottom.

Entry template:

- Date:
- Worker:
- Task worked on:
- Status: done / partial / blocked
- Files changed:
- Unfinished items or things to check:
- Notes for the next worker:

History:

- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator
- Task worked on: T01 Project setup and skeleton
- Status: done
- Files changed: root Gradle files (build.gradle.kts, settings.gradle.kts, gradle.properties), Gradle wrapper 8.9, app/build.gradle.kts, app/proguard-rules.pro, AndroidManifest.xml (zero permissions), Prefs.kt (PREFS_FILE placeholder), SettingsActivity.kt (launcher), launcher icon (adaptive, monochrome per DESIGN.md), strings.xml, .gitignore
- Unfinished items or things to check: "app installs and opens a blank screen" needs device test (no adb in build env); assembleRelease deliberately not run (forbidden outside T10, ruling in SDD ledger); R8 configured but unverified until T10; Minor parked: explicit Kotlin jvmTarget line (safe while AGP/Kotlin pinned), android:allowBackup decision deferred to T10
- Notes for the next worker: build env has no system Java/SDK — `source ~/tools/android-env.sh` before every gradle command (JDK 17, Gradle 8.9, SDK 35 at ~/Android/Sdk; local.properties untracked with sdk.dir). Stack: minSdk 28, compileSdk/targetSdk 35, AGP 8.7.3, Kotlin 2.0.21, applicationId com.awannawa.vbutton. Runtime deps: none; junit test-only. Do not run assembleRelease. .superpowers/ is locally excluded via .git/info/exclude
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator
- Task worked on: T02 AccessibilityService and overlay window
- Status: done
- Files changed: VButtonService.kt (service, VButtonService.instance singleton with private set, attachOverlay/detachOverlay idempotent + try-catch, 24dp x 72dp placeholder window), res/xml/accessibility_service_config.xml, AndroidManifest.xml (service declaration, still zero permissions), strings.xml
- Unfinished items or things to check: needs device test — service listed in Settings > Accessibility with description; placeholder visible on right edge over all apps; toggling off removes it without crash; `adb shell dumpsys accessibility` shows no event types
- Notes for the next worker: attachOverlay/detachOverlay are private in VButtonService — T03 extends them in place to swap placeholder for OverlayView (AGENTS.md integration point). dp() in VButtonService truncates with toInt(); use Math.round in new helpers. windowManager field not nulled in onDestroy (parked Minor, harmless). Placeholder pinned to 24x72dp via onMeasure because a bare View in a WRAP_CONTENT window measures full height — T03 replaces it entirely
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; 1 fix round (dp() truncation -> Math.round)
- Task worked on: T03 Handle and panel UI
- Status: done
- Files changed: overlay/Tokens.kt (single source of all overlay dp/colors/durations, presets S/M/L as data), overlay/ButtonSpec.kt, overlay/OverlayView.kt (setExpanded/applyConfig idempotent, state enum, 150ms translationX DecelerateInterpolator), overlay/HandleView.kt (5x72dp bar, 1dp border, rounded ends, alpha 0.6), overlay/PanelView.kt (renders from ButtonSpec list, pressed highlight circle, icon inset math), VButtonService.kt (placeholder -> OverlayView, dp() now Math.round), strings.xml (3 contentDescriptions), drawable ic_volume_up/ic_volume_down/ic_power.xml (byte-identical copies)
- Unfinished items or things to check: needs device test — both states visually (slide direction/feel, window width via Layout Inspector), readability on white AND black app backgrounds, alignment at handle Y. Design check section 5 passed in code review; screenshot-dependent items pending
- Notes for the next worker (T04): state model is one enum (Collapsed/Expanded) with a documented path for Dragging (T05). Collapse-side window shrink (60->24dp) runs in the guarded animation end action so the close slide stays visible — expand grows immediately. In-flight animation not canceled on detach (benign caught error-log noise, parked). T08: a preset change while collapsed must refresh the parked translationX offset. PRESET_S/L + panelHeightDp + repeatable are declared-unused until T05-T08 (spec data, not dead code)
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; 1 fix round (fling vertical-dominance per FR-4, orchestrator ruling; +3 JVM tests)
- Task worked on: T04 Expand and collapse gestures
- Status: done
- Files changed: overlay/GestureController.kt (new; tap/left-fling detection, ViewConfiguration thresholds at event time, dominance gate), overlay/OverlayView.kt (auto-collapse Handler with paired remove paths, resetAutoCollapse() public for T06, ACTION_OUTSIDE collapse guarded by state), overlay/HandleView.kt (touch consumption + forwarding), Prefs.kt (auto-collapse timeout key, default 3000), VButtonService.kt (FLAG_WATCH_OUTSIDE_TOUCH added, FLAG_NOT_FOCUSABLE kept), ShouldOpenOnFlingTest.kt (9/9)
- Unfinished items or things to check: needs device test — 20 taps + 20 swipes open the panel; auto-collapse after ~3s and reset on any panel touch; tap outside collapses (no flicker when collapsed); handle touches don't leak to the app below; service-off-while-open is clean in logcat
- Notes for the next worker (T06): wire Actions-backed lambdas into the ButtonSpec list inside OverlayView (defaultButtons) and call its own resetAutoCollapse() on volume actions — VButtonService.overlayView stays private; construct Actions in VButtonService and pass into OverlayView. See T04 task-file Notes for the lint deferral and fling ruling
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; 1 fix round (implemented the locked 100ms pressed-highlight fade-out per DESIGN.md Motion, orchestrator ruling; Tokens gained PRESS_FADE_OUT_MS)
- Task worked on: T06 Volume and power actions
- Status: done
- Files changed: Actions.kt (new; volume via adjustSuggestedStreamVolume FLAG_SHOW_UI, power tap/long-press via performGlobalAction with checked+logged results, repeat scheduler 400ms/150ms + PressPolicy JVM-tested, tap-vs-long-press single-fire), overlay/ButtonSpec.kt (additive press lifecycle, ratified in AGENTS.md), overlay/PanelView.kt (down/up/cancel wiring, haptics read at press time, 100ms fade-out on release with instant-appear-wins rule), overlay/OverlayView.kt (Actions injected, defaultButtons call Actions + resetAutoCollapse on every action and repeat tick), Prefs.kt (haptics_enabled default true), VButtonService.kt (constructs Actions, passes in), overlay/Tokens.kt (PRESS_FADE_OUT_MS=100 from DESIGN.md Motion), PressPolicyTest.kt (14/14 total)
- Unfinished items or things to check: needs device test — volume +/- with system indicator on the active stream (music playing AND ringer mode), hold-to-repeat on both volume buttons, power tap locks / long-press opens power menu, Funtouch OS behavior vs expectation (PRD R6), logcat shows no handler running after release, finger slide-off keeps repeat running (task-letter compliant, judge on device)
- Notes for the next worker (T05): Actions interface is stable and T11-ready (T11 adds one method + one ButtonSpec list item, zero PanelView changes). Press lifecycle fields are part of ButtonSpec (AGENTS.md updated). State enum gains Dragging (path documented in OverlayView since T03). DESIGN.md drag state: handle scaleX 1.5 + alpha 1.0 while dragging, back on release in 100ms. SafeArea.compute() must have a signature T07 can improve without breaking callers
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; review clean (no fix round)
- Task worked on: T05 Drag and position persistence
- Status: done
- Files changed: overlay/SafeArea.kt (new; pure core clamp/ratio + API-28/29 fallback wrapper, signature stable for T07), overlay/GestureController.kt (long-press enters drag, raw-stream drag handling, release-flick cannot open panel), overlay/OverlayView.kt (Dragging state with preDragState capture, allocation-free move path, yRatio save on release, restore on Expanded), overlay/Tokens.kt (drag-scale/alpha/release-100ms citing DESIGN.md Motion), Prefs.kt (y_ratio key default 0.4), VButtonService.kt (restore yRatio on connect before addView), SafeAreaTest.kt (16 tests; 30 total)
- Unfinished items or things to check: needs device test — drag tracking without lag, clamps at safe-area extremes, persistence across service restart AND reboot, expanded-drag moves whole panel, 100ms release visuals, release-flick does not open panel
- Notes for the next worker (T07): (1) SafeArea's API-28/29 inset fallback (status_bar_height/navigation_bar_height resources + DisplayMetrics) is plan-mandated temporary — replace internals with WindowMetrics/insets per 01-ui-spec section 4, keep compute() signature; bottom clamp is currently loose under gesture navigation. (2) T08: applyConfig mid-drag would restore a stale preDragHandleAlpha on release — re-read alpha in endDrag or branch applyConfig mid-drag. (3) Multi-touch: rawY tracks pointer 0 — dragging finger lift with a second finger down makes the window jump to the other finger (self-corrects; pointer-id tracking is the fix if device testing shows it matters). (4) Drag-entry haptic uses HapticFeedbackConstants.LONG_PRESS (heavier than T06's VIRTUAL_KEY) — deliberate-or-lighter call open
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; review clean (no fix round)
- Task worked on: T07 Responsive behavior and gesture exclusion
- Status: done
- Files changed: overlay/SafeArea.kt (WindowMetrics systemBars+displayCutout on API 30+, resource fallback below; pure selectFittingPreset compact mode, floor S; compute() signature unchanged), overlay/OverlayView.kt (gesture exclusion rects in onLayout with API 29 guard, lp.x = right inset, compact preset consumption with parked-translationX refresh, applyConfig runs full recompute), VButtonService.kt (onConfigurationChanged -> recomputeLayout, layoutInDisplayCutoutMode ALWAYS/SHORT_EDGES with rationale), SafeAreaTest.kt (+9 compact-mode tests; 39 total)
- Unfinished items or things to check: needs device test — rotation at top/middle/bottom, both nav modes, back-gesture exclusion works, RTL keeps handle right, compact mode on short landscape, cutout not clipped in landscape, fullscreen apps + lock screen, split-screen, keyboard, small/large display sizes. PARKED plan-mandated: API 28/29 right-inset = 0 (nav-bar overlap possible on those levels only) — T10 risk list
- Notes for the next worker (T08): applyConfig() already runs the full recompute (T07 wired it) — extend it to read your new prefs (opacity, preset, timeout, haptics) and it all applies live; register the OnSharedPreferenceChangeListener in VButtonService onServiceConnected and unregister in onDestroy (pair it!). Handle the T05 finding: an applyConfig arriving mid-drag must not let endDrag restore a stale preDragHandleAlpha. Settings copy: sentence case, literal, no emoji (DESIGN.md section 3); typography tokens 20sp/14sp/16sp are in DESIGN.md
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; review clean (no fix round)
- Task worked on: T08 Settings and onboarding
- Status: done
- Files changed: SettingsActivity.kt (status dual-check, Enable, all setting rows, onboarding), res/layout/activity_settings.xml (new), Prefs.kt (size preset + opacity keys/readers/writers, timeout bounds), VButtonService.kt (prefs listener paired register/unregister), overlay/OverlayView.kt (applyConfig reads prefs, mid-drag alpha retarget guard), res/values/strings.xml (all copy), res/values/themes.xml (DayNight v29 + API 28 fallback), SizePresetFromPrefTest.kt (42 total)
- Unfinished items or things to check: needs device test — clean install to enabled service end to end; restricted-settings step on Android 13+; every setting reflects on the overlay immediately; status active/inactive both ways; day/night; TalkBack on the settings screen
- Notes for the next worker (T09): dead-service warning needs a "was active" record — the service connect path is the place to set it (keys only in Prefs.kt). The idle audit (no timers/polling/receivers/threads while idle; every Handler callback removed when unused) should re-verify: collapse timer (T04, paired), repeat handler (T06, paired + detach kill), fade animator (T06, detach-cancelled). Vivo guide: use ACTION_IGNORE_BATTERY_OPTIMIZATIONS or app info — do not assume a Vivo-specific intent exists. Measurement commands for the report: adb shell dumpsys meminfo com.awannawa.vbutton (target < 40MB PSS idle) and adb shell dumpsys batterystats
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; review clean (no fix round)
- Task worked on: T09 Vivo resilience and performance
- Status: done (code parts; all measurements need device)
- Files changed: SettingsActivity.kt (Vivo guide section, two battery/app-info buttons with fallback, dead-service warning line), res/layout/activity_settings.xml (guide + warning), Prefs.kt (was_service_active key), VButtonService.kt (sets the flag first thing in onServiceConnected), res/values/strings.xml (guide copy)
- Unfinished items or things to check: ALL measurements need device — 24h survival with guide followed; `adb shell dumpsys meminfo com.awannawa.vbutton` < 40MB PSS idle; `adb shell dumpsys batterystats` no significant consumer; reboot survival; power-saving mode; hours screen-off; warning appears after revoke/kill-and-disable. Exact commands in task-09-report.md. Idle audit: all 6 items PASS (verified); drag move path zero-allocation confirmed
- Notes for the next worker (T10): the Settings action constant is ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS (corrected from the ruling — verified against android.jar). was_service_active never clears by design. Force-stop gap is a documented limitation (T09 Notes). You own signing: keystore OUTSIDE the repo (~/keystores/), secrets in a gitignored properties file — never commit them. assembleRelease is allowed FOR YOU ONLY. After release build, check APK size (NFR-5 <1MB if possible) and permissions (aapt dump permissions — must stay empty)
- Date: 2026-11-01
- Worker: orchestrator-dispatched implementer (fresh subagent session), audited by orchestrator; review clean (no fix round)
- Task worked on: T10 QA and APK release
- Status: done (release pipeline + docs; device QA checklist outstanding)
- Files changed: app/build.gradle.kts (signingConfig from /home/awan/keystores/vbutton-signing.properties with debug fallback), .gitignore (keystore/secret patterns), docs/virtual-buttons/INSTALL.md (new). Keystore + secrets live OUTSIDE the repo at /home/awan/keystores/ (alias vbutton; passwords only in the chmod-600 properties file)
- Unfinished items or things to check: DEVICE QA — AC-1..AC-6 manual steps, NFR-1..4 measurements, AC-9 screenshots (exact steps in task-10-report.md and INSTALL.md). AC-7 verified (zero permissions on both APKs); NFR-5 verified (release APK 62,453 bytes)
- Notes for the next worker: only T11 (optional) remains — needs explicit request. Release APK at app/build/outputs/apk/release/app-release.apk. To rebuild release on this machine, the signing properties must exist at /home/awan/keystores/vbutton-signing.properties; other machines fall back to debug signing. Keep the keystore safe — losing it means the app can never be updated in place (would need uninstall/reinstall)
