# Compose Component Patterns

## Current Structure
UI implementation currently lives mostly in one large file:

- `app/src/main/java/com/example/musicapp/ui/MusicApp.kt` (~2000 lines)

Sections are grouped by screen and shared components:

- Home
- Search
- Library
- Player
- Settings
- Shared rows/utilities

## Composition Pattern
Route-level composables own ViewModel acquisition; screen-level composables stay parameter-driven.

Pattern used:

1. Route composable gets VM via `hiltViewModel()`.
2. Collect state via `collectAsStateWithLifecycle()`.
3. Pass `state + callbacks` to pure UI composable.

This keeps UI functions previewable/testable by contract.

## Navigation Pattern
Bottom-nav uses single `NavHost` with top-level destinations:

- `home`, `search`, `library`, `player`, `settings`

Rules:

- Keep destination enum as source-of-truth.
- Use `launchSingleTop` + `restoreState` for tab restoration.
- Use no-op transitions for instant tab switch feel.

## UI State and Callback Rules
For composable signatures:

- Accept typed `UiState` object.
- Accept explicit callback lambdas (`onPlaySong`, `onToggleFavorite`, etc.).
- Avoid composables directly accessing repository/network/session objects.

## Theming and Visual Rules
Theme is centralized in `AppTheme.kt`:

- Shared palette constants (`SpotifyGreen`, `SpotifyBackground`, etc.).
- Material 3 dark color scheme.

Rules:

- Reuse theme constants across screens.
- Keep spacing and shape decisions consistent (`RoundedCornerShape`, common paddings).
- Use deterministic visual helpers (`gradientForSong`) for stable UI identity.

## File Organization Rule (Important)
Current monolithic `MusicApp.kt` is functional but hard to scale.

Target split structure:

- `ui/home/HomeScreen.kt`
- `ui/search/SearchScreen.kt`
- `ui/library/LibraryScreen.kt`
- `ui/player/PlayerScreen.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/components/*` for shared rows/cards

Keep route wiring in one navigation file, but move feature UI out.

## Do / Don’t
Do:

- Keep reusable pieces small, private by default, and stateless when possible.
- Keep list rows/components in shared component files.
- Use lifecycle-aware state collection.

Don’t:

- Let feature files exceed maintainability thresholds without splitting.
- Put business logic and data fetching in composables.
- Duplicate style values across screens.

## Best-Practice Upgrades
Recommended next improvements:

1. Split `MusicApp.kt` by feature without changing behavior.
2. Add Compose previews for each screen and key row component.
3. Add UI tests for bottom navigation, mini-player visibility, and settings dialog flows.
