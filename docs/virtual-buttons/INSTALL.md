# INSTALL: Virtual Buttons

Virtual Buttons is an Android app that puts virtual volume up, volume down, and power buttons in a small overlay on the right edge of the screen, for phones whose physical buttons are broken.

## 1. Install the APK

- The release APK is `app/build/outputs/apk/release/app-release.apk`.
- With a computer and adb connected to the phone:

  ```
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```

- Without adb: copy the APK to the phone, open it from a file manager, and confirm the install. Android asks you to allow "install unknown apps" for the file manager first.

- Open the app once after installing. It shows the settings screen.

## 2. Enable the accessibility service

1. Open Settings, then Accessibility, then Virtual Buttons (or tap Enable in the app).
2. Turn the service on and confirm the permission dialog.
3. A thin handle appears on the right edge of the screen, on top of every app.

### If the toggle is blocked (Android 13 and newer)

Android blocks accessibility for sideloaded apps until you allow restricted settings:

1. Open the app info page: long-press the Virtual Buttons icon, then App info (or Settings, Apps, Virtual Buttons).
2. Tap the three-dot menu in the top right corner.
3. Tap Allow restricted settings.
4. Go back to Settings, Accessibility, Virtual Buttons, and turn the service on.

## 3. Keep the service running on Vivo (Funtouch OS)

Funtouch OS may stop the service in the background unless you allow it to start and run:

1. Autostart: enable Autostart for Virtual Buttons (Settings, Battery, or the i Manager app, App manager, Autostart).
2. Battery: set battery use to unrestricted (Settings, Battery, Background power consumption management, or App info, Battery, Unrestricted).
3. Recents: open the recents screen and lock Virtual Buttons with its lock icon.

The app shows a warning line when the service is not running, so you can tell when Funtouch OS has stopped it.

## 4. Use the overlay

- Tap the handle on the right edge (or swipe it left) to open the panel.
- Tap Volume up, Volume down, or Power. Holding a volume button repeats the change.
- Tap Power once to lock the screen. Long-press Power to open the system power menu.
- Long-press the handle, then drag up or down to move it. The position is saved.
- The panel closes itself after the auto-collapse timeout (3 seconds by default), or when you tap outside it.
- Open the Virtual Buttons app to change size, handle opacity, auto-collapse timeout, and haptics, or to reset the position. Changes apply immediately.

## 5. If something goes wrong

- Hide the buttons: Settings, Accessibility, Virtual Buttons, and turn the service off. The overlay disappears immediately.
- Remove the app completely: uninstall Virtual Buttons from its app info page or the launcher. Uninstalling also removes the service.
- If the handle is in the way while the service is on, drag it to another height, or lower the handle opacity in the app.

The app requests no permissions, does not use the internet, and does not read screen content.
