package org.evoionosp.abstrak.data.test
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.data.download.work.DownloadWorkScheduler
import org.evoionosp.abstrak.domain.download.model.DownloadStatus

class FakeDownloadWorkScheduler : DownloadWorkScheduler {
    data class Scheduled(
        val audiobookId: String,
        val trackIndexes: List<Int>,
        val wifiOnly: Boolean,
    )

    val scheduled = mutableListOf<Scheduled>()
    val allowedAnyNetwork = mutableListOf<Pair<String, List<Int>>>()
    val cancelledBooks = mutableListOf<String>()
    var cancelAllCount = 0

    override fun scheduleBook(
        audiobookId: String,
        trackIndexes: List<Int>,
        wifiOnly: Boolean,
    ) {
        scheduled += Scheduled(audiobookId, trackIndexes, wifiOnly)
    }

    override suspend fun allowAnyNetwork(
        audiobookId: String,
        trackIndexes: List<Int>,
    ) {
        allowedAnyNetwork += audiobookId to trackIndexes
    }

    override fun cancelBook(audiobookId: String) {
        cancelledBooks += audiobookId
    }

    override fun cancelAll() {
        cancelAllCount++
    }
}

fun testTrackDownloadEntity(
    audiobookId: String = "book-1",
    trackIndex: Int = 0,
    status: DownloadStatus = DownloadStatus.QUEUED,
    bytesDownloaded: Long = 0L,
    totalBytes: Long? = null,
    localPath: String? = null,
    wifiOnly: Boolean = false,
    etag: String? = null,
    lastModified: String? = null,
    trackIno: String? = null,
    updatedAtMillis: Long = 0L,
) = TrackDownloadEntity(
    audiobookId = audiobookId,
    trackIndex = trackIndex,
    status = status.name,
    bytesDownloaded = bytesDownloaded,
    totalBytes = totalBytes,
    localPath = localPath,
    wifiOnly = wifiOnly,
    etag = etag,
    lastModified = lastModified,
    trackIno = trackIno,
    updatedAtMillis = updatedAtMillis,
)
