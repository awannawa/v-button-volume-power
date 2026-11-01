# T02: AccessibilityService and overlay window

Status: TODO
Dependencies: T01

## Goal

- An active accessibility service that shows a single overlay view on the right edge of the screen, on top of all apps.

## Scope

- In scope: service declaration, XML config, creating and removing the overlay window, lifecycle.
- Out of scope: the final visuals (T03), gestures (T04), and actions (T06).

## Implementation steps

- Create `res/xml/accessibility_service_config.xml`. Do not set the `accessibilityEventTypes` attribute (the default of 0 means no events). Set `canRetrieveWindowContent="false"`, `canPerformGestures="false"`, `accessibilityFeedbackType="feedbackGeneric"`, and a `description`.
- Declare the service in the manifest with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, an intent-filter for `android.accessibilityservice.AccessibilityService`, and meta-data pointing to the config XML.
- Create `VButtonService : AccessibilityService`. Leave `onAccessibilityEvent` and `onInterrupt` empty.
- In `onServiceConnected`: get the `WindowManager`, build `LayoutParams` with `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE`, `PixelFormat.TRANSLUCENT`, gravity `RIGHT or TOP`, width 24dp, height WRAP_CONTENT, then `addView` a placeholder (a colored box).
- In `onDestroy` and `onUnbind`: call `removeView` with a safe check (try-catch `IllegalArgumentException`).
- Keep a reference to the service in a lightweight singleton (`VButtonService.instance`) for SettingsActivity, and set it to null on destroy.

## Related files

- `VButtonService.kt`, `accessibility_service_config.xml`, `AndroidManifest.xml`, `strings.xml`

## Acceptance criteria

- The service appears in Settings, Accessibility with its description.
- After it is enabled, the placeholder is shown on the right edge in all apps.
- Disabling the service removes the placeholder without crashing.
- The service receives no accessibility events.

## Verification

- Enable the service, open several apps, and confirm the placeholder stays visible.
- `adb logcat` shows no crash when toggling the service on and off.
- `adb shell dumpsys accessibility` shows the service with no event types.

## Notes

- (to be filled in by the worker)
