package org.evoionosp.abstrak.presentation.player
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.player.sheets.PlayerSheet
import org.evoionosp.abstrak.presentation.player.transport.PlayerTransport
import org.evoionosp.abstrak.presentation.ui.book.BookArtworkPanel

@Composable
internal fun NowPlayingPortrait(
    audiobook: Audiobook,
    state: NowPlayingUiState,
    sleepLabel: String,
    speedLabel: String,
    onAction: (PlayerAction) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onOpenBookDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookArtworkPanel(
            coverUrl = audiobook.coverUrl,
            title = audiobook.title,
            author = audiobook.author,
            progress = state.bookProgress,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            titleSpacing = 12.dp,
            onTitleClick = onOpenBookDetails,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PlayerTransport(
            chapterTitle = state.chapterTitle,
            chapterTimeLeftMs = state.chapterTimeLeftMs,
            positionMs = state.playback.currentPositionMs,
            durationMs = state.playback.durationMs,
            isPlaying = state.playback.isPlaying,
            isLoading = state.isLoading,
            onAction = onAction,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PlayerFooterActions(
            sleepLabel = sleepLabel,
            speedLabel = speedLabel,
            onOpenSheet = onOpenSheet,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
internal fun NowPlayingLandscape(
    audiobook: Audiobook,
    state: NowPlayingUiState,
    sleepLabel: String,
    speedLabel: String,
    onAction: (PlayerAction) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onOpenBookDetails: () -> Unit,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
        ) {
            topBar()
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BookArtworkPanel(
                    coverUrl = audiobook.coverUrl,
                    title = audiobook.title,
                    author = audiobook.author,
                    progress = state.bookProgress,
                    onTitleClick = onOpenBookDetails,
                )
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier =
                Modifier
                    .weight(0.6f)
                    .align(Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTransport(
                chapterTitle = state.chapterTitle,
                chapterTimeLeftMs = state.chapterTimeLeftMs,
                positionMs = state.playback.currentPositionMs,
                durationMs = state.playback.durationMs,
                isPlaying = state.playback.isPlaying,
                isLoading = state.isLoading,
                onAction = onAction,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PlayerFooterActions(
                sleepLabel = sleepLabel,
                speedLabel = speedLabel,
                onOpenSheet = onOpenSheet,
            )
        }
    }
}
