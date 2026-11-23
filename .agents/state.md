# Current State

## Summary

Abstrak now has a materially better foundation than the initial prototype, and it has crossed from a catalog-only app into one with real audio playback. It supports login, server checks, library discovery, local caching, selected-library persistence through local DB state, listing audiobook items for the selected library, a detail flow with server-backed detail/chapter/track loading cached locally, and Media3-based playback of selected audiobooks.

The remaining gap to a full playback product is not the player itself but the pieces around it, chiefly a queue model/UI (see below). Listening works offline: downloaded books play from the device and their progress syncs once the device is back online.

## Implemented Features

- Enter custom Audiobookshelf server URL.
- Validate server reachability and health.
- Authenticate with username and password.
- Persist login session locally.
- Fetch audiobook libraries.
- Select one library.
- Fetch audiobook list for the selected library.
- Cache libraries and audiobooks in Room.
- Sync server-backed Continue Listening from the Audiobookshelf items-in-progress API and cache it locally, including per-book playback positions (hydrated per item when the list response omits them).
- Show per-book time left at 1x on Continue Listening cards, falling back to the author line when position or duration is unknown.
- Keep Continue Listening live without a refresh, offline too: every progress save records the book's position in the local cache before the server save, moving it to the front of its library's shelf; a finished book leaves the shelf. The next server refresh still replaces the shelf.
- Offline-first progress. A save the server can't take (offline, unreachable, failing) is queued in Room (`pending_progress`, one row per book, its latest save) and a unique WorkManager job (`ProgressSyncWorker`: any network, backoff from the 15 s interval) delivers it once the device is online, even after the app is killed; a force stop holds it until the next launch, and app start schedules a sync for anything left queued. The merge rule (`ProgressMerge`) is most recently updated wins, a tie keeping the server's, comparing each record's own `lastUpdate`: every save carries the time the player read its position, which the queue row keeps and the save sends to the server as `lastUpdate`, so listening times are compared rather than upload times. Queued progress is sent only when more recent than the server's; otherwise the server's is pulled into Continue Listening and, through `PulledProgressEvents`, moves the loaded book to it, forwards or back, but only while the book is paused and only when the pulled record is newer than this device's last listen, seek or save of it. While a book has progress queued, each further save of it merges the same way instead of overwriting; saves made online all along go straight to the server, so going back in a book on purpose sticks. Resume takes the more recently updated of the server's and the queued progress; with the server unreachable, the device's own record (queued, else the Continue Listening position, else the position the caller showed). A Continue Listening refresh keeps the more recently updated of queued and server progress (a server row whose position failed to load still competes on its update time), including books only this device has listened to, and drops a book finished on this device when that finish is the newer record. Logout wipes the queue with the rest of the account's data.
- One 15 s sync interval (`ProgressSyncPolicy.INTERVAL_SECONDS`): a book saves nothing until it has played 15 s since it loaded, so one started by accident and stopped again never reaches the shelf or the server (a just-started book missing from the shelf is expected). After that it saves every 15 s while playing and on pause, seek, chapter jumps, switching books and stop; playing to the end always saves. Saves capture the position synchronously, so a stop no longer drops its final save.
- Log out through a single ordered flow: stop playback (flushing progress first), clear the session, wipe catalog/cover caches and downloads, keeping the remembered server URL and all app settings.
- Tell audiobooks from ebooks: a library item with no audio to play (minified lists count its tracks, expanded items list them, and an ebook alone has no duration) is cached with `hasAudio = false`. Continue Listening never lists those, even when the server reports reading progress.
- Library tab: an Audiobooks section ("N Audiobooks", with the tiles/list toggle), then a Books section ("N Books") for books without audio, grayed out (grayscale cover, faded text; "Ebook" in place of a duration in the list). Tapping such a book there, on a Home shelf or in Search shows a short warning snackbar that only audiobooks are supported (`rememberCatalogBookClick`).
- Show the current library's sync status as a centered chip under the Library header (out of the library pill), counting the time since its last successful sync ("Synced 3 hours ago"): green with a done cloud within a day, amber with a cloud when the daily sync is overdue, red with a crossed-out cloud after two days or before any sync, and a syncing cloud on the card color while a sync runs; in offline mode it reads "Offline · …". Tapping it opens the Library sync sheet: the status with the exact last sync time, the daily schedule, and the switch to use the app offline. Header pills (library picker, search field, Downloads selection bar) are as tall as the round settings button beside them.
- Daily library sync (`LibrarySyncPolicy`): a library's books, and the library list with them, sync on their own at most once every 24 h, when a trigger finds the last sync a day old or older: app start, coming back to the app, the network coming back, a library switch, and Search (each library). Pull to refresh syncs whenever. `SyncLibraryBooksUseCase` runs syncs one at a time and checks they are due when their turn comes, so triggers firing together sync once. Startup opens a library that has synced before straight from Room; only a first start (or one after a failed first sync) waits on the server. Continue Listening still refreshes on every one of those triggers, since it follows listening on other devices. A sync cut short by the process dying is settled when the database next opens (`InterruptedSyncReset`), so the chip doesn't show syncing until the next daily sync.
- Offline mode (the Library sync sheet's switch, `AppSettings.useOfflineMode`): the app acts as if there were no network, even with one. `ConnectivityObserver` reports offline (`OfflineAwareConnectivityObserver` over the device's network), so every offline path applies: syncs skip, progress saves queue, and only downloaded tracks play. `OfflineModeInterceptor` fails every request before it leaves the device, covering callers that don't check connectivity (cover art, streaming, downloads, token refresh); it throws an `IOException`, so a token refresh counts as retryable and the session is kept. Downloads pause (their batches are cancelled with the tracks still queued, so no retries are spent) and resume when offline mode is turned off, which also delivers queued progress at once (`ProgressSyncScheduler.scheduleNow`) and runs the daily sync if due. Pull to refresh says offline mode is on instead of trying. Signing out turns offline mode off (`FollowOfflineModeUseCase`), as signing back in needs the server.
- Open an audiobook detail page from the catalog grid, with chapters and cached track metadata. Its play button reads Continue Listening when the book has a position to continue from (the live one while it is the loaded book, else the saved one: the Continue Listening cache's at once and offline, then the server's) and Start Listening otherwise, including for a finished book, which starts over.
- Fetch and display audiobook chapters from the Audiobookshelf item-detail API.
- Cache expanded audiobook detail, chapters, and ordered remote tracks in Room as a playback-ready catalog model.
- Real audio playback support using AndroidX Media3 (ExoPlayer + MediaSession), with the core extracted into the `:playback` module.
- Background playback with system media controls and notification.
- Real-time playback synchronization between UI surfaces (Bar, Overlay, Screen).
- Playing a book from Home's Continue Listening or from its Book Details page (the play button, a chapter, a confirmed chapter switch) opens the full player at once, on the book still loading; the shell clears the expanded flag once nothing plays, so a book that couldn't start never leaves the sheet stuck open.
- Playing a book shows it in the player at once (`PlaybackState.isPreparing`) while its detail and resume position load; the book playing before pauses right away. A newer request or a stop cancels one still preparing, and a book that can't play falls back to the book loaded before, saying why in an error snackbar (`PlaybackNotice`, which `NowPlayingViewModel` turns into snackbars): offline with that part not downloaded (offline, a book starts only from a track on the device), the server unreachable or failing, no audio, or gone from the server. Playback that stops on a player error says why the same way, and play after an error prepares the player again, so it retries. Play/pause (mini and full) shows a spinner while the book prepares or buffers (`isBuffering`: buffering while it would play) and stays tappable, pausing a book that buffers.
- Server-side playback progress sync and resume-from-saved-position.
- Chapter navigation and playback speed control (0.5x–4x).
- The full player's transport sizes its buttons to the row: 12 dp always between buttons; on narrow phones the seek and play/pause buttons shrink together (never below the 48 dp touch target) instead of touching.
- Basic settings and appearance screens.
- Download books and chapters as durable WorkManager work (one unique work per book), tracked per track in Room; cancel and delete from Book Details.
- Downloads tab: every book with download rows, across libraries, with its size on disk (partial files included), under a plain "Downloads" heading with no total size. A row menu deletes one book; a long press starts multi-select (select all, delete). Both confirm through the shared delete dialog, which names the book when there is one and counts them otherwise ("Delete 3 downloads?"). Deleting a book that is still downloading cancels it and removes its partial files; a playing book keeps streaming. The header's settings button opens Downloads & Storage.
- Search tab: searches the cached catalog by title and author (case- and accent-insensitive, every word must match, title matches first), across all libraries or only the selected one per the "Search across all libraries" preference. Queries are normalized: folded letters and curly quotes, with optional marks stripped for Latin, Greek, Cyrillic, Arabic and Hebrew. While the list is at the top it stays there as new results arrive above; a new query starts at the top, and a saved scroll position survives a tab switch or rotation. While it covers all libraries, it refreshes every library but the selected one when it opens and whenever the preference is turned back on.
- Root tabs: Home, Library, Search, Downloads (the placeholder search FAB is gone).
- Downloads & Storage settings page, opened from Preferences or the Downloads tab's settings button (back returns there): the Downloads group (Wi-Fi-only and auto-delete download toggles) first, then the Manage storage group with a storage summary whose bar splits Abstrak's downloads, its cache and the device's free space, a divider, and clearing all downloaded media.

