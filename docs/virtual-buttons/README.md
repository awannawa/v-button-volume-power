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
- [ ] T02 AccessibilityService and overlay window
- [ ] T03 Handle and panel UI
- [ ] T04 Expand and collapse gestures
- [ ] T06 Volume and power actions
- [ ] T05 Drag and position persistence
- [ ] T07 Responsive behavior and gesture exclusion
- [ ] T08 Settings and onboarding
- [ ] T09 Vivo resilience and performance
- [ ] T10 QA and APK release
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
