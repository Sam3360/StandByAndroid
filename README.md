# StandBy for Android

Turn your Android phone into a bedside display with clocks, battery status, upcoming calendar events, and a photo slideshow.

## Install on your phone

1. Open the [latest StandBy release](https://github.com/Sam3360/StandByAndroid/releases).
2. Download the APK attached to the release to your Android phone.
3. Open the downloaded APK and follow Android's installation prompts.
4. If Android asks, allow your browser or file manager to install unknown apps, then return to the APK and continue.

StandBy requires Android 8.0 (API 26) or newer. Calendar events are optional; grant calendar access in the app only if you want to show them. Photos are selected using Android's system photo picker.

### About preview builds

The available APK is a debug-signed preview for testing, not a production or Google Play release. Android treats APKs signed with different keys as different apps for updates. If a future version cannot install over this preview, uninstall the preview first and then install the newer APK. Uninstalling removes the app's saved settings.

## Features

- Full-screen digital, analog, and split-flap-inspired clocks
- Battery level and charging status
- Optional Android screen saver mode for a charging bedside display
- Optional upcoming calendar events
- Photo selection and configurable automatic slideshow
- Persistent clock, night mode, and screen-on settings
- Portrait and landscape layouts

## Use StandBy while charging

Android starts screen savers while a device is idle, so StandBy may not appear immediately when you connect a charger. To enable it:

1. Open StandBy and tap **Settings** > **Set up charging screen**.
2. Choose **StandBy** in Android's screen saver settings.
3. Select **While charging** (wording and menu location can vary by phone).

The phone must be charging and Android must decide it is idle before the screen saver starts. Some manufacturers customize or disable screen saver controls; in that case, use StandBy manually from the app.

## Build from source

These steps are for contributors or anyone who wants to build the app themselves. They are not needed to install the APK from Releases.

### Requirements

- Android Studio with Android SDK Platform 34 installed
- JDK 17 (Android Studio's bundled JDK is suitable)

1. Clone or download this repository.
2. Open the project folder in Android Studio and allow Gradle sync to finish.
3. Select the `app` configuration and run it on a connected Android device or emulator.

You can also build from the project root using the Gradle wrapper:

**Windows (PowerShell):**

```powershell
.\gradlew.bat assembleDebug
```

**macOS or Linux:**

```sh
./gradlew assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Run unit tests

**Windows (PowerShell):**

```powershell
.\gradlew.bat testDebugUnitTest
```

**macOS or Linux:**

```sh
./gradlew testDebugUnitTest
```
