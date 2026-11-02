# Device QA checklist: virtual-buttons

Manual verification run for the owner on the Vivo T1 Pro (or any test device).
Produced from the T10 QA report after the final whole-branch review. Machine-verified
items (AC-7 permissions, NFR-5 APK size, 42/42 unit tests, lint, both build types)
are already done — everything below needs a device.

Release APK: `app/build/outputs/apk/release/app-release.apk` (~61 KB, signed with the
personal key). Install: `adb install -r app/build/outputs/apk/release/app-release.apk`.

## Steps

1. **Install and enable end to end**: clean install; open the app; enable via
   Settings → Accessibility → Virtual Buttons; the handle appears. On Android 13+:
   App info → three-dot menu → Allow restricted settings first.
2. **AC-1**: handle visible over browser, chat, fullscreen video, games, camera,
   lock screen. `adb reboot` → handle present after unlock without re-enabling;
   `adb shell settings get secure enabled_accessibility_services` still contains
   `com.awannawa.vbutton/.VButtonService`.
3. **AC-2**: 20 taps and 20 left swipes open the panel; the panel collapses after the
   configured timeout; any panel interaction resets the timer; a tap outside collapses
   it; no flicker while collapsed.
4. **AC-3**: long-press then drag up and down (no lag); clamps at the safe-area
   extremes; dragging while expanded moves the whole panel; a release flick does not
   open the panel; the position survives service off/on AND a reboot.
5. **AC-4**: volume +/- changes the active stream with the system indicator (test
   with music playing AND in ringer mode); hold repeats on both buttons; slide the
   finger off during a hold — the repeat keeps running (a known task-letter behavior;
   judge whether it annoys before requesting a fix).
6. **AC-5**: power tap locks the screen; power long press opens the system power
   menu; note any Funtouch OS variance (PRD risk R6).
7. **AC-6**: rotate portrait/landscape with the handle at top, middle, and bottom;
   both gesture and 3-button navigation; split-screen; keyboard open; incoming call.
   Also the task-list scenarios: browser, chat, fullscreen video, games, camera,
   lock screen.
8. **NFR-1 runtime**: `adb shell dumpsys accessibility` shows no event types for the
   service; `adb logcat -s VButton` is silent while idle.
9. **NFR-2**: `adb shell dumpsys meminfo com.awannawa.vbutton` — TOTAL PSS under
   40 MB when idle.
10. **NFR-3**: `adb shell dumpsys batterystats --charged com.awannawa.vbutton` after
    at least 6 hours — no significant battery consumer.
11. **NFR-4**: a volume change feels under 100 ms from touch.
12. **AC-9 screenshots**: overlay collapsed and expanded over a white app and a black
    app; settings screen in day and night; compare against DESIGN.md section 5.
13. **Vivo resilience (T09)**: follow the in-app Vivo guide (Autostart, unrestricted
    battery, locked in Recents); 24-hour survival including a reboot; power-saving
    mode; hours with the screen off then wake; the dead-service warning appears after
    revoke and clears on re-enable; both guide buttons resolve on Funtouch.
14. **Known risk check**: if any device runs API 28/29 (older than the T1 Pro), check
    the handle against the navigation bar in landscape — the right-inset fallback is 0
    on those levels only (parked plan limitation, see T07 notes).
15. **Screen side (left)**: set Settings → Screen side → Left. Verify the handle sits
    flush to the left edge over both a white and a black app; the panel opens from the
    left at the same height; a rightward swipe opens it while a leftward swipe and the
    Back gesture do not navigate; the drag pivot grows inward; rotation keeps it on the
    left; the choice survives service off/on and a reboot. Then set it back to Right and
    confirm the original behaviour. Changing the side while the panel is open must move
    it live without closing it, keeping the same height.

## Known documented limitations (do not re-report)

- Force-stop reads as Active in the app (the secure listing persists); the
  dead-service warning catches revoke / kill-and-disable (the Funtouch pattern) and
  force-stop self-heals on next app open.
- TalkBack cannot activate the collapsed handle (touch-only utility; recorded as
  accepted a11y debt in the T04 task notes).
- On API 28/29 only: possible landscape navigation-bar overlap (step 14).
