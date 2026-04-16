# ViewModel and StateFlow Patterns

## Core Pattern
ViewModels in this project follow unidirectional state flow:

- Input: UI callbacks (`fun onAction(...)`).
- Work: use case/repository/playback calls in `viewModelScope`.
- Output: immutable UI state (`StateFlow<UiState>`).

Primary files:

- `ui/home/HomeViewModel.kt`
- `ui/search/SearchViewModel.kt`
- `ui/library/LibraryViewModel.kt`
- `ui/player/PlayerViewModel.kt`
- `ui/settings/SettingsViewModel.kt`
- `ui/auth/SignInViewModel.kt`

## State Construction Rules
Use these rules for new viewmodels:

1. Define one immutable `data class` per screen state.
2. Build state from flow pipelines (`map`, `combine`, `stateIn`).
3. Use `SharingStarted.WhileSubscribed(5_000)` for hot state sharing.
4. Keep ephemeral one-off events out of `UiState` (use `SharedFlow` events).

## Action Handling Rules
For user actions:

- Launch suspend work via `viewModelScope.launch`.
- Gate re-entrant operations with working flags when needed (`isWorking`).
- Use `runCatching` for UI-safe failure handling and message emission.
- Keep action methods small and intention-revealing.

## Query/Search Pattern
Search screen pattern:

- Keep raw query in `MutableStateFlow`.
- Emit debounced query (`300ms`) using `MutableSharedFlow`.
- Execute backend/local search via `flatMapLatest`.
- Combine current query + results into `SearchUiState`.

This avoids stale results and reduces request spam.

## Event Pattern
Settings uses event channel for one-off effects:

- Toast messages.
- Drive consent launcher.
- Sign-out completion.

Rule: side effects requiring activity/launcher integration should use event flow, not persistent state booleans.

## Do / Don’t
Do:

- Keep UI state immutable and serializable-by-concept.
- Keep flow pipelines in ViewModel, not in composables.
- Keep domain logic in use cases/repositories.

Don’t:

- Store `Context` in ViewModel unless unavoidable platform bridge.
- Expose mutable flow types (`MutableStateFlow`) directly to UI.
- Trigger navigation/toasts directly inside repository layer.

## Best-Practice Upgrades
Recommended next improvements:

1. Standardize loading/error wrappers across all screens.
2. Extract common `launchWithUiState` helper for repetitive `runCatching` blocks.
3. Add unit tests for flow transformations (debounce, combine, event emissions).
