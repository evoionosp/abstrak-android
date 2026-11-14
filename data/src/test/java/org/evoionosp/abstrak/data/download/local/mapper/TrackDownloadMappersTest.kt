package org.evoionosp.abstrak.data.download.local.mapper
import org.evoionosp.abstrak.data.test.testTrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackDownloadMappersTest {
    @Test
    fun `toDomain maps every field`() {
        val domain =
            testTrackDownloadEntity(
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = 40L,
                totalBytes = 100L,
                localPath = "/root/track_0.mp3",
            ).toDomain()
        assertEquals("book-1", domain.audiobookId)
        assertEquals(0, domain.trackIndex)
        assertEquals(DownloadStatus.DOWNLOADING, domain.status)
        assertEquals(40L, domain.bytesDownloaded)
        assertEquals(100L, domain.totalBytes)
        assertEquals("/root/track_0.mp3", domain.localPath)
    }

    @Test
    fun `toDomain falls back to queued on unknown status`() {
        assertEquals(
            DownloadStatus.QUEUED,
            testTrackDownloadEntity().copy(status = "SOMEDAY").toDomain().status,
        )
    }

    @Test
    fun `queuedRow starts queued and carries flags`() {
        val row = queuedRow("book-9", 3, wifiOnly = true, updatedAtMillis = 21L)
        assertEquals("book-9", row.audiobookId)
        assertEquals(3, row.trackIndex)
        assertEquals(DownloadStatus.QUEUED.name, row.status)
        assertEquals(true, row.wifiOnly)
        assertEquals(21L, row.updatedAtMillis)
        assertEquals(0L, row.bytesDownloaded)
    }
}
