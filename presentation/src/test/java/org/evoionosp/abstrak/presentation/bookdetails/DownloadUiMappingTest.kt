package org.evoionosp.abstrak.presentation.bookdetails
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.presentation.ui.download.DownloadButtonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadUiMappingTest {
    private val calculator = DownloadScopeCalculator()
    private val chapters =
        listOf(
            AudiobookChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 100),
            AudiobookChapter(title = "Ch 2", startInSeconds = 100, endInSeconds = 200),
            AudiobookChapter(title = "Ch 3", startInSeconds = 200, endInSeconds = 300),
        )
    private val tracks =
        listOf(
            AudiobookTrack(0, 0, 60, "Track 0", "https://example.com/0.mp3", "audio/mpeg"),
            AudiobookTrack(1, 60, 60, "Track 1", "https://example.com/1.mp3", "audio/mpeg"),
            AudiobookTrack(2, 120, 60, "Track 2", "https://example.com/2.mp3", "audio/mpeg"),
            AudiobookTrack(3, 180, 60, "Track 3", "https://example.com/3.mp3", "audio/mpeg"),
            AudiobookTrack(4, 240, 60, "Track 4", "https://example.com/4.mp3", "audio/mpeg"),
        )

    private fun download(
        trackIndex: Int,
        status: DownloadStatus,
        bytesDownloaded: Long = 0L,
        totalBytes: Long? = null,
    ) = TrackDownload(
        audiobookId = "book-1",
        trackIndex = trackIndex,
        status = status,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        localPath = null,
    )

    @Test
    fun `chapters without rows offer download`() {
        val states = chapterDownloadUiStates(chapters, tracks, emptyList(), 300.0, calculator)
        assertEquals(3, states.size)
        states.forEach { assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, it.status) }
    }

    @Test
    fun `chapter reads downloaded only when every overlapping track is complete`() {
        val downloads =
            listOf(
                download(0, DownloadStatus.COMPLETED, 60, 60),
                download(1, DownloadStatus.COMPLETED, 60, 60),
            )
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADED, states[0].status)
        assertEquals(120L, states[0].downloadedBytes)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, states[1].status)
    }

    @Test
    fun `chapter with active work shows weighted progress`() {
        val downloads =
            listOf(
                download(0, DownloadStatus.COMPLETED, 60, 60),
                download(1, DownloadStatus.DOWNLOADING, 30, 60),
            )
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADING, states[0].status)
        assertEquals(0.75f, states[0].progressFraction)
        assertEquals(90L, states[0].downloadedBytes)
    }

    @Test
    fun `chapter progress is indeterminate while any total is unknown`() {
        val downloads = listOf(download(0, DownloadStatus.DOWNLOADING, 30, null))
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADING, states[0].status)
        assertNull(states[0].progressFraction)
    }

    @Test
    fun `chapter cancelled part way offers download again and keeps its finished bytes`() {
        val downloads = listOf(download(0, DownloadStatus.COMPLETED, 60, 60))
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, states[0].status)
        assertNull(states[0].progressFraction)
        assertEquals(60L, states[0].downloadedBytes)
    }

    @Test
    fun `active work wins over failed tracks`() {
        val downloads =
            listOf(
                download(0, DownloadStatus.FAILED, 30, 60),
                download(1, DownloadStatus.QUEUED, 0, 60),
            )
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADING, states[0].status)
    }

    @Test
    fun `failed chapter offers download again`() {
        val downloads = listOf(download(0, DownloadStatus.FAILED, 10, 60))
        val states = chapterDownloadUiStates(chapters, tracks, downloads, 300.0, calculator)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, states[0].status)
    }

    @Test
    fun `book reads downloaded when the remaining scope is complete`() {
        val downloads =
            listOf(1, 2, 3, 4).map { download(it, DownloadStatus.COMPLETED, 60, 60) }
        val state = bookDownloadUiState(chapters, tracks, downloads, 150.0, false, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADED, state.status)
        assertEquals(240L, state.downloadedBytes)
    }

    @Test
    fun `book shows progress while the scope is fetching`() {
        val downloads =
            listOf(
                download(1, DownloadStatus.COMPLETED, 60, 60),
                download(2, DownloadStatus.DOWNLOADING, 30, 60),
            )
        val state = bookDownloadUiState(chapters, tracks, downloads, 150.0, false, calculator)
        assertEquals(DownloadButtonStatus.DOWNLOADING, state.status)
        assertEquals(0.75f, state.progressFraction)
    }

    @Test
    fun `book cancelled part way offers download again and keeps its finished bytes`() {
        val downloads = listOf(download(1, DownloadStatus.COMPLETED, 60, 60))
        val state = bookDownloadUiState(chapters, tracks, downloads, 150.0, false, calculator)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, state.status)
        assertEquals(60L, state.downloadedBytes)
    }

    @Test
    fun `finished book scopes over every track`() {
        val downloads = listOf(download(0, DownloadStatus.COMPLETED, 60, 60))
        val state = bookDownloadUiState(chapters, tracks, downloads, 299.0, true, calculator)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, state.status)
    }

    @Test
    fun `book without tracks offers download`() {
        val state = bookDownloadUiState(chapters, emptyList(), emptyList(), 0.0, false, calculator)
        assertEquals(DownloadButtonStatus.NOT_DOWNLOADED, state.status)
    }
}
