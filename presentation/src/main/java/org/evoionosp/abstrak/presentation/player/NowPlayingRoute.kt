package org.evoionosp.abstrak.presentation.player
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.player.sheets.PlayerSheet
import org.evoionosp.abstrak.presentation.player.sheets.PlayerSheets

@Composable
internal fun NowPlayingRoute(
    audiobook: Audiobook,
    onMinimize: () -> Unit,
    onOpenBookDetails: (Audiobook) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    var openSheet by rememberSaveable { mutableStateOf(PlayerSheet.NONE) }
    NowPlayingScreen(
        audiobook = audiobook,
        state = uiState,
        onAction = viewModel::onPlayerAction,
        onOpenSheet = { openSheet = it },
        onMinimize = onMinimize,
        onOpenBookDetails = { onOpenBookDetails(audiobook) },
        modifier = modifier,
    )
    PlayerSheets(
        openSheet = openSheet,
        state = uiState,
        persistedSleepMinutes = sleepTimerMinutes,
        onAction = viewModel::onPlayerAction,
        onDismiss = { openSheet = PlayerSheet.NONE },
    )
}

@Composable
internal fun MiniPlayerRoute(
    audiobook: Audiobook,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MiniPlayer(
        audiobook = audiobook,
        chapterTitle = uiState.chapterTitle,
        isPlaying = uiState.playback.isPlaying,
        isLoading = uiState.isLoading,
        onAction = viewModel::onPlayerAction,
        modifier = modifier,
    )
}

private fun NowPlayingViewModel.onPlayerAction(action: PlayerAction) {
    when (action) {
        PlayerAction.TogglePlayPause -> togglePlayPause()
        PlayerAction.SeekBackward -> seekBackward()
        PlayerAction.SeekForward -> seekForward()
        is PlayerAction.SeekTo -> seekTo(action.positionMs)
        PlayerAction.PreviousChapter -> previousChapter()
        PlayerAction.NextChapter -> nextChapter()
        is PlayerAction.PlayChapter -> playChapter(action.chapter)
        is PlayerAction.SetSpeed -> setPlaybackSpeed(action.speed)
        is PlayerAction.ArmSleepTimer -> armSleepTimer(action.minutes)
        PlayerAction.ArmSleepEndOfChapter -> armSleepEndOfChapter()
        is PlayerAction.SaveSleepTimerMinutes -> saveSleepTimerMinutes(action.minutes)
        PlayerAction.CancelSleepTimer -> cancelSleepTimer()
    }
}
