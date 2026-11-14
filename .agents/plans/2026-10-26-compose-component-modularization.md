> **Status: DONE (2026-10-27), with the open items at the end of the progress log.** Analysis written 2026-10-26
> against the downloads work on `dev` (uncommitted); line numbers below are that
> snapshot and drift as the steps land. Decisions taken and the progress log are
> at the end of this file.

## Goal

Break the oversized Compose screens into small, single-purpose components, and
turn repeated UI into a shared component layer that features reuse instead of
copying. Do it incrementally, with no visual or behavioral regressions, and with
package boundaries that later become Gradle module boundaries mechanically,
following the module direction in `../architecture_target.md`.

## Success Criteria

- No production Compose file over ~250 lines. Screens read as a list of named
  sections, each previewable on its own.
- Every nav destination is split into a stateful `XxxRoute` (ViewModel, events,
  navigation) and a stateless `XxxScreen(state, onAction)`. No composable below
  a route calls `hiltViewModel()`.
- Each duplicated pattern listed under "Duplication" exists exactly once, in the
  shared layer.
- No feature package imports another feature package. The design-system layer
  imports nothing from `:domain`, the shared UI layer, or any feature.
- Every reusable component and every screen state has a light/dark preview.
  Screenshot tests guard the refactor; each step is pixel-identical unless the
  change is intended and called out.
- Production `:presentation` code shrinks by roughly 1.3k lines (out of ~11.4k)
  even after the shared components are added.

## Context And Current Facts

### Size and shape

`:presentation` has ~11.4k lines of main Kotlin in 70 files, plus 2.5k lines of
tests (mostly ViewModels and pure helpers). There are **0 `@Preview`s**, no
screenshot tests, and only the template `ExampleInstrumentedTest` in
`androidTest`. `ui-tooling` (needed to render previews) isn't a dependency,
though `libs.androidx.compose.ui.tooling` is already in the version catalog.

Largest UI files and what each one mixes together:

