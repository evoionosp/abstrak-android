package org.evoionosp.abstrak.domain.download
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.testing.testBookDownloadSummary
import org.evoionosp.abstrak.domain.testing.testTrackDownload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadModelsTest {
    @Test
    fun `aggregate status prefers active work over failure over completed`() {
        assertEquals(
            DownloadStatus.DOWNLOADING,
            BookDownloadSummary.aggregateStatus(listOf(DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING)),
        )
        assertEquals(
            DownloadStatus.QUEUED,
            BookDownloadSummary.aggregateStatus(listOf(DownloadStatus.FAILED, DownloadStatus.QUEUED)),
        )
        assertEquals(
            DownloadStatus.REMOVING,
            BookDownloadSummary.aggregateStatus(listOf(DownloadStatus.FAILED, DownloadStatus.REMOVING)),
        )
        assertEquals(
            DownloadStatus.FAILED,
            BookDownloadSummary.aggregateStatus(listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED)),
        )
        assertEquals(
            DownloadStatus.COMPLETED,
            BookDownloadSummary.aggregateStatus(listOf(DownloadStatus.COMPLETED)),
        )
        assertEquals(DownloadStatus.COMPLETED, BookDownloadSummary.aggregateStatus(emptyList()))
    }

    @Test
    fun `track progress is null while the total is unknown`() {
        assertEquals(0.5f, testTrackDownload(bytesDownloaded = 50, totalBytes = 100).progressFraction)
        assertNull(testTrackDownload(totalBytes = null).progressFraction)
        assertNull(testTrackDownload(totalBytes = 0).progressFraction)
        assertEquals(1f, testTrackDownload(bytesDownloaded = 150, totalBytes = 100).progressFraction)
    }

    @Test
    fun `summary progress is null while the total is unknown`() {
        assertEquals(0.25f, testBookDownloadSummary(bytesDownloaded = 25, totalBytes = 100).progressFraction)
        assertNull(testBookDownloadSummary(totalBytes = null).progressFraction)
    }

    @Test
    fun `abstrak bytes are the downloads plus the cache`() {
        assertEquals(130L, StorageUsage(downloadsBytes = 100, cacheBytes = 30, deviceFreeBytes = 500).abstrakBytes)
        assertEquals(0L, StorageUsage(downloadsBytes = 0, cacheBytes = 0, deviceFreeBytes = 500).abstrakBytes)
    }
}
