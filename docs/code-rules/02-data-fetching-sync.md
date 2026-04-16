# Data Fetching and Sync Patterns

## Fetching Stack
Current network stack in `data/network`:

- Retrofit interface: `SpotifishApi`.
- OkHttp client with `AuthInterceptor` and logging interceptor.
- Kotlinx serialization converter via `Json`.
- Base URL from `BuildConfig.SPOTIFISH_BASE_URL` (sourced from `.env`).

Primary files:

- `data/network/NetworkModule.kt`
- `data/network/SpotifishApi.kt`
- `data/network/AuthInterceptor.kt`
- `data/network/dto/*.kt`

## Request Rules
Apply these rules for new API endpoints:

1. Define endpoint in `SpotifishApi`.
2. Use DTO request/response models in `data/network/dto`.
3. Add mapper (`toDomain`) before exposing data outside data layer.
4. Annotate auth-free endpoints with `@NoAuth`.
5. Keep endpoint naming grouped by feature (`auth`, `drive`, `sync`, `songs`, `playlists`, `favorites`).

## Repository Pattern
Repository implementations are remote-first with local augmentation:

- `RemoteMusicRepository`: merges remote songs + local `MediaStore` songs + favorites state.
- `RemoteFavoritesRepository`: optimistic toggle then rollback on failure.
- `RemotePlaylistRepository`: mutation endpoint then `reload()`.
- `RemoteDriveRepository`: folder listing + mapping.

State cache pattern:

- Keep local cache in `MutableStateFlow`.
- Expose `Flow` through repository interface.
- Hydrate cache in `init` using coroutine scope on IO dispatcher.

## Sync Pattern
Drive sync flow in `RemoteMusicRepository`:

1. Trigger backend sync (`runSync`, `resumeSync`, `pauseSync` endpoints).
2. Poll `syncStatus` every 1.5s.
3. Update `DriveSyncState` state flow continuously.
4. Refresh remote songs during polling for progressive UI updates.
5. Stop polling on terminal states (`succeeded`, `failed`, `paused`) or attempt cap.

## Fallback and Merge Rules
Use resilient fallback behavior:

- Search: try backend search first, fallback to local filtering on failure.
- Song list merge: deduplicate by `id`, then sort deterministically.
- DTO list null safety: nullable API lists wrapped with safe getter (`safeItems`).
- Parsing safety: tolerate RFC3339 parse failure by falling back to provided epoch millis.

## Do / Don’t
Do:

- Keep network and DTO handling in data layer only.
- Always map backend models to domain models.
- Use defensive parsing for backend drift and null payloads.
- Keep UI-facing collections sorted deterministically.

Don’t:

- Surface raw Retrofit exceptions directly to UI without context.
- Assume backend list fields are always non-null.
- Hardcode base URL strings in app code.

## Best-Practice Upgrades
Recommended next improvements:

1. Add retry/backoff policy around transient sync/status failures.
2. Introduce typed error model (`sealed class`) instead of plain string messages.
3. Add paging strategy toggle when libraries exceed current single-call assumptions.