| File | Lines | What it mixes |
|---|---|---|
| `settings/ServerConnectionScreen.kt` | 707 | route + screen, ~80-line collapsing-header state machine, 3 groups, a header editor dialog, a user-agent sheet, a bypass-SSL dialog, a `KeyChain` call inline, 5 private row types |
| `player/NowPlayingScreen.kt` | 531 | route (`hiltViewModel`), derived state per tick, 3 sheet flags, inline portrait layout (landscape is a separate file), drag gesture, transport, top bar, footer, and book components used by other features (`BookTitleBlock`, `BookProgressBar`, a second `BookCoverArtwork`) |
| `bookdetails/BookDetailsContent.kt` | 472 | the 4th copy of the collapsing-header state machine, header art/scrims/title animation, button row, chapter list wiring |
| `player/SleepSheet.kt` | 357 | sheet chrome, a value picker (headline + slider + presets), a switch row, formatting helpers |
| `settings/PreferencesScreen.kt` | 346 | route + screen, collapsing-header copy, inline groups, logout dialog |
| `settings/AppearanceSettingsScreen.kt` | 328 | collapsing-header copy, group + 2 private row types |
| `library/CatalogControls.kt` | 326 | two near-identical library dropdowns (~100 duplicated lines); `CatalogTopControls`, `LibraryDropdownTrigger` and `SyncStatusLabel` are unused |
| `player/NowPlayingOverlay.kt` | 317 | sheet morph geometry, landing pulse, cover palette and scoped theme, back handling, mini/full slots, a duplicated drag detector, `LocalPlayerColors` provided twice |
| `library/LibraryScreen.kt` | 286 | route + screen on **`HomeViewModel`**, header with a settings button (copy of Home's), section header, grid/list toggle |

### Duplication (counted, not estimated)

| Pattern | Copies | Where |
|---|---|---|
| Collapsing-header state machine (`Animatable` height, `NestedScrollConnection`, snap-on-release) | 4 × ~75 lines | `PreferencesScreen` 77–155, `AppearanceSettingsScreen` 65–143, `ServerConnectionScreen` 91–169, `BookDetailsContent` 121–204 |
| Two-line list row (headline + supporting + trailing) | 7 | `ThemePreferenceRow`, `PreferenceSwitchRow`, `ConnectionSwitchRow`, `ConnectionNavigationRow`, `HeaderRow`, `PreferenceListRow`, `SleepChapterRow` |
| Titled rounded group container | 4 | `PreferenceGroup`, `ConnectionPreferenceGroup`, 2 inline in `PreferencesScreen` |
| Group divider (`outlineVariant` @ 0.4) | 6 | `SettingsDivider` + 5 inline |
| Tonal rounded card (26–30dp, tonal 3, `surfaceContainerLow`) | ~8 | settings groups, auth cards, `PlaceholderSectionCard`, `StatCard` |
| Destructive confirm dialog (red confirm with spinner, Cancel) | 3 | `DeleteDownloadDialog`, logout (`PreferencesScreen` 267), bypass SSL (`ServerConnectionScreen` 380) |
| Button with inline spinner | 2 | login (`AuthScreen` 206), check server (`ConnectToServerCard` 121) |
| Accent single-choice segmented row (accent fill, no check icon) | 3 | speed presets, sleep presets, protocol selector |
| Player bottom-sheet chrome (card color, tinted drag handle, padded column, title) | 3 | `SpeedSheet`, `ChaptersSheet`, `SleepSheet` |
| Value picker (headline + slider with magnet snapping + presets) | 2 | `SpeedSheet`, `SleepSheet` |
| Seek button | 2 | `SeekButton` (64dp), `MiniSeekButton` (44dp) |
| Vertical drag → `SheetDragState` gesture | 2 | `NowPlayingScreen` 108, `NowPlayingOverlay` 287 |
| Haptic tick helper | 2 | `PlayingTransport`, `MiniPlayerContent` |
| Artwork panel (cover + width-matched progress bar + title block) | 3 | portrait player 167–207, `NowPlayingScreenLandscape` 58–86, `BookDetailsLandscape` 82–106 |
| Current chapter for a position | 5 call sites, 3 implementations | `NowPlayingScreen` 93 (`lastOrNull`) and 246 (`indexOfLast`), `MiniPlayerContent` 60, both book-details layouts (`inProgressChapterIndex`) |
| Speed label | 3, with **two visible formats** | footer `"1.00x"` (`NowPlayingScreen` 98, `SpeedSheet` 92) vs presets `"1x"` (`SpeedSheet` 42) |
| `Long.toDurationLabel()` | 2 identical | `common/model/UiModels.kt` 121, `player/PlayerUiHelpers.kt` 47 |
| `authorizedImageRequest` | 4 overloads, 2 near-identical builders | `PlayerUiHelpers`; `AuthorCard` builds a painter per recomposition instead |
| `BookCoverArtwork` | 2 functions with the same name | `utils/CoverArt.kt` 224, `player/NowPlayingScreen.kt` 414 |
| Library dropdown | 2 | `LibraryDropdown`, `LibraryDropdownTrigger` |
| Root header settings button (56dp tonal circle) | 2 (+Auth variant) | `HomeHeader`, `LibraryHeader` |
| Book download action | 2 different designs | portrait: round icon with progress ring; landscape: labeled button, no progress, 4 inline branches |
| Chapter list wiring (`ChapterRow` + 4 lambdas) | 2 (+sheet) | both book-details layouts, `ChaptersSheet` |
| Section header (title + subtitle) | 2 live + 1 dead | `common/SectionBlock`, `LibrarySectionHeader`, `home/HomeSections.SectionBlock` |

**Dead code:**
- `home/RootScreenHeader.kt`, `home/HomeSections.kt:SectionBlock`, `home/EmptyState.kt`.
- `CatalogTopControls`, `LibraryDropdownTrigger` and `SyncStatusLabel`.
- The medium/high-contrast color schemes, `ColorFamily` and `unspecified_scheme` (~300 lines across `Theme.kt` and `Color.kt`; never referenced).

### Package dependency graph (from imports)

```
auth        -> home (homeBackgroundBrush), navigation (LocalSnackbarHostState, ObserveAsEvents)
bookdetails -> player (BookCoverArtwork, BookProgressBar, BookTitleBlock, ChapterRow,
               DownloadButtonStatus, DownloadTrailingButton, DownloadUiState, inProgressChapterIndex),
               navigation (route arg constants), common
home        -> navigation, common, utils
library     -> home (HomeViewModel, HomeUiEvent, AudiobookGridCard, AudiobookListCard,
               ForegroundRefreshEffect, homeBackgroundBrush), player (authorizedImageRequest),
               navigation, common
player      -> utils (CoverArt, BookCoverArtwork, SheetDragState, palette helpers), icons, common
utils       -> player (authorizedImageRequest), common         <- cycle with player
navigation  -> every feature (expected: it's the shell)
```

These edges would block a feature-module split: `bookdetails → player`,
`library → home`, `library → player`, `auth → home`, the `utils ↔ player` cycle,
and features → `navigation`.

### Other facts that shape the design

- **`PlayerColors` duplicates `ColorScheme` roles.** `playerColorsFallback()`
  maps them one to one: accent→`primary`, onAccent→`onPrimary`,
  textPrimary→`onSurface`, textSecondary→`onSurfaceVariant`,
  containerSubtle→`surfaceContainerHigh`, trackSubtle→`surfaceVariant`, and the
  card is `surface`. Shared components (`BookTitleBlock`, `BookProgressBar`,
  `ChapterRow`, buttons, sheets) read `LocalPlayerColors`, so `AbstrakApp`
  provides an app-level fallback just so Book Details can use them.
- **Derived state is recomputed per position tick in composables:** book
  progress, chapter title, current chapter index, speed label, sleep label,
  end-of-chapter availability. The mini player, the full player and the sheets
  each derive their own.
- **The overlay reads `sheetDrag.offsetPx` and `expansionFraction` during
  composition** (`NowPlayingOverlay` 132, 153), so every drag frame recomposes
  the overlay, and the card's `.height()` and `RoundedCornerShape(cornerRadius)`
  relayout per frame.
- **`hiltViewModel()` appears below the route level:** `MiniPlayerContent`,
  `NowPlayingScreen` (both re-inject `NowPlayingViewModel`, which `AbstrakApp`
  already holds), `HomeScreen`, `LibraryScreen`, `AuthScreen`,
  `PreferencesScreen`, `ServerConnectionScreen`.
- **The pure helper → unit test pattern already works here**
  (`resolveSheetTarget`, `playPauseCorner`, `progressFraction`, the sleep
  formatters, `inProgressChapterIndex`). The plan leans on it.
- **The Compose compiler is from Kotlin 2.4,** so strong skipping is on by
  default. Domain models come from `:domain` (no Compose compiler), which makes
  them unstable to Compose, compared by instance.
- **Navigation is string routes on Navigation 2.9.8,** where type-safe routes
  are available. The app module's release build minifies (R8), so preview
  fixtures in `main` are stripped from release.
- **Compose UI won't carry over to Android Auto or Wear** (see the multi-surface
  direction). Cross-surface reuse means pure-Kotlin UI models and formatters,
  not composables.

