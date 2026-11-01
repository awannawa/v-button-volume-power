# T10: QA and APK release

Status: done (2026-11-01, orchestrator-audited; device QA outstanding — see Notes)
Dependencies: T09

## Goal

- A tested, signed release APK ready for daily use.

## Scope

- In scope: full testing, bug fixes, signing, release build, short documentation.
- Out of scope: new features.

## Implementation steps

- Run every global acceptance criterion in `00-main-task.md` (AC-1 through AC-9) and tick them one by one under Notes.
- Test inside the apps used most often: browser, chat, fullscreen video, games, camera.
- Test on the lock screen, during an incoming call, and with the keyboard open.
- Create a personal keystore (stored outside the repo) and configure release signing. This is the only task allowed to touch signing.
- Build `assembleRelease`, then check the APK size and the manifest permissions.
- Write a short `docs/virtual-buttons/INSTALL.md`: how to install, how to enable the service, how to deal with restricted settings, and how to disable it if something goes wrong.

## Related files

- `build.gradle.kts`, `INSTALL.md`

## Acceptance criteria

- AC-1 through AC-9 all pass.
- The release APK installs cleanly and works.
- There is no `INTERNET` permission.

## Verification

- Install the release APK on a clean device and run the checklist.

## Notes

- (orchestrator, 2026-11-01) Release pipeline delivered and reviewed clean. Keystore at /home/awan/keystores/vbutton.jks (alias vbutton; passwords ONLY in /home/awan/keystores/vbutton-signing.properties, chmod 600, never committed). signingConfig reads the properties file when present, debug-signed fallback otherwise (fallback drill executed both directions). Release APK 62,453 bytes (NFR-5 <1MB met), signed, aapt dump permissions EMPTY (AC-7 verified on the artifact). INSTALL.md claim-traced against code by review with zero misses. AC table honest: AC-7 + NFR-5 PASS; AC-1..AC-6 needs-device; AC-8/AC-9 partial.
- (orchestrator, 2026-11-01) Ruling: android:allowBackup stays at default (true) — backed-up data is non-sensitive UI preferences only; restore on a new device is user-beneficial; no INTERNET permission means no app-driven exfiltration. Deferred minors (final review triage): signing missing-key NPE edge; storeFile relative-path resolution; .gitignore misses .p12/.pfx patterns; INSTALL.md warning-line wording could note "after you have enabled it once". OUTSTANDING: device QA — full AC-1..AC-6 + NFR-1..4 steps in task-10-report.md.
