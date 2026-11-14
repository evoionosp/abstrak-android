package org.evoionosp.abstrak.presentation.bookdetails
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.dialog.ConfirmDialog
import org.evoionosp.abstrak.presentation.ui.download.DeleteDownloadDialog
import org.evoionosp.abstrak.presentation.ui.event.ObserveAsEvents

@Composable
internal fun BookDetailsRoute(
    onBackClick: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    viewModel: BookDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            BookDetailsUiEvent.OpenPlayer -> onOpenPlayer()
        }
    }
    BookDetailsScreen(
        state = uiState,
        onBackClick = onBackClick,
        onAction = { action ->
            when (action) {
                BookDetailsAction.Play -> viewModel.playBook()
                is BookDetailsAction.PlayChapter -> viewModel.playChapter(action.chapter)
                BookDetailsAction.DownloadBook -> viewModel.downloadBook()
                BookDetailsAction.CancelBookDownload -> viewModel.cancelBookDownload()
                BookDetailsAction.DeleteBookDownload -> viewModel.requestDeleteBook()
                is BookDetailsAction.DownloadChapter -> viewModel.downloadChapter(action.index)
                is BookDetailsAction.CancelChapterDownload -> viewModel.cancelChapterDownload(action.index)
                is BookDetailsAction.DeleteChapterDownload -> viewModel.requestDeleteChapter(action.index)
            }
        },
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    )
    uiState.pendingDelete?.let { target ->
        DeleteDownloadDialog(
            itemNames = listOf(target.name),
            freedBytes = target.bytes,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
    uiState.pendingChapterSwitch?.let { chapter ->
        ConfirmDialog(
            title = stringResource(R.string.book_details_play_chapter_title, chapter.title),
            message = stringResource(R.string.book_details_play_chapter_message),
            confirmLabel = stringResource(R.string.play),
            onConfirm = viewModel::confirmChapterSwitch,
            onDismiss = viewModel::dismissChapterSwitch,
        )
    }
}
