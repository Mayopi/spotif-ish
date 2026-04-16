# Playback and Media3 Patterns

## Playback Boundary
Playback contract is defined in domain:

- `domain/player/PlaybackController.kt`
- `PlaybackState` exposed as `Flow`.

Concrete Media3 implementation lives in player module:

- `player/controller/Media3PlaybackController.kt`
- `player/service/PlaybackService.kt`

Rule: app layer depends on `PlaybackController`, never `ExoPlayer` directly.

## Media3 Controller Pattern
`Media3PlaybackController` responsibilities:

1. Convert domain `Song` list into `MediaItem` queue.
2. Manage ExoPlayer instance and listeners.
3. Publish unified `PlaybackState` via `MutableStateFlow`.
4. Keep periodic position ticker while playing.
5. Start foreground playback service when playback starts.

## Authenticated Stream Pattern
Because ExoPlayer uses its own HTTP stack:

- Access token is pulled from `PlaybackTokenSource` (core contract).
- Authorization header is set on `DefaultHttpDataSource.Factory`.
- Data layer provides adapter `SessionPlaybackTokenSource`.

Rule: player module must not depend on `SessionStore` or auth repository directly.

## Lifecycle Rules
Use these lifecycle guarantees:

- `PlaybackService` creates `MediaSession` with controller’s player.
- Service destroys session and releases controller on teardown.
- Controller cancels position ticker and releases player in `release()`.

Never leak player resources across activity/service lifecycle.

## Queue/State Rules
State publishing expectations:

- `currentSong` derived from queue index.
- `durationMs` fallback to song duration if Exo duration unavailable.
- Repeat/shuffle mapped from Player constants to domain enum.
- Queue changes must keep deterministic current index behavior.

## Do / Don’t
Do:

- Keep transport-specific concerns (headers, datasource) in player impl.
- Publish playback state from one source-of-truth.
- Keep domain playback API stable for UI and tests.

Don’t:

- Expose ExoPlayer APIs through domain.
- Hardcode auth/session knowledge into player module.
- Update UI directly from player module.

## Best-Practice Upgrades
Recommended next improvements:

1. Add player reconnection/recovery behavior for process death.
2. Add audio focus and noisy intent handling policies explicitly.
3. Add playback unit/integration tests using fake `PlaybackTokenSource` and fake media sources.
