package org.evoionosp.abstrak.presentation.ui.download

enum class DownloadButtonStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
}

data class DownloadUiState(
    val status: DownloadButtonStatus = DownloadButtonStatus.NOT_DOWNLOADED,
    val progressFraction: Float? = null,
    val downloadedBytes: Long = 0L,
)
