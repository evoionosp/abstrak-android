package org.evoionosp.abstrak.presentation.search
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.state.EmptyState
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.search.components.SearchField
import org.evoionosp.abstrak.presentation.ui.catalog.AudiobookListRow
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeader
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeaderDefaults
import org.evoionosp.abstrak.presentation.ui.catalog.RootScreenSurface
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
fun SearchScreen(
    state: SearchUiState,
    queryState: TextFieldState,
    bottomBarPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var focusRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focusRequested && queryState.text.isEmpty()) {
            focusRequested = true
            focusRequester.requestFocus()
        }
    }
    RootScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            RootHeader(
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(RootHeaderDefaults.PinnedPadding),
            ) {
                SearchField(
                    state = queryState,
                    placeholder = searchPlaceholder(state),
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = AbstrakSpacing.catalog),
            ) {
                if (state.isRefreshing && state.results != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            val results = state.results
            when {
                results == null -> {
                    EmptyState(
                        title = stringResource(R.string.search_idle_title),
                        subtitle = searchIdleHint(state),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }

                results.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.search_no_results),
                        subtitle = stringResource(R.string.search_no_results_hint),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }

                else -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.isScrollInProgress }
                            .filter { it }
                            .collect { focusManager.clearFocus() }
                    }
                    LaunchedEffect(listState) {
                        snapshotFlow { queryState.text.toString() }
                            .drop(1)
                            .collect { listState.requestScrollToItem(0) }
                    }
                    val previousResults = remember { PreviousResults(results) }
                    SideEffect {
                        if (previousResults.list != results) {
                            previousResults.list = results
                            if (listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0
                            ) {
                                listState.requestScrollToItem(0)
                            }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .consumeWindowInsets(PaddingValues(bottom = bottomBarPadding))
                                .imePadding(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomBarPadding + 16.dp),
                    ) {
                        items(
                            items = results,
                            key = { it.audiobook.id },
                            contentType = { "search_result" },
                        ) { result ->
                            AudiobookListRow(
                                audiobook = result.audiobook,
                                detail = result.libraryName,
                                onClick = { onOpenAudiobook(result.audiobook) },
                                enabled = result.audiobook.hasAudio,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun searchPlaceholder(state: SearchUiState): String =
    when {
        state.searchAllLibraries == null -> {
            stringResource(R.string.root_search)
        }

        state.searchAllLibraries == true -> {
            stringResource(R.string.search_placeholder_all)
        }

        state.selectedLibraryName != null -> {
            stringResource(
                R.string.search_placeholder_library,
                state.selectedLibraryName,
            )
        }

        else -> {
            stringResource(R.string.root_search)
        }
    }

@Composable
private fun searchIdleHint(state: SearchUiState): String =
    when {
        state.searchAllLibraries == null -> {
            stringResource(R.string.search_idle_hint)
        }

        state.searchAllLibraries -> {
            stringResource(R.string.search_idle_hint_all)
        }

        state.selectedLibraryName != null -> {
            stringResource(
                R.string.search_idle_hint_library,
                state.selectedLibraryName,
            )
        }

        else -> {
            stringResource(R.string.search_idle_hint)
        }
    }

private class PreviousResults(
    var list: List<SearchResultUiModel>,
)
