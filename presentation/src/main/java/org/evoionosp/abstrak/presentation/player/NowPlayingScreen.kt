package org.evoionosp.abstrak.presentation.player
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.collapsingTitle
import org.evoionosp.abstrak.presentation.player.sheets.PlayerSheet
import org.evoionosp.abstrak.presentation.ui.format.formatPlaybackSpeed

@Composable
internal fun NowPlayingScreen(
    audiobook: Audiobook,
    state: NowPlayingUiState,
    onAction: (PlayerAction) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onMinimize: () -> Unit,
    onOpenBookDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sleepLabel = state.sleepFooter.label()
    val speedLabel = formatPlaybackSpeed(state.playback.playbackSpeed)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLandscape) {
                NowPlayingLandscape(
                    audiobook = audiobook,
                    state = state,
                    sleepLabel = sleepLabel,
                    speedLabel = speedLabel,
                    onAction = onAction,
                    onOpenSheet = onOpenSheet,
                    onOpenBookDetails = onOpenBookDetails,
                    topBar = { PlayerTopBar(onMinimize = onMinimize) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                PlayerTopBar(onMinimize = onMinimize)
                NowPlayingPortrait(
                    audiobook = audiobook,
                    state = state,
                    sleepLabel = sleepLabel,
                    speedLabel = speedLabel,
                    onAction = onAction,
                    onOpenSheet = onOpenSheet,
                    onOpenBookDetails = onOpenBookDetails,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(onMinimize: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onMinimize,
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.close),
            )
        }
        Text(
            text = stringResource(R.string.now_playing_title),
            modifier = Modifier.padding(start = 18.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.collapsingTitle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SleepFooter.label(): String =
    when (this) {
        SleepFooter.Off -> stringResource(R.string.now_playing_sleep_off)
        is SleepFooter.Minutes -> stringResource(R.string.now_playing_sleep_minutes, minutes)
        SleepFooter.EndOfChapter -> stringResource(R.string.now_playing_sleep_chapter_short)
    }