## Constraints And Non-goals

- Visual output stays the same, except for the explicitly listed fixes (a11y,
  strings, the two speed formats). Motion (sheet morph, collapsing headers,
  landing pulse) must feel identical.
- No ViewModel API churn just for the refactor. Routes map UI actions onto the
  existing ViewModel methods, so VM tests keep passing unchanged.
- No new Gradle modules until the package boundaries are clean (Phase 5).
  Packages get most of the value now and keep the move mechanical.
- No speculative components. Something enters a shared layer only when two or
  more real call sites use it (rule of two); otherwise it stays in its feature.
- `LibraryScreen.kt` and `CatalogControls.kt` have uncommitted changes from
  another agent session. Library work waits until those land.
- Not in scope: new features, the offline-playback wiring, or a Library
  ViewModel split (listed as a follow-up only).

## Key Decisions

1. **Packages first, Gradle modules later.** Every problem above (size,
   duplication, dependency direction) is solvable inside `:presentation`. Each
   new Android module costs ~60 lines of copied Gradle config today (no
   convention plugins exist). Clean packages make Phase 5 a directory move plus
   a short build file, and the import graph above becomes its acceptance test.
2. **Two shared layers with a strict direction, mirroring Now in Android's
   `:core:designsystem` / `:core:ui`:**
   - `presentation.designsystem`: theme, tokens and generic components. No
     `:domain` types, no ViewModels, no Hilt, no Coil.
   - `presentation.ui`: domain-aware shared components (book, chapter,
     download, catalog), image loading, formatters, event/effect helpers.
   - Features (`auth`, `home`, `library`, `bookdetails`, `player`,
     `settings`, `splash`) depend on both and never on each other.
     `navigation` (the shell) depends on features.
   - Existing feature package names stay (no `feature.` prefix) to keep import
     churn low. `common`, `utils`, `icons` and `theme` dissolve into the two
     shared layers.
3. **Route / Screen split everywhere, and a single `onAction` for big
   screens.** `XxxRoute` owns the ViewModel, lifecycle collection, one-off
   events and saveable transient UI state (open sheet, dialog). `XxxScreen` is
   stateless. Screens with more than ~6 callbacks (the landscape player
   takes 17 parameters, 10 of them callbacks; Book Details portrait takes 19,
   9 of them callbacks) take `onAction: (XxxAction) -> Unit` with a `sealed
   interface XxxAction`, and the route maps actions onto today's VM methods. Leaf
   components keep plain lambdas (Material style, maximally reusable). A sealed
   action is one stable parameter and makes previews trivial (`onAction = {}`).
   A data class of lambdas would need careful `remember`ing to stay stable.
4. **Scoped `MaterialTheme` replaces `LocalPlayerColors` and
   `LocalPlayerCardColor`.** The player builds a `ColorScheme` from the cover's
   dynamic scheme (`MaterialDynamicColors` has every role) with `surface` set
   to the card tint, animates the few roles it uses, and provides it with
   `MaterialTheme(colorScheme = …)` around the sheet. Shared components then
   use plain `MaterialTheme` roles: Book Details gets app colors, the player and
   its sheets get cover colors, and no app-root fallback provider is needed.
   Sheets already inherit locals (as the existing comments note). This also
   deletes most explicit `SliderDefaults`/`SegmentedButtonDefaults`/
   `SwitchDefaults` color plumbing. It must land before book and chapter
   components move to `ui`, otherwise `ui` would depend on the player.
5. **Derived playback state moves to the ViewModel,** via a pure mapper with
   unit tests (`toNowPlayingUiState(...)`). It's computed once per emission for
   the mini player, the full player and the sheets. The current-chapter rule
   joins `PlaybackPositionCalculator` in `:domain` as `chapterIndexAt(...)`
   (`previousChapterTarget` already uses the same rule), so Auto and Wear
   reuse it.
6. **Safety net before surgery.** Add previews and screenshot tests for each
   screen *before* refactoring it. Roborazzi is recommended: it runs on the
   Robolectric setup `:presentation` tests already use, inside
   `testDebugUnitTest`. Stateful screens get their mechanical Route/Screen split
   first, then baselines, then the internal refactor.
7. **Formatters are pure Kotlin** (no Compose, no Android): durations, time
   left, speed, bytes, sleep labels. Strings with words come from resources at
   the call site. This keeps them unit-testable and movable to a JVM module once
   Wear work starts.

## Recommended Approach

### Target package layout (inside `:presentation`)

