package org.evoionosp.abstrak.presentation.bookdetails
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.presentation.ui.download.DownloadButtonStatus
import org.evoionosp.abstrak.presentation.ui.download.DownloadUiState

private val ACTIVE_STATUSES =
    setOf(
        DownloadStatus.QUEUED,
        DownloadStatus.DOWNLOADING,
    )

internal fun chapterDownloadUiStates(
    chapters: List<AudiobookChapter>,
    tracks: List<AudiobookTrack>,
    downloads: List<TrackDownload>,
    totalSeconds: Double,
    calculator: DownloadScopeCalculator,
): List<DownloadUiState> =
    chapters.indices.map { index ->
        val overlapping =
            calculator.chapterTrackIndexes(
                chapterIndex = index,
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = totalSeconds,
            )
        val rows = downloads.filter { it.trackIndex in overlapping }
        when {
            overlapping.isEmpty() || rows.isEmpty() -> {
                DownloadUiState()
            }

            rows.size == overlapping.size && rows.all { it.status == DownloadStatus.COMPLETED } -> {
                DownloadUiState(
                    status = DownloadButtonStatus.DOWNLOADED,
                    downloadedBytes = rows.sumOf { it.bytesDownloaded },
                )
            }

            rows.any { it.status in ACTIVE_STATUSES } -> {
                DownloadUiState(
                    status = DownloadButtonStatus.DOWNLOADING,
                    progressFraction = combinedProgress(rows),
                    downloadedBytes = rows.sumOf { it.bytesDownloaded },
                )
            }

            else -> {
                DownloadUiState(downloadedBytes = rows.sumOf { it.bytesDownloaded })
            }
        }
    }

internal fun bookDownloadUiState(
    chapters: List<AudiobookChapter>,
    tracks: List<AudiobookTrack>,
    downloads: List<TrackDownload>,
    positionSeconds: Double,
    isFinished: Boolean,
    calculator: DownloadScopeCalculator,
): DownloadUiState {
    val scope =
        calculator.bookTrackIndexes(
            chapters = chapters,
            tracks = tracks,
            positionSeconds = positionSeconds,
            isFinished = isFinished,
        )
    if (scope.isEmpty()) return DownloadUiState()
    val rows = downloads.filter { it.trackIndex in scope }
    val completed = rows.filter { it.status == DownloadStatus.COMPLETED }.map { it.trackIndex }.toSet()
    return when {
        completed.containsAll(scope) -> {
            DownloadUiState(
                status = DownloadButtonStatus.DOWNLOADED,
                downloadedBytes = rows.sumOf { it.bytesDownloaded },
            )
        }

        rows.any { it.status in ACTIVE_STATUSES } -> {
            DownloadUiState(
                status = DownloadButtonStatus.DOWNLOADING,
                progressFraction = combinedProgress(rows),
                downloadedBytes = rows.sumOf { it.bytesDownloaded },
            )
        }

        else -> {
            DownloadUiState(downloadedBytes = rows.sumOf { it.bytesDownloaded })
        }
    }
}

private fun combinedProgress(rows: List<TrackDownload>): Float? {
    var total = 0L
    var downloaded = 0L
    rows.forEach { row ->
        total += row.totalBytes ?: return null
        downloaded += row.bytesDownloaded
    }
    if (total <= 0) return null
    return (downloaded.toFloat() / total).coerceIn(0f, 1f)
}
