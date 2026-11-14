package org.evoionosp.abstrak.domain.download.model

data class TrackDownload(
    val audiobookId: String,
    val trackIndex: Int,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long? = null,
    val localPath: String? = null,
) {
    val progressFraction: Float?
        get() {
            val total = totalBytes?.takeIf { it > 0 } ?: return null
            return (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
        }
}
