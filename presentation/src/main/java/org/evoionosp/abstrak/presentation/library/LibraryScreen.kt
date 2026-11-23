package org.evoionosp.abstrak.presentation.library
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.button.TonalCircleIconButton
import org.evoionosp.abstrak.presentation.designsystem.component.header.SectionHeader
import org.evoionosp.abstrak.presentation.designsystem.component.state.EmptyState
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.home.CatalogEffects
import org.evoionosp.abstrak.presentation.home.HomeUiState
import org.evoionosp.abstrak.presentation.home.HomeViewModel
import org.evoionosp.abstrak.presentation.library.components.LibraryPicker
import org.evoionosp.abstrak.presentation.library.components.LibraryStats
import org.evoionosp.abstrak.presentation.library.components.LibrarySyncChip
import org.evoionosp.abstrak.presentation.library.components.LibrarySyncSheet
import org.evoionosp.abstrak.presentation.ui.catalog.AudiobookGridCard
import org.evoionosp.abstrak.presentation.ui.catalog.AudiobookListCard
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeader
import org.evoionosp.abstrak.presentation.ui.catalog.RootScreenSurface
import org.evoionosp.abstrak.presentation.ui.catalog.rememberCatalogBookClick
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.time.rememberCurrentTimeMillis

private const val GRID_COLUMNS = 12
private val ItemGap = 12.dp
private val SectionTopPadding = 12.dp

@Composable
fun LibraryRoute(
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    bottomBarPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isGridView by rememberSaveable { mutableStateOf(true) }
    CatalogEffects(viewModel = viewModel, onSessionExpired = onSessionExpired)
    LibraryScreen(
        state = state,
        isGridView = isGridView,
        bottomBarPadding = bottomBarPadding,
        nowMillis = rememberCurrentTimeMillis(),
        onRefresh = viewModel::refresh,
        onLibrarySelected = viewModel::onLibrarySelected,
        onToggleLayout = { isGridView = !isGridView },
        onOfflineModeChange = viewModel::setOfflineMode,
        onOpenSettings = onOpenSettings,
        onOpenAudiobook = rememberCatalogBookClick(onOpenAudiobook),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    state: HomeUiState,
    isGridView: Boolean,
    bottomBarPadding: Dp,
    nowMillis: Long,
    onRefresh: () -> Unit,
    onLibrarySelected: (String) -> Unit,
    onToggleLayout: () -> Unit,
    onOfflineModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSyncSheet by rememberSaveable { mutableStateOf(false) }
    if (showSyncSheet) {
        LibrarySyncSheet(
            syncStatus = state.syncStatus,
            isOfflineMode = state.isOfflineMode,
            nowMillis = nowMillis,
            onOfflineModeChange = onOfflineModeChange,
            onDismiss = { showSyncSheet = false },
        )
    }
    RootScreenSurface(modifier = modifier) {
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = AbstrakSpacing.catalog,
                        end = AbstrakSpacing.catalog,
                        top = 16.dp,
                        bottom = bottomBarPadding + 16.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(ItemGap),
                verticalArrangement = Arrangement.spacedBy(ItemGap),
            ) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    RootHeader(onOpenSettings = onOpenSettings) {
                        LibraryPicker(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            libraries = state.libraries,
                            selectedLibraryId = state.selectedLibraryId,
                            selectedLibraryName = state.selectedLibraryName,
                            onLibrarySelected = onLibrarySelected,
                        )
                    }
                }
                if (state.libraries.isNotEmpty()) {
                    item(span = { GridItemSpan(GRID_COLUMNS) }) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LibrarySyncChip(
                                syncStatus = state.syncStatus,
                                isOfflineMode = state.isOfflineMode,
                                nowMillis = nowMillis,
                                onClick = { showSyncSheet = true },
                            )
                        }
                    }
                }
                if (state.audiobooks.isEmpty() && state.ebooks.isEmpty() && state.libraries.isNotEmpty()) {
                    item(span = { GridItemSpan(GRID_COLUMNS) }) {
                        EmptyState(
                            title = stringResource(R.string.home_no_books),
                            subtitle = stringResource(R.string.home_no_books_hint),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        )
                    }
                } else {
                    val showAudiobooks = state.audiobooks.isNotEmpty() || state.ebooks.isEmpty()
                    if (showAudiobooks) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            SectionHeader(
                                modifier = Modifier.padding(top = SectionTopPadding),
                                title = stringResource(R.string.library_audiobooks),
                                subtitle =
                                    pluralStringResource(
                                        R.plurals.library_audiobooks_count,
                                        state.audiobooks.size,
                                        state.audiobooks.size,
                                    ),
                                trailing = { LayoutToggle(isGridView = isGridView, onClick = onToggleLayout) },
                            )
                        }
                        books(books = state.audiobooks, isGridView = isGridView, onClick = onOpenAudiobook)
                    }
                    if (state.ebooks.isNotEmpty()) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            SectionHeader(
                                modifier = Modifier.padding(top = SectionTopPadding),
                                title = stringResource(R.string.library_books),
                                subtitle =
                                    pluralStringResource(
                                        R.plurals.library_books_count,
                                        state.ebooks.size,
                                        state.ebooks.size,
                                    ),
                                trailing =
                                    if (showAudiobooks) {
                                        null
                                    } else {
                                        { LayoutToggle(isGridView = isGridView, onClick = onToggleLayout) }
                                    },
                            )
                        }
                        books(books = state.ebooks, isGridView = isGridView, onClick = onOpenAudiobook)
                    }
                    if (state.audiobooks.isNotEmpty() || state.ebooks.isNotEmpty()) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            SectionHeader(
                                modifier = Modifier.padding(top = SectionTopPadding),
                                title = stringResource(R.string.library_stats),
                            )
                        }
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            LibraryStats(
                                booksCount = state.audiobooks.size + state.ebooks.size,
                                authorsCount = state.authorCount,
                                hoursCount = state.durationHours,
                                modifier =
                                    Modifier.layout { measurable, constraints ->
                                        val bleed = AbstrakSpacing.catalog.roundToPx()
                                        val width = constraints.maxWidth + bleed * 2
                                        val placeable =
                                            measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                                        layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.books(
    books: List<AudiobookUiModel>,
    isGridView: Boolean,
    onClick: (AudiobookUiModel) -> Unit,
) {
    items(
        items = books,
        key = { it.id },
        span = { GridItemSpan(if (isGridView) 4 else GRID_COLUMNS) },
    ) { audiobook ->
        if (isGridView) {
            AudiobookGridCard(
                audiobook = audiobook,
                onClick = { onClick(audiobook) },
                enabled = audiobook.hasAudio,
            )
        } else {
            AudiobookListCard(
                audiobook = audiobook,
                onClick = { onClick(audiobook) },
                enabled = audiobook.hasAudio,
            )
        }
    }
}

@Composable
private fun LayoutToggle(
    isGridView: Boolean,
    onClick: () -> Unit,
) {
    TonalCircleIconButton(
        imageVector =
            if (isGridView) {
                Icons.AutoMirrored.Rounded.FormatListBulleted
            } else {
                Icons.Rounded.GridView
            },
        contentDescription =
            stringResource(
                if (isGridView) R.string.library_show_list else R.string.library_show_grid,
            ),
        onClick = onClick,
        size = 48.dp,
    )
}
