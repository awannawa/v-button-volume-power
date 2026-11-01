# T01: Project setup and skeleton

Status: TODO
Dependencies: none

## Goal

- An Android Kotlin project that builds and installs, ready to serve as the foundation for the following tasks.

## Scope

- In scope: project structure, Gradle, manifest, basic resources, small release configuration.
- Out of scope: the service, the overlay, or any logic.

## Implementation steps

- Create an Empty Views Activity project (Kotlin) and choose the package and app name (see Open decisions in the PRD).
- Set `minSdk 28` and set `targetSdk` and `compileSdk` to the latest stable version.
- Remove unnecessary dependencies (AppCompat, Material, ConstraintLayout) if they are not used. Keep at most `core-ktx` for runtime, and `junit` via `testImplementation` for unit tests.
- Enable R8 for release: `isMinifyEnabled = true`, `isShrinkResources = true`.
- Make sure the manifest does not contain the `INTERNET` permission.
- Add an empty `SettingsActivity` as the launcher.
- Prepare a simple launcher icon (adaptive icon vector).
- Create an empty `Prefs.kt` (an object holding the SharedPreferences file name) as a placeholder.

## Related files

- `build.gradle.kts` (app and root), `AndroidManifest.xml`, `SettingsActivity.kt`, `Prefs.kt`

## Acceptance criteria

- Debug and release builds pass.
- The app installs and opens a blank screen without crashing.
- The manifest does not contain `INTERNET`.

## Verification

- `./gradlew assembleDebug assembleRelease`
- `adb install -r ...` then open the app.
- Run `aapt dump permissions` on the APK and confirm there is no `INTERNET`.

## Notes

- (to be filled in by the worker)
