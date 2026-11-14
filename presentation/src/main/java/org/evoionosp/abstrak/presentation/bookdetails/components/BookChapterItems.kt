package org.evoionosp.abstrak.presentation.bookdetails.components
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import org.evoionosp.abstrak.presentation.bookdetails.BookDetailsAction
import org.evoionosp.abstrak.presentation.bookdetails.BookDetailsUiState
import org.evoionosp.abstrak.presentation.ui.chapter.ChapterRowDefaults
import org.evoionosp.abstrak.presentation.ui.chapter.chapterItems
import org.evoionosp.abstrak.presentation.ui.download.DownloadIconButton
import org.evoionosp.abstrak.presentation.ui.download.DownloadUiState

internal fun LazyListScope.bookChapterItems(
    state: BookDetailsUiState,
    onAction: (BookDetailsAction) -> Unit,
    horizontalPadding: Dp,
) {
    chapterItems(
        chapters = state.chapters,
        currentIndex = state.currentChapterIndex,
        onPlay = { onAction(BookDetailsAction.PlayChapter(it)) },
        contentPadding = ChapterRowDefaults.contentPadding(horizontal = horizontalPadding),
        trailing = { index ->
            DownloadIconButton(
                state = state.chapterDownloads.getOrElse(index) { DownloadUiState() },
                onDownload = { onAction(BookDetailsAction.DownloadChapter(index)) },
                onCancel = { onAction(BookDetailsAction.CancelChapterDownload(index)) },
                onDelete = { onAction(BookDetailsAction.DeleteChapterDownload(index)) },
            )
        },
    )
}