```
presentation/
  designsystem/
    theme/      AbstrakTheme (was AppTheme), Color, Type, Shapes, Spacing, text-style tokens
    component/
      card/     TonalCard, PlaceholderCard
      list/     ListGroup, GroupDivider, AbstrakListItem + NavigationListItem / SwitchListItem / RadioListItem
      header/   CollapsingHeaderState + rememberCollapsingHeaderState, CollapsingTitleHeader, SectionHeader
      button/   LoadingButton, TonalCircleIconButton, SegmentedChoiceRow
      dialog/   ConfirmDialog
      picker/   ValuePicker
      sheet/    AbstrakBottomSheet
      text/     MarqueeText
      icon/     PlayingEqualizerIcon
      state/    EmptyState
  ui/
    image/      CoverArt pipeline (interceptor, generated cover), one authorizedImageRequest, LocalAccessToken
    book/       BookCover, BookTitleBlock, BookProgressBar, BookArtworkPanel
    chapter/    ChapterRow, LazyListScope.chapterItems
    download/   DownloadUiState/Status, DownloadIconButton, DownloadLabeledButton, DeleteDownloadDialog
    catalog/    AudiobookGridCard, AudiobookListCard, AuthorCard, HorizontalBookRow, RootHeader
    format/     pure formatters (+ tests)
    event/      ObserveAsEvents, LocalSnackbarHostState, ForegroundRefreshEffect
    haptics/    rememberHapticTick
  auth/ home/ library/ bookdetails/ player/ settings/ splash/   (features; Route + Screen + components/)
  navigation/   AbstrakApp, AbstrakNavHost, RootNavigationBar, NavTransitions, routes
```

### Shared component catalog (proposed APIs)

The design system follows Material conventions: `modifier` is the first
optional parameter, colors and sizes come through `XxxDefaults`, and nothing
reads a feature's CompositionLocal.

```kotlin
// designsystem
@Composable fun TonalCard(modifier: Modifier = Modifier, shape: Shape = AbstrakShapes.card,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow, content: @Composable ColumnScope.() -> Unit)
@Composable fun ListGroup(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)
@Composable fun GroupDivider(modifier: Modifier = Modifier)
@Composable fun AbstrakListItem(headline: String, modifier: Modifier = Modifier, supporting: String? = null,
    supportingMaxLines: Int = Int.MAX_VALUE, leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null, colors: AbstrakListItemColors = AbstrakListItemDefaults.colors())
@Composable fun NavigationListItem(headline: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    supporting: String? = null, leading: (@Composable () -> Unit)? = null, trailing: @Composable () -> Unit = { Chevron() })
@Composable fun SwitchListItem(headline: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, supporting: String? = null, enabled: Boolean = true)     // toggleable, Role.Switch
@Composable fun RadioListItem(headline: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, supporting: String? = null)                              // selectable, Role.RadioButton

@Stable class CollapsingHeaderState { val heightPx: Float; val collapseFraction: Float; val nestedScrollConnection: NestedScrollConnection }
@Composable fun rememberCollapsingHeaderState(listState: LazyListState, minHeight: Dp, maxHeight: Dp): CollapsingHeaderState
    // owns the snap-on-release LaunchedEffect that all four copies repeat
@Composable fun CollapsingTitleHeader(title: String, collapseFraction: Float, height: Dp, onBack: () -> Unit)
@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null)
@Composable fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier, icon: ImageVector? = null)

@Composable fun LoadingButton(text: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    loadingText: String = text, enabled: Boolean = true)
@Composable fun TonalCircleIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 56.dp)
@Composable fun <T> SegmentedChoiceRow(options: List<T>, selected: T?, onSelect: (T) -> Unit, label: (T) -> String,
    modifier: Modifier = Modifier, enabled: Boolean = true, colors: SegmentedButtonColors = SegmentedChoiceDefaults.colors())
@Composable fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
    onDismiss: () -> Unit, destructive: Boolean = false, inProgress: Boolean = false)
@Composable fun <T> ValuePicker(valueLabel: String, value: Float, valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit, presets: List<T>, presetValue: (T) -> Float, presetLabel: (T) -> String,
    modifier: Modifier = Modifier, onValueChangeFinished: (() -> Unit)? = null, enabled: Boolean = true)
@Composable fun AbstrakBottomSheet(onDismiss: () -> Unit, title: String?, modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit)
@Composable fun MarqueeText(text: String, style: TextStyle, modifier: Modifier = Modifier)

// ui
@Composable fun BookCover(coverUrl: String, title: String, author: String, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop, contentDescription: String? = null)
    // null inside cards that already show the title, so TalkBack doesn't read it twice
@Composable fun BookTitleBlock(title: String, author: String, modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start, onClick: (() -> Unit)? = null)
@Composable fun BookProgressBar(progress: Float, modifier: Modifier = Modifier)
@Composable fun BookArtworkPanel(coverUrl: String, title: String, author: String, progress: Float,
    modifier: Modifier = Modifier, onTitleClick: (() -> Unit)? = null)                       // the 3 copies
@Composable fun ChapterRow(chapter: AudiobookChapter, isCurrent: Boolean, isPlaying: Boolean, onPlay: () -> Unit,
    modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null)               // download control is a slot
fun LazyListScope.chapterItems(chapters: List<AudiobookChapter>, currentIndex: Int, isPlaying: Boolean,
    onPlay: (AudiobookChapter) -> Unit, trailing: (@Composable (index: Int) -> Unit)? = null) // keyed items
@Composable fun DownloadIconButton(state: DownloadUiState, onDownload: () -> Unit, onPause: () -> Unit, onDelete: () -> Unit,
    modifier: Modifier = Modifier)                                                            // ring; moves only while active
@Composable fun DownloadLabeledButton(/* same */)
@Composable fun RootHeader(onOpenSettings: () -> Unit, modifier: Modifier = Modifier, title: @Composable () -> Unit)
```

