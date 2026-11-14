package org.evoionosp.abstrak.domain.download.model

data class BookDownloadSummary(
    val audiobookId: String,
    val totalTracks: Int,
    val completedTracks: Int,
    val activeTracks: Int,
    val failedTracks: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val status: DownloadStatus,
) {
    val progressFraction: Float?
        get() {
            val total = totalBytes?.takeIf { it > 0 } ?: return null
            return (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
        }

    companion object {
        fun aggregateStatus(statuses: List<DownloadStatus>): DownloadStatus =
            when {
                statuses.any { it == DownloadStatus.DOWNLOADING } -> DownloadStatus.DOWNLOADING
                statuses.any { it == DownloadStatus.QUEUED } -> DownloadStatus.QUEUED
                statuses.any { it == DownloadStatus.REMOVING } -> DownloadStatus.REMOVING
                statuses.any { it == DownloadStatus.FAILED } -> DownloadStatus.FAILED
                else -> DownloadStatus.COMPLETED
            }
    }
}
