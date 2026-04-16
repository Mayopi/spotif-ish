# Testing and Quality Gates

## Current State
Current repository has no test sources (`test` / `androidTest`) and no static quality plugins (detekt/ktlint/spotless) configured yet.

This means behavior is verified manually only.

## Minimum Test Strategy
Adopt this baseline in phases.

### Phase 1: Domain and ViewModel Unit Tests
Add pure Kotlin/JVM tests for:

1. Use case pass-through behavior and orchestration (`RefreshLibrariesUseCase` ordering).
2. ViewModel flow transformations:
- `SearchViewModel` debounce + result updates.
- `LibraryViewModel` grouping/sorting.
- `PlayerViewModel` mapping from `PlaybackState` to `PlayerUiState`.
3. Settings event emission logic (`LaunchDriveConsent`, message events).

### Phase 2: Data Layer Tests
Add tests with fake API/data sources for:

1. `RemoteMusicRepository` merge/dedup/sort rules.
2. Sync poll termination behavior and error state updates.
3. Favorites optimistic toggle rollback.
4. Playlist reload-after-mutation contract.
5. DTO mapping safety (`safeItems`, date parsing fallback, URL composition).

### Phase 3: Integration/UI Tests
Add targeted Android tests for:

1. Auth gate screen switch (signed-out -> signed-in path).
2. Bottom navigation route restoration.
3. Mini-player visibility behavior.
4. Settings folder picker flow and sync controls.

## Quality Gate Rules
Before merge, require:

1. `./gradlew test` passes.
2. `./gradlew :app:assembleDebug` passes.
3. Lint/static checks pass (after introducing tools).
4. New features include tests for critical branches.

## Static Analysis Recommendations
Add and enforce:

- Kotlin style: ktlint or spotless.
- Static bug checks: detekt.
- Android lint baseline for app module.

Start non-blocking, then make blocking in CI.

## Testability Patterns to Preserve
Keep these existing test-friendly patterns:

- Repository contracts in domain.
- Dispatcher abstraction (`DispatchersProvider`).
- Playback abstraction (`PlaybackController`).
- Pure mapper functions in DTO files.
- StateFlow-based ViewModel outputs.

## Do / Don’t
Do:

- Test behavior rules (merge, sorting, fallback, retries), not just line coverage.
- Use fakes/stubs for API and playback interfaces.
- Add regression tests when fixing bugs.

Don’t:

- Add UI-only tests for business logic that belongs in unit tests.
- Depend on live backend for routine test runs.
- Merge large refactors without baseline test protection.