Note `ChapterRow`'s `trailing` slot. Today it takes a nullable download state
plus three download callbacks, which couples chapter UI to downloads. With the
slot, the player's sheet passes nothing and Book Details passes a
`DownloadIconButton`.

### Screen-by-screen decomposition

**Now Playing (`player`), ~1.4k lines across 7 files today.**

| New file | ~Lines | Content |
|---|---|---|
| `NowPlayingRoute.kt` | 80 | VM, `collectAsStateWithLifecycle`; `openSheet` as one saveable `PlayerSheet { None, Speed, Chapters, Sleep }` instead of 3 booleans; `PlayerAction` → VM; hosts `PlayerSheets` |
| `NowPlayingScreen.kt` | 90 | `NowPlayingScreen(state, onAction, sheetDrag)`: surface, `Modifier.sheetDragGesture`, `PlayerTopBar`, then the portrait or landscape layout |
| `NowPlayingLayouts.kt` | 110 | two thin arrangements of the same three blocks: `BookArtworkPanel`, `PlayerTransport`, `PlayerFooterActions` (portrait column, landscape 40/60 row) |
| `transport/PlayerTransport.kt` | 110 | chapter title line + `PlayerSeekBar` + controls row |
| `transport/SeekButton.kt`, `transport/PlayPauseButton.kt` | 60 + 90 | one seek button with `size = Mini/Full`; morphing play/pause with content descriptions |
| `PlayerFooterActions.kt` | 70 | three uniform `PlayerFooterAction(label, value \| icon, onClick)` with `Role.Button` |
| `sheets/PlayerSheets.kt` + Speed / Chapters / Sleep | 40 + 70 + 50 + 150 | on `AbstrakBottomSheet` + `ValuePicker` + `chapterItems` + `SwitchListItem` |
| `overlay/NowPlayingOverlay.kt` | 120 | the shell: slots for the mini and full players |
| `overlay/SheetMorph.kt` | 60 | **pure** `sheetMorphGeometry(fraction, …)` (card top/height, side padding, corner, mini/full alpha, parallax) + unit tests |
| `overlay/LandingPulse.kt` | 35 | `rememberLandingPulse(targetExpanded)` |
| `theme/PlayerTheme.kt` | 90 | `rememberPlayerColorScheme(book, themeFromCover)` + `PlayerTheme { }` (Decision 4); absorbs `PlayerColors.kt` and the palette usage |
| `gesture/SheetDragState.kt`, `gesture/SheetDragGesture.kt` | 193 + 35 | moved from `utils`; the shared drag modifier |
| `MiniPlayer.kt` | 90 | stateless `MiniPlayer(state, onAction)`; the overlay wires the VM |

Efficiency work in the same phase:
- Read `sheetDrag` in layout and draw lambdas (`Modifier.layout {}`,
  `graphicsLayer { shape = …; clip = true }`) so a drag doesn't recompose the
  player's subtree. Confirm with Layout Inspector recomposition counts before
  and after.
- Add stable `key`s to chapter lists (`items(chapters.size)` and
  `itemsIndexed(chapters)` have none today).

Fixes in this phase (visible, and each called out in its PR):
- The play/pause button gets content descriptions.
- Seek buttons announce "Rewind 15 seconds" / "Forward 30 seconds" instead of
  "15"/"30". The labels come from `PlaybackConnection`'s `SEEK_BACKWARD_MS` /
  `SEEK_FORWARD_MS` instead of being hard-coded twice.
- The Chapters footer action gets a role.
- One speed format everywhere.
- "Zz"/"Ch" move to string resources.

**Book Details (`bookdetails`), 826 lines today.**

| New file | ~Lines | Content |
|---|---|---|
| `BookDetailsRoute.kt` | 60 | VM, state, `BookDetailsAction` → VM, delete-dialog host (one `DeleteDownloadDialog` call; the two identical branches merge) |
| `BookDetailsScreen.kt` | 70 | loading/content, orientation switch, landscape top bar |
| `BookDetailsPortrait.kt` | 90 | `rememberCollapsingHeaderState` + `BookHeroHeader` over a list: progress, `BookActionsRow`, `chapterItems` |
| `BookDetailsLandscape.kt` | 70 | `BookArtworkPanel` + `BookActionsRow` + chapters heading + `chapterItems` |
| `components/BookHeroHeader.kt` | 120 | the full-bleed cover header (scrims, back button, title ride-up); its fraction math stays local |
| `components/BookActionsRow.kt` | 40 | `ContinueListeningButton` + `DownloadIconButton`, **shared by both orientations** |

