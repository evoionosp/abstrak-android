package org.evoionosp.abstrak.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.state.EmptyState
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.designsystem.theme.screenBackgroundBrush
import org.evoionosp.abstrak.presentation.home.components.HorizontalBookRow
import org.evoionosp.abstrak.presentation.home.components.PlaceholderSectionCard
import org.evoionosp.abstrak.presentation.home.components.SectionBlock
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeader
import org.evoionosp.abstrak.presentation.ui.catalog.rememberCatalogBookClick
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    onOpenPlayer: () -> Unit,
    bottomBarPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogEffects(viewModel = viewModel, onSessionExpired = onSessionExpired)
    HomeScreen(
        state = state,
        bottomBarPadding = bottomBarPadding,
        onRefresh = viewModel::refresh,
        onOpenSettings = onOpenSettings,
        onOpenAudiobook = rememberCatalogBookClick(onOpenAudiobook),
        onPlayAudiobook = { audiobook ->
            viewModel.playAudiobook(audiobook)
            onOpenPlayer()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    bottomBarPadding: Dp,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    onPlayAudiobook: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(screenBackgroundBrush())
                    .padding(top = innerPadding.calculateTopPadding()),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = 16.dp,
                        bottom = bottomBarPadding + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    RootHeader(
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.padding(horizontal = AbstrakSpacing.catalog, vertical = 8.dp),
                    ) {
                        Text(
                            text =
                                if (state.username.isNotBlank()) {
                                    stringResource(R.string.home_greeting_named, state.username)
                                } else {
                                    stringResource(R.string.home_greeting)
                                },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (state.libraries.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.home_no_libraries),
                            subtitle = stringResource(R.string.home_no_libraries_hint),
                            modifier = Modifier.fillParentMaxHeight(0.8f),
                        )
                    }
                } else {
                    item {
                        SectionBlock(title = stringResource(R.string.home_continue_listening)) {
                            if (state.continueListening.isEmpty()) {
                                PlaceholderSectionCard(
                                    title = stringResource(R.string.home_continue_empty),
                                    body = stringResource(R.string.home_continue_empty_hint),
                                )
                            } else {
                                HorizontalBookRow(
                                    audiobooks = state.continueListening,
                                    onBookClick = onPlayAudiobook,
                                )
                            }
                        }
                    }
                    item {
                        SectionBlock(title = stringResource(R.string.home_recently_added)) {
                            HorizontalBookRow(
                                audiobooks = state.recentlyAdded,
                                onBookClick = onOpenAudiobook,
                            )
                        }
                    }
                    item {
                        SectionBlock(title = stringResource(R.string.home_discover)) {
                            HorizontalBookRow(
                                audiobooks = state.discover,
                                onBookClick = onOpenAudiobook,
                            )
                        }
                    }
                }
            }
        }
    }
}
