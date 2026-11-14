package org.evoionosp.abstrak.presentation.downloads
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.toUiModel

enum class DownloadedBookStatus {
    DOWNLOADED,
    DOWNLOADING,
    FAILED,
}

data class DownloadedBookUiModel(
    val audiobook: AudiobookUiModel,
    val sizeBytes: Long,
    val status: DownloadedBookStatus,
    val progressPercent: Int?,
)

data class PendingDownloadsDelete(
    val titles: List<String>,
    val bytes: Long,
)

data class DownloadsUiState(
    val isLoaded: Boolean = false,
    val books: List<DownloadedBookUiModel> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val pendingDelete: PendingDownloadsDelete? = null,
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
}

internal fun downloadsUiState(
    books: List<DownloadedBook>,
    selectedIds: Set<String>,
    pendingDeleteIds: Set<String>,
): DownloadsUiState {
    val listedIds = books.mapTo(HashSet()) { it.audiobook.id }
    val pendingBooks = books.filter { it.audiobook.id in pendingDeleteIds }
    return DownloadsUiState(
        isLoaded = true,
        books = books.map { it.toUiModel() },
        selectedIds = selectedIds intersect listedIds,
        pendingDelete =
            pendingBooks.takeIf { it.isNotEmpty() }?.let { pending ->
                PendingDownloadsDelete(
                    titles = pending.map { it.audiobook.title },
                    bytes = pending.sumOf { it.summary.bytesDownloaded },
                )
            },
    )
}

internal fun DownloadedBook.toUiModel(): DownloadedBookUiModel {
    val status =
        when (summary.status) {
            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> DownloadedBookStatus.DOWNLOADING
            DownloadStatus.FAILED -> DownloadedBookStatus.FAILED
            DownloadStatus.COMPLETED, DownloadStatus.REMOVING -> DownloadedBookStatus.DOWNLOADED
        }
    return DownloadedBookUiModel(
        audiobook = audiobook.toUiModel(),
        sizeBytes = summary.bytesDownloaded,
        status = status,
        progressPercent =
            summary.progressFraction
                ?.takeIf { status == DownloadedBookStatus.DOWNLOADING }
                ?.let { (it * 100).toInt() },
    )
}
