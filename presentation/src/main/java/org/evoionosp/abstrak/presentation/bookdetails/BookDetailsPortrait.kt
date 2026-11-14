package org.evoionosp.abstrak.presentation.bookdetails
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.bookdetails.components.BookActionsRow
import org.evoionosp.abstrak.presentation.bookdetails.components.BookHeroHeader
import org.evoionosp.abstrak.presentation.bookdetails.components.bookChapterItems
import org.evoionosp.abstrak.presentation.designsystem.component.header.rememberCollapsingHeaderState
import org.evoionosp.abstrak.presentation.ui.book.BookProgressBar

private const val DETAILS_HEADER_HEIGHT_FRACTION = 0.4f
private val DETAILS_HEADER_MIN_BAR_HEIGHT = 64.dp
private val DETAILS_HORIZONTAL_PADDING = 16.dp
private val PROGRESS_BAR_HEIGHT = 3.dp
private val PROGRESS_BAR_GAP = 8.dp

@Composable
internal fun BookDetailsPortrait(
    audiobook: Audiobook,
    state: BookDetailsUiState,
    onAction: (BookDetailsAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minHeaderHeight = DETAILS_HEADER_MIN_BAR_HEIGHT + statusBarHeight
    val maxHeaderHeight = (LocalConfiguration.current.screenHeightDp * DETAILS_HEADER_HEIGHT_FRACTION).dp
    val header = rememberCollapsingHeaderState(listState, minHeaderHeight, maxHeaderHeight)
    val headerHeight = with(LocalDensity.current) { header.heightPx.toDp() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(header.nestedScrollConnection),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, (header.heightPx - header.minHeightPx).roundToInt()) },
            contentPadding =
                PaddingValues(
                    top = minHeaderHeight + PROGRESS_BAR_GAP,
                    bottom = 24.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(PROGRESS_BAR_HEIGHT + 16.dp))
            }
            item {
                BookActionsRow(
                    hasProgress = state.hasProgress,
                    download = state.bookDownload,
                    onAction = onAction,
                    modifier = Modifier.padding(horizontal = DETAILS_HORIZONTAL_PADDING),
                )
            }
            if (state.chapters.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.now_playing_chapters_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = DETAILS_HORIZONTAL_PADDING),
                    )
                }
            } else {
                bookChapterItems(state = state, onAction = onAction, horizontalPadding = DETAILS_HORIZONTAL_PADDING)
            }
        }
        BookHeroHeader(
            audiobook = audiobook,
            chaptersCount = state.chapters.size,
            collapseFraction = header.collapseFraction,
            headerHeight = headerHeight,
            onBackClick = onBackClick,
        )
        HeaderProgressBar(
            progress = state.bookProgress,
            collapseFraction = header.collapseFraction,
            headerHeight = headerHeight,
        )
    }
}

@Composable
private fun HeaderProgressBar(
    progress: Float,
    collapseFraction: Float,
    headerHeight: Dp,
) {
    val sidePadding = lerp(DETAILS_HORIZONTAL_PADDING, 0.dp, collapseFraction)
    BookProgressBar(
        progress = progress,
        trackColor =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface,
                collapseFraction,
            ),
        modifier =
            Modifier
                .padding(
                    start = sidePadding,
                    top = headerHeight + lerp(PROGRESS_BAR_GAP, 0.dp, collapseFraction),
                    end = sidePadding,
                ).fillMaxWidth()
                .height(PROGRESS_BAR_HEIGHT)
                .clip(RoundedCornerShape(lerp(PROGRESS_BAR_HEIGHT / 2, 0.dp, collapseFraction))),
    )
}
