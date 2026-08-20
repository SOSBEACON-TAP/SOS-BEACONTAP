# SOS BeaconTap — Android App

A WebView wrapper that auto-connects to the SOS BeaconTap device's WiFi
(`ESP32C3-SOS`) and loads its built-in dashboard at `http://192.168.4.1`.

This folder is a complete Android Studio project. I couldn't compile the
`.apk` file directly for you (this environment has no Android SDK/Gradle
and no internet access), so pick **one** of the two options below — both
are free and neither requires writing any code.

---

## Option A — Android Studio (if you have a Windows/Mac/Linux PC)

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open Android Studio → **Open** → select this `SOSBeaconTap` folder.
3. Let it finish syncing (it will download Gradle + the Android SDK
   automatically the first time — needs internet, takes a few minutes).
4. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. When it finishes, click the **locate** link in the notification, or find
   the file at:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Copy that `.apk` to your phone and install it (enable "install from
   unknown sources" if asked).

## Option B — GitHub Actions (no PC needed, builds in the cloud)

1. Create a free account at [github.com](https://github.com) if you don't
   have one.
2. Create a new **public** repository (e.g. `sos-beacontap-app`).
3. Upload this entire `SOSBeaconTap` folder's contents into that repository
   (drag-and-drop works on github.com — "Add file → Upload files").
4. Go to the repo's **Actions** tab → you should see "Build APK" → click
   **Run workflow**.
5. Wait ~2–3 minutes for it to finish (green checkmark).
6. Click into the finished run → scroll down to **Artifacts** →
   download `SOS-BeaconTap-debug-apk` (this is a zip containing the
   `.apk`).
7. Unzip it, copy `app-debug.apk` to your phone, and install it.

---

## What the app does

- On open, it tries to auto-join the device's WiFi AP
  (SSID `ESP32C3-SOS`, password `12345678`) using Android's network
  APIs (Android 10+). On older Android versions it opens WiFi settings
  and asks you to connect manually.
- Once connected, it loads `http://192.168.4.1` inside the app — this is
  the same admin login/dashboard page defined in the ESP32 firmware
  (`startConfigMode()`, entered by 4 taps on the physical SOS sensor).
- Login credentials are whatever's set in the firmware
  (`admin` / `esp32sos` by default).

## Notes / things you may want to change later

- App id: `com.beacontap.sos` — change in `app/build.gradle` if you plan
  to publish it.
- Device WiFi name/password/URL are in
  `app/src/main/res/values/strings.xml` — update these if you ever change
  `AP_SSID` / `AP_PASS` in the `.ino` firmware.
- The device's login page currently sends the username/password with no
  encryption (`http://`, not `https://`) — fine for a local offline AP,
  but don't reuse this password anywhere important.
