# Dependency Injection and Module Wiring

## DI Framework
Project uses Hilt across modules.

Main entry points:

- `@HiltAndroidApp`: `MusicApplication`
- `@AndroidEntryPoint`: `MainActivity`, `PlaybackService`
- `@HiltViewModel`: all ViewModels

## Binding Pattern
Data module uses interface binding module:

- `DataBindingsModule` with `@Binds` for repository/data-source/token-source interfaces.
- `DataModule` with `@Provides` for singleton utility instances (`DispatchersProvider`).

Player module mirrors this with `PlayerModule` binding `PlaybackController` to `Media3PlaybackController`.

## Rules for New Dependencies
Use these rules for DI additions:

1. Bind interfaces to impl with `@Binds` when no custom construction needed.
2. Use `@Provides` for factory logic or third-party builder creation.
3. Install app-wide singletons in `SingletonComponent`.
4. Prefer constructor injection for concrete classes.
5. Avoid direct use of service locators or manual singleton objects.

## Circular Dependency Handling
Current auth/network cycle is resolved with `Lazy<T>`:

- `AuthInterceptor` uses `Lazy<AuthRepository>`.
- `AuthRepository` uses `Lazy<SpotifishApi>`.

Rule: when unavoidable cycles exist, break with `Lazy` and document why.

## Module Ownership Rule
Place DI modules in module that owns implementation:

- Network and repositories bound in `data`.
- Playback implementation bound in `player`.
- App should wire UI-specific dependencies only.

## Do / Don’t
Do:

- Keep bindings near implementation module.
- Expose abstractions at domain/core boundaries.
- Use clear naming (`bindX`, `provideY`) for readability.

Don’t:

- Bind concrete type to itself with unnecessary modules.
- Inject framework-heavy concrete classes into domain.
- Hide complex construction logic in random call sites.

## Best-Practice Upgrades
Recommended next improvements:

1. Add qualifier annotations if multiple implementations emerge (e.g., local/remote repos).
2. Add test modules for fake bindings in unit/integration tests.
3. Add DI graph verification test for missing/duplicate bindings.