**Settings (`settings`), 1,381 lines today.** A `SettingsScaffold(title, onBack,
miniTrailPadding, content: LazyListScope.() -> Unit)` wraps
`rememberCollapsingHeaderState`, nested scroll, insets and the header. Each
screen becomes a list of `ListGroup`s of list items:
- `PreferencesRoute`/`Screen`: ~120 lines (logout uses `ConfirmDialog`).
- `AppearanceScreen`: ~90.
- `ServerConnectionRoute`/`Screen`: ~170, plus `HeaderEditorDialog.kt` (~90),
  `UserAgentSheet.kt` (~60) and `rememberClientCertificatePicker()` (~45),
  which isolates the `KeyChain`/`Toast` Activity code from layout.

Expected total: ~650 lines, down from 1,381.

**Home and Library (`home`, `library`).**
- `HomeRoute`/`Screen`, `LibraryRoute`/`Screen`.
- `RootHeader` replaces both settings-button headers.
- One `LibraryPicker` replaces the two dropdowns; delete the unused trio once
  the other session's changes land.
- `SectionHeader` (with the grid/list toggle in its `trailing` slot) replaces
  `LibrarySectionHeader`.
- Cards move to `ui/catalog`.
- `homeBackgroundBrush` becomes a design-system token, which removes the
  `auth → home` and `library → home` UI edges.
- Strings: "Hi, $username"/"Hi there"; "${n} Books"/"${n} Authors" (should be
  plurals); "Up to date"; "Unknown duration"; the Search FAB description.

**Auth (`auth`).** `AuthRoute`/`Screen`. Extract a `CredentialsCard` from
`AuthScreen`; both cards use `TonalCard` and `LoadingButton`, and the protocol
selector uses `SegmentedChoiceRow`.

### Expected size change (production code, approximate)

- Collapsing headers −210
- Settings rows, groups and dividers −100
- Dead code −580
- Library dropdown −100
- Player sheet chrome and value picker −80
- Artwork panel −50
- Labeled download button −40
- Dialogs and loading buttons −60
- Chapter items −30
- Formatters and image requests −40

That's about −1.3k lines. Previews and screenshot tests are added on top on
purpose and don't count against this.

## Work Plan

Each step is one small PR-sized change that passes Validation before the next
one starts. The order front-loads safety and the biggest, lowest-risk wins.

**Phase 0: safety net and cleanup (no behavior change)**
1. Add `debugImplementation(libs.androidx.compose.ui.tooling)`. Add an internal
   `preview/PreviewData.kt` (sample book, 10 chapters, every
   `DownloadButtonStatus`, playback states).
2. Add Roborazzi. Record light/dark baselines for the already-stateless UI:
   `BookDetailsContent`, `BookDetailsLandscape`, `AppearanceSettingsScreen`, the
   three sheets, `ChapterRow` × download states, the cards.
3. Do the mechanical Route/Screen split for the stateful screens (Auth, Home,
   Library, Preferences, ServerConnection, NowPlaying, MiniPlayer), then record
   their baselines.
4. Delete the dead code. Break the `utils ↔ player` cycle by moving
   `authorizedImageRequest` next to `CoverArt` (merged into one builder).

**Phase 1: design system and its first consumers**
5. Move theme files into `designsystem/theme`. Add `AbstrakShapes` and
   `AbstrakSpacing` (only values already in use: 30/28/22dp shapes; 20/18/16dp
   spacing) and the collapsed-title text style shared by `PlayerTopBar` and
   `CollapsingSettingsHeader`.
6. Add `TonalCard`, `ListGroup`, `GroupDivider` and the list items, then
   migrate the three settings screens (biggest single win).
7. Add `rememberCollapsingHeaderState` and `SettingsScaffold`, then migrate
   settings, then Book Details portrait.
8. Add `ConfirmDialog` (logout, bypass SSL, `DeleteDownloadDialog` on top of
   it), `LoadingButton` (auth) and `SegmentedChoiceRow` (protocol, speed, sleep).

**Phase 2: shared UI layer**
9. Move into `ui/format` and `ui/event`: formatters (one duration and one speed
   formatter, with tests), `ObserveAsEvents`, `LocalSnackbarHostState`,
   `ForegroundRefreshEffect`. Add `chapterIndexAt` to
   `PlaybackPositionCalculator` and use it at all 5 call sites.
10. Move the cards to `ui/catalog` and add `RootHeader`. Afterwards
    `library → home` (except `HomeViewModel`), `auth → home` and
    `features → navigation` (except route args) are gone.

**Phase 3: player**
11. Scoped player theme (Decision 4). Screenshot-diff every player surface
    and sheet.
12. Move `BookCover`/`BookTitleBlock`/`BookProgressBar`/`BookArtworkPanel`,
    `ChapterRow` + `chapterItems`, and the download components into `ui`, with
    the `trailing` slot. Afterwards `bookdetails → player` is gone.
13. Add the derived-state mapper to `NowPlayingViewModel` (with tests). Wire
    `NowPlayingRoute`, `NowPlayingScreen` and the layouts, `PlayerTransport`
    and `PlayerFooterActions`.
14. Sheets on `AbstrakBottomSheet` + `ValuePicker`. `MiniPlayer` becomes
    stateless.
15. Split the overlay: `sheetMorphGeometry` (tests), landing pulse, drag
    modifier, deferred state reads. Do the manual motion pass.

