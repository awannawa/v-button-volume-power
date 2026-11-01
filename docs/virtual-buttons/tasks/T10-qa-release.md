# T10: QA and APK release

Status: TODO
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

- (to be filled in by the worker)
