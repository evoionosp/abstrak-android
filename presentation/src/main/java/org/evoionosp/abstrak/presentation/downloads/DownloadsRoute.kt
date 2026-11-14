package org.evoionosp.abstrak.presentation.downloads
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.ui.download.DeleteDownloadDialog
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
fun DownloadsRoute(
    onOpenStorageSettings: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    bottomBarPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.isSelectionMode, onBack = viewModel::clearSelection)
    DownloadsScreen(
        state = state,
        bottomBarPadding = bottomBarPadding,
        onAction = { action ->
            when (action) {
                DownloadsAction.OpenStorageSettings -> onOpenStorageSettings()
                is DownloadsAction.OpenBook -> onOpenAudiobook(action.audiobook)
                is DownloadsAction.ToggleSelection -> viewModel.toggleSelection(action.audiobookId)
                DownloadsAction.SelectAll -> viewModel.selectAll()
                DownloadsAction.ClearSelection -> viewModel.clearSelection()
                is DownloadsAction.DeleteBook -> viewModel.requestDelete(action.audiobookId)
                DownloadsAction.DeleteSelected -> viewModel.requestDeleteSelected()
            }
        },
        modifier = modifier,
    )
    state.pendingDelete?.let { pending ->
        DeleteDownloadDialog(
            itemNames = pending.titles,
            freedBytes = pending.bytes,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}
