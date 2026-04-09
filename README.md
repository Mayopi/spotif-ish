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

