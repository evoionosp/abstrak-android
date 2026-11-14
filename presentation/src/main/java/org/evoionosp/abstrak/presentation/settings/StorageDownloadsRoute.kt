package org.evoionosp.abstrak.presentation.settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.ui.download.DeleteDownloadDialog

@Composable
fun StorageDownloadsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    viewModel: StorageDownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StorageDownloadsScreen(
        state = state,
        onBackClick = onBackClick,
        onDownloadWifiOnlyChange = viewModel::onDownloadWifiOnlyChange,
        onAutoDeleteFinishedDownloadsChange = viewModel::onAutoDeleteFinishedDownloadsChange,
        onClearAllClick = viewModel::requestClearAll,
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    )
    if (state.showClearAllDialog) {
        DeleteDownloadDialog(
            itemNames = state.downloadedBookTitles,
            freedBytes = state.storage.downloadsBytes,
            onConfirm = viewModel::confirmClearAll,
            onDismiss = viewModel::dismissClearAll,
            isDeleting = state.isClearing,
        )
    }
}
