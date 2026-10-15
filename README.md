# StandBy

A lightweight Android StandBy experience inspired by bedside display modes.

## Features
- Full-screen digital, analog, and alternate clock layouts
- Calendar event widget with optional permission flow
- Photo slideshow with system picker and configurable automatic rotation
- Battery and charging status
- Persistent night mode and screen-on controls
- Persistent settings via DataStore
- Responsive portrait/landscape layouts

## Requirements
- JDK 17
- Android SDK 34
- Android Studio or Gradle wrapper

## Install the preview on Android
Download the latest APK from the [GitHub Releases page](https://github.com/Sam3360/StandByAndroid/releases), open it on your phone, and approve installation from that source if Android asks.

Preview APKs are debug-signed and intended for testing. Android may require allowing your browser or file manager to install unknown apps. Future builds signed with a different key cannot update this preview in place.

## Windows setup
```powershell
cd C:\Users\samar\Downloads\android_apk
set JAVA_HOME=C:\Users\samar\Downloads\android_apk\jdk17\jdk-17.0.20.1
set GRADLE_USER_HOME=C:\Users\samar\Downloads\android_apk\.gradle
set PATH=%JAVA_HOME%\bin;%PATH%
set ANDROID_HOME=C:\Users\samar\Downloads\android_apk\android-sdk
set ANDROID_SDK_ROOT=C:\Users\samar\Downloads\android_apk\android-sdk
.\gradlew.bat --no-daemon assembleDebug
```

## Gradle native cache fix
If Gradle fails with `Failed to load native library 'native-platform.dll'` or a `native-platform.dll.lock` access error on Windows, remove the stale lock files from the project-local cache and rerun:

```powershell
Remove-Item -Recurse -Force C:\Users\samar\Downloads\android_apk\.gradle\native\*\windows-amd64\*.lock -ErrorAction SilentlyContinue
.\gradlew.bat --no-daemon assembleDebug
```

## Test run
```powershell
.\gradlew.bat --no-daemon testDebugUnitTest
```

## Install with ADB
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
