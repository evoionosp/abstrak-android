# Best Practices

## Architecture

- Keep `domain` as pure Kotlin. No `android.*` imports and no AndroidX runtime dependencies unless there is a deliberate, documented exception.
- Keep repository interfaces in domain and implementations in data.
- Keep the data layer as the single source of truth for app data.
- Do not inject Room, Retrofit, DataStore, or platform services directly into Compose UI or screen ViewModels unless there is a very strong reason.
- Prefer feature-oriented modules over one large presentation module as the app grows.
- Keep `app` thin. Avoid putting business logic in `Application` or activities.

## State Modeling

- Model auth state, bootstrap state, sync state, and feature UI state separately.
- Do not use one boolean to represent multiple unrelated states.
- Treat startup as a state machine, not a sequence of ad hoc checks.
- Prefer explicit sealed models when state transitions matter.
- Expose screen state as read-only `StateFlow` backed by private `MutableStateFlow`.
- Use `SharedFlow` for one-off events such as navigation and transient UI actions.
- Show every snackbar through the injected `SnackbarController` (`ui.message`; composables use `LocalSnackbarController`) with a type: `error` for failures, `warning` (orange) for things the user should know but didn't fail, `success` (green) for completed actions, `info` otherwise. Text is `UiText`, so ViewModels never hold a Context. The app shell shows them all in `AbstrakSnackbar`. Layers below presentation report events (e.g. `PlaybackConnection.notices`) and a ViewModel maps them to messages.
- Do not expose `MutableStateFlow` or `MutableSharedFlow` publicly.

## Navigation

- Use Navigation Compose as the default navigation approach for all new multi-screen work.
- Keep route arguments small and stable.
- Pass IDs through navigation, not large objects.
- Let screens and ViewModels resolve data from IDs rather than passing models through routes.

## Data and Sync

- Design local DB schema for incremental evolution.
- Until release, keep Room at version 1 and change the schema in place — no migrations, migration tests or exported schemas (see `AGENTS.md`). From release on, every schema change ships a migration from the released schema.
- Prefer offline-first reads backed by local storage where practical.
- Use stale-while-revalidate behavior where it fits the product: show cached data quickly, then refresh in background.
- Keep sync metadata explicit: last synced, stale state, failure reason, retry policy.
- Separate catalog sync from media download sync.
- Main-safe suspend functions are required in data and domain APIs.
- Prefer `Flow` for observable data changes and `suspend` functions for one-shot operations.

## Security

- Do not log tokens, credentials, or authenticated response bodies.
- Store session secrets in encrypted storage.
- Keep auth header handling centralized.
- Validate and normalize base URLs consistently.

## Playback and Download Readiness

- Build playback around Media3, not UI-specific abstractions.
- Build download orchestration as durable background work.
- Never drop a server write that failed offline: queue it locally and deliver it from WorkManager (see `SavePlaybackProgressUseCase` and `SyncPendingProgressUseCase`). When the device and server disagree about where a book is, merge with `ProgressMerge` (furthest along wins) instead of overwriting either side.
- Persist playback position and queue state centrally.
- Treat local/offline media as first-class data, not a later patch.

## ViewModels and Use Cases

- ViewModels should coordinate UI state, not contain networking or storage details.
- Keep use cases small and focused.
- Prefer one clear use case over leaking repository logic into presentation.
- If logic is reused by multiple surfaces, move it below presentation early.
- Launch ViewModel work in `viewModelScope`.
- Do not expose suspend functions from ViewModels to UI callers.

## UI

- Keep UI composables stateless where practical.
- Hoist screen state into ViewModels.
- Use lifecycle-aware state collection.
- Use `collectAsStateWithLifecycle()` for screen state in Compose.
- Keep one-off event collection out of recomposition-sensitive code paths.
- Avoid making phone UI assumptions that would block tablet, Auto, or Wear reuse.

### Structure

- `presentation` has two shared layers. `designsystem` holds the theme and generic components and knows no domain types. `ui` holds domain-aware shared pieces (covers, chapters, downloads, catalog cards, formatters, events). Features build on both and never import each other; `navigation` is the shell that wires them together. `PackageDependencyTest` enforces this, with the few known exceptions listed in it.
- Promote a component to a shared layer only when a second feature needs it (rule of two). Until then it stays in its feature.
- One destination is an `XxxRoute` (ViewModel, collected state, dialogs, mapping intents to ViewModel methods) plus a stateless `XxxScreen`. A screen with more than about six callbacks takes one sealed `XxxAction` instead.
- Sections go in the feature's `components/`, each under about 200 lines. A private helper lives next to its only caller.

### Components

- Take `modifier: Modifier = Modifier` as the first optional parameter and apply it to the root.
- Get colors and sizes from an `XxxDefaults` object, never inject a ViewModel, and never read a feature's CompositionLocal.
- Read theme roles from `MaterialTheme`. A surface with its own palette scopes a `MaterialTheme` around itself (the player's `PlayerTheme`) instead of adding color locals.
- Read values that change every frame (drag offsets, animations) in layout or draw lambdas such as `Modifier.layout {}`, `offset {}` and `graphicsLayer {}`, and turn thresholds into booleans with `derivedStateOf`, so a gesture doesn't recompose the subtree.
- Give lazy list items a `contentType`, and a `key` only when the items have a stable identity.
- Keep the math behind gestures and morphs in pure functions with unit tests (for example `sheetMorphGeometry`, `nowPlayingUiState`).

### Accessibility and strings

- Strings come from resources, with plurals for counts.
- Every actionable icon has a content description, and custom clickables declare a `Role`.
- An image next to a visible title gets a `null` content description, so TalkBack doesn't read the title twice.

### Previews

- There are none yet. When they are added (with `ui-tooling` as a debug dependency and shared sample data), every component gets a light and dark preview and every screen a preview per state: loading, empty, content and error.

## Coroutines

- Inject dispatchers instead of hardcoding `Dispatchers.IO` or `Dispatchers.Default` inside classes.
- Never use `GlobalScope`.
- Do not swallow `CancellationException` inside broad exception handlers.
- Use structured concurrency for parallel work.
- Use an application-scoped coroutine scope only for truly app-lifetime work, and prefer durable background APIs when the work must survive process death.

## Networking

- Keep Retrofit service definitions declarative. Use `@Path`, `@Query`, and `@Body` instead of manual URL construction when possible.
- Handle HTTP and connectivity failures in repositories so UI state stays simple.
- Use `Response<T>` when status code handling matters.
- Keep auth header creation centralized rather than repeating `Bearer` formatting throughout UI or feature code.

## Testing

- Unit test domain use cases first.
- Add repository tests around cache and sync behavior.
- Add tests for startup state decisions and selection/sync edge cases.
- When playback and downloads are introduced, treat those areas as high-test-priority.
- Prefer testing emitted state and events over implementation details.
- Use coroutine test utilities such as `runTest` and test dispatchers.
- Use Flow testing utilities such as Turbine where helpful.
- Use MockK or similarly capable mocking only where fakes are not the clearer choice.

## Project Hygiene

- Keep README and `.agents` docs updated as the architecture evolves.
- Record major architectural decisions before large refactors.
- Prefer small, targeted changes over broad rewrites without milestones.