## Current Architecture Assessment

### Good Foundations

- Separate modules already exist: `app`, `presentation`, `domain`, `data`, `playback`.
- Domain layer defines repository interfaces and use cases.
- Data layer owns Retrofit and Room implementations.
- Compose UI uses screen ViewModels and `StateFlow`.
- The app already uses local cache instead of depending entirely on the network.
- Root navigation now uses Navigation Compose.
- Session storage is encrypted.
- Hardcoded dispatcher usage has been replaced in the main repository and coordinator paths.
- One snackbar for the whole app: `SnackbarController` (a singleton ViewModels inject; composables use `LocalSnackbarController`) queues typed messages and `AbstrakApp` shows them in `AbstrakSnackbar`, above the player so they stay visible with the full player open, clearing the mini player and navigation bar. Types follow Material 3: info uses the standard inverse surface, error the error role, and success (green) and warning (orange) are Material custom color roles (`AbstrakExtendedColors`: seeds harmonized with the theme's primary, dynamic or not, then the error role's tones), each with an icon TalkBack names. Refresh failures, login and server errors, failed deletes, and a book that couldn't play or stopped on an error are errors; offline, no audiobook library, an expired session and the ebook tap are warnings; deleted downloads and a cleared download store are successes; the player moving a book to where another device got to is info.
- Playback is service-backed via a Media3 `MediaLibraryService`, not embedded in a screen ViewModel.

