package org.evoionosp.abstrak.presentation.player.sheets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.component.sheet.AbstrakBottomSheet
import org.evoionosp.abstrak.presentation.designsystem.component.sheet.AbstrakBottomSheetDefaults
import org.evoionosp.abstrak.presentation.player.NowPlayingUiState
import org.evoionosp.abstrak.presentation.player.PlayerAction

enum class PlayerSheet {
    NONE,
    SPEED,
    CHAPTERS,
    SLEEP,
}

@Composable
internal fun PlayerSheets(
    openSheet: PlayerSheet,
    state: NowPlayingUiState,
    persistedSleepMinutes: Int,
    onAction: (PlayerAction) -> Unit,
    onDismiss: () -> Unit,
) {
    when (openSheet) {
        PlayerSheet.NONE -> {
            Unit
        }

        PlayerSheet.SPEED -> {
            SpeedSheet(
                speed = state.playback.playbackSpeed,
                onSpeedChange = { onAction(PlayerAction.SetSpeed(it)) },
                onDismiss = onDismiss,
            )
        }

        PlayerSheet.CHAPTERS -> {
            ChaptersSheet(
                chapters = state.chapters,
                currentChapterIndex = state.currentChapterIndex,
                positionSeconds = state.playback.currentBookPositionSeconds,
                totalSeconds = state.totalSeconds,
                isPlaying = state.playback.isPlaying,
                onPlayChapter = { onAction(PlayerAction.PlayChapter(it)) },
                onDismiss = onDismiss,
            )
        }

        PlayerSheet.SLEEP -> {
            SleepSheet(
                sleepTimer = state.playback.sleepTimer,
                sleepRemainingMs = state.playback.sleepRemainingMs,
                currentChapterTitle = state.chapterTitle,
                endOfChapterAvailable = state.endOfChapterAvailable,
                persistedMinutes = persistedSleepMinutes,
                onAction = onAction,
                onDismiss = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerBottomSheet(
    onDismiss: () -> Unit,
    title: String,
    contentPadding: PaddingValues = AbstrakBottomSheetDefaults.ContentPadding,
    titlePadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(24.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    AbstrakBottomSheet(
        onDismiss = onDismiss,
        title = title,
        skipPartiallyExpanded = true,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        },
        contentPadding = contentPadding,
        titlePadding = titlePadding,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}
