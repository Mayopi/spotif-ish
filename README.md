# Spotify Clone Barebone

This repository now contains a multi-module Android baseline aligned to `PRD.md` and `system-design-diagrams.md`.

## Modules

- `app`: Compose UI, navigation, ViewModels, Hilt app entry point
- `domain`: pure Kotlin models, repository contracts, playback contract, use cases
- `data`: repository implementations, MediaStore local source, JSON persistence, settings storage, Drive stub
- `player`: Media3-backed playback controller and playback service
- `core`: shared coroutine dispatcher abstraction

## Current scope

- Clean Architecture + MVVM baseline
- Home, Search, Library, Player, and Settings screens
- Local music scanning via `MediaStore`
- JSON persistence for favorites and playlists
- DataStore-backed settings
- Media3 playback abstraction with service wiring
- Drive integration intentionally stubbed behind `DriveMusicDataSource`

## Next implementation steps

1. Add Gradle wrapper and run the first compile pass.
2. Replace `StubDriveMusicDataSource` with Google Sign-In + Drive REST integration.
3. Add runtime permission handling and folder selection flows.
4. Add notification/media session polish and a richer now playing surface.
5. Add tests for repositories, use cases, and ViewModels.

## Device Development

For this project, the fastest iteration loop on a real Android device is:

- Android Studio `Live Edit` for Jetpack Compose UI changes
- Android Studio `Apply Changes` for many small code and resource edits
- `scripts/dev-connected-device.sh` for CLI install and relaunch on an ADB-connected device

Requirements for `Live Edit`:

- Android Studio Giraffe or newer
- Device or emulator on Android 11 / API 30 or newer
- App launched as a debuggable `debug` build from Android Studio

ADB install and launch:

```bash
./scripts/dev-connected-device.sh
./scripts/dev-connected-device.sh 192.168.18.6:5555
```

Recommended Android Studio setup on macOS:

1. Open the project in Android Studio.
2. Go to `Android Studio > Settings > Editor > Live Edit`.
3. Enable `Live Edit`.
4. Run the `app` configuration on your connected device in `Debug`.
5. Edit Compose UI code while the app is running.

Use `Apply Changes` when Live Edit cannot handle a change, and do a full rerun for changes like method signatures, new classes used by existing call sites, manifest edits, or dependency graph changes.
