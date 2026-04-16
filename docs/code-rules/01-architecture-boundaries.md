# Architecture and Module Boundaries

## Scope
This project uses a multi-module Clean Architecture baseline:

- `app`: Compose UI, navigation, ViewModels, Hilt Android entry points.
- `domain`: entities, repository contracts, playback contract, use cases.
- `data`: repository implementations, local scan, network client, session/settings persistence.
- `player`: Media3 playback implementation + foreground service.
- `core`: shared abstractions (`DispatchersProvider`, `PlaybackTokenSource`).

See: `settings.gradle.kts`, `README.md`.

## Dependency Rules
Use these module dependency constraints:

1. `domain` must stay Android-free and framework-light.
2. `data` implements `domain` repository contracts, never the reverse.
3. `app` consumes use cases/contracts, not low-level infra directly (except deliberate exceptions such as auth screen flows).
4. `player` depends on `domain`/`core` contracts and owns Media3 internals.
5. `core` contains minimal cross-cutting interfaces only.

Current dependency graph:

- `app -> core, domain, data, player`
- `data -> core, domain`
- `player -> core, domain`
- `domain -> core`

## Layering Pattern
Follow this runtime chain:

`UI (Compose) -> ViewModel -> UseCase -> Repository interface (domain) -> Repository implementation (data/player)`

Use cases here are intentionally thin command/query wrappers (`operator fun invoke`), with orchestration mostly in repositories and ViewModels.

## Ownership Rules
Use ownership boundaries consistently:

- Domain models live in `domain/model` and are consumed everywhere.
- Network DTOs live in `data/network/dto` and are mapped to domain models before leaving data layer.
- Platform APIs (`MediaStore`, `EncryptedSharedPreferences`, `DataStore`, `Media3`) stay outside domain.
- Token/session details stay in data layer; other modules access through contracts.

## Do / Don’t
Do:

- Add new business capability by first defining domain contract + use case.
- Keep UI modules free of networking and storage details.
- Add small `core` interfaces when cross-module decoupling is required.

Don’t:

- Put Android framework types in `domain`.
- Return DTOs from repositories/use cases.
- Bypass repository contracts from ViewModel for convenience.

## Best-Practice Upgrades
Recommended next improvements:

1. Split giant `MusicApp.kt` into feature files (`home`, `search`, `library`, `player`, `settings`) while preserving same dependency boundaries.
2. Move remaining direct API calls in `SettingsViewModel` behind a domain/data repository contract for stricter layering consistency.
3. Add architecture tests or lint checks to detect forbidden module imports.