### Important Weaknesses

#### 1. Playback works offline too, but is not yet complete

Playback is implemented with Media3 (ExoPlayer + `MediaLibrarySession`), with the core extracted into the `:playback` module (`PlaybackService`, `PlaybackConnection`, `PlaybackState`) and exposed to the now-playing surfaces. Progress syncs with the Audiobookshelf server: the resume position merges server and queued device progress on play, and progress is saved once a book has played 15 s, then every 15 s while playing plus on pause, seek, chapter jump, book switch, stop and playback end; saves made offline queue and sync in the background (see Implemented Features). Downloaded tracks play from the device. Chapter navigation and speed control (0.5x–4x) work. Still missing:

- No playback queue/queue-source model or queue UI (tracks play in order; no queue management).

Impact:

- Resume across sessions and devices works online and offline; offline listening reaches the server once the device is back online.
- The player is usable but not yet a complete listening experience.

#### 2. Downloads play from the device

Books and chapters download through WorkManager into app storage, with per-track rows in Room, and the Downloads tab manages them. Playback reads every downloaded track from its file (`ResolveLocalTracksUseCase` over `DownloadRepository.localFileForTrack`; the player's `DefaultDataSource` reads file URIs and sends the rest to the authorized HTTP stack) and streams the others. Logout deletes downloads through `DownloadStore`.

Impact:

- A downloaded book plays offline; a partly downloaded one plays its downloaded tracks.
- Deleting a download of the loaded book never breaks playback: the player watches the book's download rows, swaps a deleted track behind or ahead of the playhead to streaming in place (`replaceMediaItem`), and reloads at the same spot when the deleted track is the one playing. Auto-delete of finished chapters only ever removes tracks behind the playhead.

#### 3. Catalog detail data is cached, but still needs richer playback semantics

The audiobook detail screen observes a Room-backed expanded detail model and refreshes item detail from the server. Chapters and ordered remote tracks are cached locally, which gives playback a stable catalog source.

Impact:

- Playback resolves cached detail and remote track URLs from the repository layer.
- The model still needs bookmarks.

#### 3.5 Session lifetime is now handled centrally

Audiobookshelf 2.26.0+ issues short-lived JWT access tokens with a longer-lived refresh token.
Token rotation is centralised in `SessionRefreshCoordinator` (domain) and driven from an OkHttp
`Authenticator` (`data.network.TokenAuthenticator`) plus a proactive `GetValidSessionUseCase`
that rotates an expired token before a request rather than after it 401s.

Rules that must be preserved:

- Only the coordinator clears the session, and only when the server definitively rejects the
  refresh token. Transient failures must never log the user out.
- The authenticated OkHttp client and the auth client (login / server check / refresh) are
  separate on purpose. See `NetworkModule`.
- Cache fallbacks must not absorb `CatalogError.AUTH`; serving stale content over a dead session
  is what made an expired token look like a working signed-in app.

#### 4. Catalog sync is still app-process scoped

Catalog refresh is still coordinated from `Application` with an application-scoped coroutine pattern. Downloads and progress sync, which must survive the process, already run as WorkManager work.

Impact:

- Catalog refresh has no durable retries or process-death-safe work; a sync that is due reruns when the app next starts, comes back to the foreground or comes back online.

#### 5. The module layout is cleaner, but still transitional

Session persistence lives in `:data` (`SessionDataStore`, encrypted) with contracts in `:domain/session`. Playback core is already extracted into `:playback`, with now-playing UI in `presentation.player`, but the app still uses broad modules like `presentation` and `data` rather than feature-oriented or capability-oriented splits. Inside `presentation`, packages are layered (`designsystem`, `ui`, features, `navigation`) and `PackageDependencyTest` enforces the boundaries, so feature modules can be split out later without untangling imports first.

Impact:

- Acceptable for the current project size.
- No further playback extraction is needed before Auto and Wear; new surfaces should reuse `:playback` plus the domain playback rules.

## What Was Improved Recently

- Startup/auth/catalog state separation.
- Dedicated authenticated catalog error route instead of routing failures back to auth.
- Root Navigation Compose graph.
- Encrypted session storage (`SessionDataStore` in `:data`).
- Safer debug logging for authenticated traffic.
- Injected dispatchers and better coroutine cancellation handling.
- `SharedFlow` for transient UI messages in key screens.
- `ConnectivityObserver` moved out of `data` contract ownership.
- Audiobook detail overlay (glance mode) with no detail route in the nav graph.
- Chapter loading from server item detail.
- Room-backed expanded audiobook detail cache with chapters and ordered remote tracks.
- Server-backed Continue Listening shelf sync cached locally for Room-first home reads.
- Presentation UI files have been split into screen-specific and component-specific Kotlin files for home/catalog, detail, and now-playing surfaces.
- App navigation, route definitions, transition helpers, root bottom navigation, and now-playing scaffold state have been extracted from `MainActivity` into a dedicated `presentation.navigation` package.
- Real Media3 playback wired behind the detail `Play` action, with a background media session service and now-playing surfaces synchronized through shared playback state.
- Continuous mini-bar-to-full-sheet transition: one hoisted bidirectional `SheetDragState` (offset + expansion fraction with a unit-tested settle policy) drives a unified sheet that morphs from the mini card (16dp corners) to full-bleed, with a staged mini/full handoff and parallax; tap-to-expand/minimize behavior is preserved. Both surfaces share one cover-tinted background; the mini shows the chapter title over "book - author" with 15/30 seek buttons; the nav bar slides away with expansion.
- Centralised access-token refresh: `SessionRefreshCoordinator`, an OkHttp `Authenticator`, JWT
  expiry tracking on the stored session, and proactive rotation via `GetValidSessionUseCase`.
- The login form remembers the last server URL and protocol across logout, so signing back in
  does not start from a blank field.
- Logout is a single ordered, fail-open flow (`LogoutUserUseCase` in domain, Hilt-bound
  cleaners per module): stop playback with an awaited progress flush, clear the session,
  delete downloads, clear Coil cover caches, wipe the
  Room catalog. All logout paths (Settings, expired-session auto-logout, splash) run it;
  Settings shows a Material3 confirm dialog with logout progress. The remembered server
  URL and all app settings survive.
- Continue Listening cards show time left at 1x (`"5h 30m left"`), computed from cached
  per-book positions; grid cover art is square with 4dp rounded corners.
- Presentation layering (plan `2026-10-26-compose-component-modularization.md`): a
  `designsystem` package (theme tokens, list items, headers, sheets, dialogs, pickers) and a
  domain-aware `ui` package (covers, chapters, downloads, catalog cards, formatters) shared by
  the features. Every destination is a route plus a stateless screen; the player runs under a
  scoped `PlayerTheme` and its sheet morph is a pure, tested function; Book Details reads the
  cache before refreshing, and Continue listening always plays.
- Pre-release schema policy (`AGENTS.md`): Room stays at version 1 and changes in place —
  no migrations, migration tests or exported schemas. `fallbackToDestructiveMigration`
  covers version mismatches, and `ResettingOpenHelperFactory` (wrapping Room's open
  helper) starts the database over when Room refuses the file: a schema changed in place
  at the same version (Room's identity check), a schema it can't validate, or a migration
  that fails or is missing. No reinstall needed; the catalog refetches, and the startup
  storage sweep removes the downloads whose rows went with it. Real migrations start at
  release, from the shipped schema; revisit the factory then.
- Downloads and Search tabs. Deleting a book's download now removes its rows before stopping
  its work (as a cancel does), so a racing worker finds its row gone and drops its temp file.
  Search reads Room only; the "Search across all libraries" preference lives in app settings
  next to the appearance settings. No schema version change.
- Downloads have no paused state (milestone 4's model minus paused). Cancelling a book or a
  chapter deletes its unfinished rows and `.part` files before stopping any work, so a racing
  worker finds its row gone and skips; finished tracks stay on device. The notification
  action is Cancel too.
- Review pass on the Downloads and Search work: one downloads API (the parallel summaries
  path is gone), one delete dialog, one byte formatter, `AudiobookListCard` built on
  `AudiobookListRow`, a shared dropdown menu style, shared root-tab chrome
  (`RootScreenSurface`, `RootHeaderDefaults`), and the tab order defined once
  (`rootNavItems`). `StorageMeter` (`:data`) measures the cache once per collection and the
  device's free space for `observeStorageUsage`. Download deletes run on the application
  scope and a multi-select delete starts them all at once, so leaving the screen mid-delete
  can't leave books or files behind. The theme defines the M3 fixed color roles.
  `:app` lint passes again: WorkManager's foreground service type lives in `:data`'s
  manifest. Shared test fakes live in `:domain`'s testFixtures, which `:presentation`'s
  tests use too.
- Failed downloads no longer overstate their size: when a corrupt resume discards the temp file, the row's `bytesDownloaded` is set to 0 in the same update; rows that keep a partial file for a later resume keep their bytes.
- Deleting downloads on a full disk no longer crashes: the row delete frees space once and retries once on `SQLiteFullException`; a delete that still fails shows a snackbar (success first, failure last), and clearing all downloads reports its failure the same way.
- Search queries are normalized (folded letters and curly quotes; optional marks stripped per script, including Arabic and Hebrew), the results list stays at the top as new results arrive above, a new query starts at the top, and a saved scroll position survives a tab switch or rotation.
- Book rows grow with large text instead of clipping: the row is minimum-height rather than fixed-height, and the cover keeps its fixed size, centered.
- Download and storage events use a `Channel` instead of a `SharedFlow`, so an event sent with no collector waits instead of being dropped; `ObserveAsEvents` takes any `Flow`. The Downloads & Storage page waits for its first combined state, the search scope stays unknown until the setting loads, and Search takes focus only once (a saved flag survives rotation and tab switches).
- The list-to-UI mapping in `DownloadsViewModel` and `SearchViewModel` runs on the injected default dispatcher (`flowOn` before `stateIn`).
- The Downloads tab header is a plain heading with no total size, and the Downloads & Storage page is reordered: the Downloads group first, then the Manage storage group with the storage summary, a divider, and clear-all.
- The theme preference also sets the system's per-app night mode (API 31+, `applyAppNightMode`), which the splash screen is drawn with, so the splash follows the app theme from the next cold start. `AbstrakApplication` keeps the two in step without blocking startup; a change recreates the activity.
- Server URLs are checked by the HTTP stack's own parser: `ServerUrlValidator` in domain, implemented over OkHttp's `HttpUrl` in data (also refusing user info, a query, a fragment, or a mistyped scheme such as `ftp://`). Connect stays disabled for an invalid URL and the field says why; a pasted URL is trimmed.
- A completed download whose file is empty, unreadable or not a file counts as missing everywhere (`isPlayableDownload`): playback streams that track, and the start-up storage check prunes the row so the track can be downloaded again.
- Navigation transitions follow Material motion, set once on the nav host (`navMotion` in `NavTransitions.kt`): fade through between root tabs, and shared axis X (30dp, mirrored in RTL) for everything else — forward on navigate, backward on pop and on landing on login (logout). The direction comes from which transition lambda runs, not from tab indices, so a tab and the settings page it opens slide the same way.

## Current Product Fit

### Good enough for current scope

- Login and server validation.
- Logout with full local wipe (session, caches, downloads).
- Initial catalog browsing.
- Basic cached library experience.
- Detail-oriented catalog browsing.
- Streaming playback of selected audiobooks with background controls.

### Not ready yet for future scope

- Playback queue management.
- Android Auto.
- Wear OS.

## Recommended Immediate Priorities

1. Introduce a playback queue/queue-source model plus queue UI on top of the cached tracks.
2. Finish polishing the main authenticated browsing shell across `Home` and `Library` (the Library's Authors section was removed), including filtering and sort affordances (search shipped as its own tab).
3. Keep reusing the `:playback` module for Android Auto and Wear OS — no further extraction needed.