**Phase 4: Book Details, Home, Library, Auth**
16. `BookDetailsAction`, portrait and landscape layouts from
    `BookHeroHeader` / `BookActionsRow` / `chapterItems`, and
    `DownloadIconButton` in both orientations.
17. Home and Library sections and headers, `LibraryPicker` (after the other
    session lands). Auth cards.
18. Write the UI conventions into `../best_practices.md` (see below) and adopt
    the compose-rules ktlint ruleset (mrmans0n/compose-rules, via the ktlint
    plugin's `ktlintRuleset` configuration) to enforce them. Add an
    architecture test (Konsist or a small import scan) for the dependency rules.

**Phase 5 (optional): Gradle modules**
19. Add a `build-logic` included build with convention plugins
    (`abstrak.android.library`, `…library.compose`, `…hilt`, `…feature`).
20. Extract `:core-designsystem`, then `:core-ui`, then one feature at a time,
    using the hyphen naming from `../architecture_target.md`:
    - `:feature-settings` first (a leaf);
    - then `:feature-auth`, `:feature-book-details`, `:feature-playback`
      (the player);
    - `:feature-catalog` (Home and Library together, since they share a
      ViewModel today).
    Each feature exposes `NavGraphBuilder.xxxScreen(...)` and
    `NavController.navigateToXxx(...)` and owns its route arguments (Book
    Details' argument constants leave `navigation`). Type-safe `@Serializable`
    routes are optional. `:presentation` shrinks to the UI shell. Only start
    this if build times or boundary leaks hurt; until then the architecture
    test enforces the same rules.

### UI conventions (to add to `best_practices.md` with step 18)

- One destination = `XxxRoute` + stateless `XxxScreen`. Sections go in the
  feature's `components/`, each ≤ ~200 lines. A private helper lives next to its
  only caller.
- Components take `modifier: Modifier = Modifier` as the first optional
  parameter, get colors and sizes from `XxxDefaults`, never inject a ViewModel,
  and never read a feature's CompositionLocal.
- Strings come from resources. Every actionable icon has a content description.
  Custom clickables declare a `Role`. Images inside a card that shows the title
  get a `null` description.
- Every component has a light/dark preview. Every screen has a preview per
  state (loading, empty, content, error).
- Shared layers follow the rule of two: promote a component only when a second
  feature needs it.

## Validation Plan

For every step:
- Run `./gradlew ktlintCheck :domain:testDebugUnitTest :presentation:testDebugUnitTest :app:assembleDebug`.
- Run `verifyRoborazziDebug`: no diffs, or intended diffs re-recorded with the
  reason in the PR.
- Re-run the import-graph scan: no new cross-feature edges, and the count of
  remaining edges only goes down.

Per phase:
- Device pass for motion and gestures, which screenshots can't cover: sheet
  drag up/down from the mini and full player, tap-to-expand, back to minimize,
  rotation with the sheet open, collapsing headers (settings ×3, Book Details)
  including fling and snap, and TalkBack over the player and chapter rows.
- Phase 3: compare player recomposition counts during a drag in Layout
  Inspector. Expect fewer, not equal.

## Risks / Rollback

- **Visual regressions:** screenshot baselines are captured before each
  screen is touched. Rollback is per PR, since each step is independent.
- **Motion regressions:** these aren't screenshot-testable. Mitigation:
  geometry moves into pure functions with unit tests, plus the device pass
  above.
- **The scoped theme changes default M3 component colors** (sheets, slider,
  switch, segmented buttons). Mitigation: diff every player surface. Fallback:
  keep explicit colors for the few components that drift.
- **Concurrent agent sessions** editing the same files: sequence the phases,
  never refactor a file with uncommitted changes from another session, and
  snapshot before each step.

## Decisions (2026-10-27)

The open questions from the analysis, as answered:

1. **Gradle modules:** not now. Phase 5 is skipped; packages plus the
   architecture test enforce the boundaries.
2. **Scoped `MaterialTheme` for the player:** yes (Decision 4).
3. **"Continue listening":** always plays (`playBook`) in both orientations. It
   never pauses.
4. **Book Details load order:** cache first. `loadBook()` starts observing the
   cached detail, then refreshes from the network.
5. **Contrast color schemes:** deleted.
6. **Screenshot tests:** dropped for now; Shubh tests on device and reports
   regressions. Phase 0 step 2 and the `verifyRoborazziDebug` validation are
   skipped. Each step is still validated by the unit tests, ktlint and the
   build.
7. **`LibraryViewModel` split:** stays a follow-up, outside this plan.

## Progress Log

- 2026-10-27: `LICENSE` (GPL-3.0) added and the README license section
  rewritten. Execution started; snapshot of the pre-refactor tree in
  `/tmp/abstrak-pre-modularization-20261027-0944.tgz`.
- Phase 0 step 3 (mechanical Route/Screen splits) is folded into each
  screen's own step: without screenshot baselines to record in between, a
  separate mechanical pass would only be rewritten again.
- Step A (Phase 0 step 4 + Phase 1 step 5): dead code deleted; theme moved to
  `designsystem/theme` (`AbstrakTheme`, tokens, `collapsingTitle`,
  `screenBackgroundBrush`); one `authorizedImageRequest` +
  `rememberAuthorizedImageRequest` in `ui/image`; `BookCover` replaces both
  `BookCoverArtwork`s (null content description: every call site shows the
  title).
- Step B (Phase 1 steps 6-8): `TonalCard`, `ListGroup`/`GroupDivider`, the
  list items, `rememberCollapsingHeaderState`, `CollapsingTitleHeader`,
  `ConfirmDialog`, `LoadingButton`, `SegmentedChoiceRow`,
  `AbstrakBottomSheet`. Settings run on `SettingsScaffold`
  (Preferences/ServerConnection got routes; the header dialog, user-agent
  sheet and certificate picker are split out). Auth got its route, a
  `CredentialsCard`, and the shared buttons. `DeleteDownloadDialog` is in
  `ui/download` on top of `ConfirmDialog`. Intended changes: switch rows
  toggle from the whole row; settings lists end with 24dp trailing padding
  everywhere (Preferences had 16dp).
- Step C (Phase 2 steps 9-10, plus Home/Library from step 17): formatters in
  `ui/format` (one duration and one speed formatter, tested),
  `ObserveAsEvents`/`LocalSnackbarHostState`/`ForegroundRefreshEffect` in
  `ui/event`, `chapterIndexAt` in `PlaybackPositionCalculator`. Cards and
  `RootHeader` in `ui/catalog`; Home and Library got routes, `LibraryPicker`
  replaces both dropdowns, `CatalogEffects` holds the shared catalog effects.
  Greeting, book/author counts (plurals), "Unknown duration" and the Search
  FAB description moved to resources. Deviations: the rule of two keeps
  `WavySliderExpressive`, `MarqueeText`, `PlaceholderCard`, `AuthorCard` and
  `rememberHapticTick` in their only feature; chapter rows get no stable keys
  (chapters carry no identity and start times can repeat in bad metadata).
- Phase 3 (steps 11-15): `PlayerTheme`/`rememberPlayerColorScheme` replace
  `LocalPlayerColors`/`LocalPlayerCardColor`; player components read plain
  `MaterialTheme` roles, so M3 defaults inside the player (unselected
  segments, disabled switch/slider/button colors) now resolve against the
  player scheme instead of the app scheme. `ui/book`, `ui/chapter` (trailing
  slot) and `ui/download` hold the shared pieces, so `bookdetails` no longer
  imports `player`. The VM derives book progress, chapter title, the sleep
  footer model and end-of-chapter availability in a pure, tested
  `nowPlayingUiState`. `NowPlayingRoute`/`Screen`/`Layouts`,
  `PlayerTransport`, `PlayerFooterActions`, one sealed `PlayerAction` and a
  saveable `PlayerSheet`. Sheets run on `AbstrakBottomSheet` (through
  `PlayerBottomSheet`), `ValuePicker`, `SwitchListItem` and `chapterItems`;
  `MiniPlayer` is stateless behind `MiniPlayerRoute`. The overlay takes the
  two players as slots, its geometry is the pure `sheetMorphGeometry`
  (tested), and the drag position is read only in layout/draw lambdas.
  `AbstrakApp` collects only the playing book instead of the per-tick player
  state. Fixes: play/pause and seek buttons have descriptions ("Rewind 15
  seconds", from `PlaybackConnection`'s constants), footer actions have a
  button role, one speed format, "Zz"/"Ch"/"%dm" and the mini subtitle are
  resources, and the mini height is measured (it was always the 64dp
  fallback, the same value). Book Details already uses `chapterItems` with a
  `DownloadIconButton` slot and `BookArtworkPanel`; landscape's labeled
  download buttons became the icon button, so no `DownloadLabeledButton`.
  The motion pass and recomposition counts still need a device.
- Phase 4 step 16: Book Details is `BookDetailsRoute` (one delete dialog;
  `DeleteDownloadTarget` exposes `name`/`bytes`), `BookDetailsScreen` (with
  `BookDetailsAction`), `BookDetailsPortrait` on `rememberCollapsingHeaderState`,
  `BookDetailsLandscape`, and `components/` (`BookHeroHeader`,
  `BookActionsRow`, `bookChapterItems`). Continue listening always calls
  `playBook()`; the unused `togglePlayPause()` is gone. `loadBook()` observes
  the cached detail before refreshing; a test with a never-ending refresh
  pins it (it fails on the old order). Step 17 landed with Steps B and C.
- Step 18: UI conventions are in `../best_practices.md`; `PackageDependencyTest`
  scans imports for the layering rules (allowed edges: library → home's
  ViewModel/state/effects, bookdetails → the route-argument constants), with
  no new dependency. `ServerConnectionScreen`'s groups moved to
  `settings/components/`. `AbstrakApp` reads a `playingAudiobook` StateFlow
  (lint flags `StateFlow.value` in composition).
- Open items:
  - compose-rules is not adopted. The sandbox blocks downloading new
    dependencies; enabling it means adding `io.nlopez.compose.rules:ktlint` to
    the version catalog and `add("ktlintRuleset", …)` in the root
    `allprojects` block, then fixing what it reports.
  - Previews and screenshot tests are deferred.
  - The device checks (motion pass, recomposition counts) are still to do.
  - `hiltViewModel()` from `androidx.hilt.navigation.compose` is deprecated
    in favor of `androidx.hilt.lifecycle.viewmodel.compose`.
  - The `LibraryViewModel` split remains a follow-up.
