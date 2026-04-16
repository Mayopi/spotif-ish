# Error Handling and Resilience Patterns

## Current Error Strategy
Project uses pragmatic fail-soft handling:

- `runCatching` for risky operations in repositories/viewmodels.
- Optimistic UI updates with rollback on API failure (favorites).
- Fallback query behavior (search local data when network fails).
- User-visible one-off messages via `SettingsEvent.Message`.

## Repository Rules
For repository-level failures:

1. Keep in-memory state consistent first.
2. Revert optimistic state when backend mutation fails.
3. Avoid crashing flow pipelines on intermittent network errors.
4. Return sensible fallback data when safe (for read operations).

Examples:

- `RemoteFavoritesRepository.toggleFavorite()`: optimistic update + rollback.
- `RemoteMusicRepository.search()`: remote-first, local fallback.
- `RemoteMusicRepository.enqueueDriveLibraryRefresh()`: publish `DriveSyncState.lastError`.

## ViewModel Rules
For UI-facing actions:

1. Guard duplicate taps with working flags where needed.
2. Wrap async calls in `runCatching`.
3. Emit user-readable message on failure.
4. Always reset loading/working flags in success and failure paths.

## Persistence and Decode Safety
Storage/parsing resilience already used:

- `DefaultSettingsRepository.observeSettings()` catches DataStore read exceptions and emits empty prefs.
- DTOs tolerate backend null lists and parse fallbacks.
- Session store has encrypted storage fallback path when keystore fails.

Rule: fail to safe defaults; never leave app in partially initialized state silently.

## Logging Rule
Use targeted logs for debugging unexpected backend shape/runtime behavior:

- Existing pattern: `Log.w` in sync polling when fetch fails.

Guideline:

- Log technical context for developers.
- Show concise user-safe message to UI.
- Never log secrets/tokens.

## Do / Don’t
Do:

- Prefer explicit error channels in state or events.
- Keep failures observable for both UI and debugging.
- Preserve app usability under partial backend outages.

Don’t:

- Swallow exceptions with no state update or logging.
- Mix technical exceptions directly into user text everywhere.
- Block UI indefinitely by forgetting loading-state reset.

## Best-Practice Upgrades
Recommended next improvements:

1. Introduce typed domain error model (`Network`, `Unauthorized`, `Validation`, `Unknown`).
2. Add centralized error-to-message mapper for consistent UX copy.
3. Add structured retry policy (backoff/jitter) for sync and transient network operations.
