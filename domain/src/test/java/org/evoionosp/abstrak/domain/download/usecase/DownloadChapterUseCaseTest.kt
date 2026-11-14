package org.evoionosp.abstrak.domain.download.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.evoionosp.abstrak.domain.testing.testChapter
import org.evoionosp.abstrak.domain.testing.testDetail
import org.evoionosp.abstrak.domain.testing.testTrack
import org.evoionosp.abstrak.domain.testing.testTrackDownload
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadChapterUseCaseTest {
    private val chapters =
        listOf(
            testChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 100),
            testChapter(title = "Ch 2", startInSeconds = 100, endInSeconds = 200),
            testChapter(title = "Ch 3", startInSeconds = 200, endInSeconds = 300),
        )
    private val tracks =
        listOf(
            testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 60),
            testTrack(index = 1, startOffsetInSeconds = 60, durationInSeconds = 60),
            testTrack(index = 2, startOffsetInSeconds = 120, durationInSeconds = 60),
            testTrack(index = 3, startOffsetInSeconds = 180, durationInSeconds = 60),
            testTrack(index = 4, startOffsetInSeconds = 240, durationInSeconds = 60),
        )

    private fun setup(): Pair<FakeAudiobookRepository, FakeDownloadRepository> {
        val catalog = FakeAudiobookRepository()
        catalog.setDetail("lib-1", "book-1", testDetail(tracks = tracks, chapters = chapters))
        return catalog to FakeDownloadRepository()
    }

    @Test
    fun `returns NoPlayableTracks when no detail is cached`() =
        runTest {
            val (catalog, downloads) = setup()
            catalog.setDetail("lib-1", "book-1", null)
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadChapterResult.NoPlayableTracks, useCase("lib-1", "book-1", 0))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `returns UnknownChapter for an out-of-range index`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadChapterResult.UnknownChapter, useCase("lib-1", "book-1", 9))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `enqueues the chapter's overlapping tracks`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadChapterResult.Enqueued(2), useCase("lib-1", "book-1", 0))
            assertEquals(listOf(Triple("book-1", listOf(0, 1), false)), downloads.enqueued)
        }

    @Test
    fun `skips completed tracks`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks("book-1", listOf(testTrackDownload(trackIndex = 0, status = DownloadStatus.COMPLETED)))
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadChapterResult.Enqueued(1), useCase("lib-1", "book-1", 0))
            assertEquals(listOf(Triple("book-1", listOf(1), false)), downloads.enqueued)
        }

    @Test
    fun `returns AlreadyComplete when the chapter is fully on device`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks(
                "book-1",
                listOf(0, 1).map { testTrackDownload(trackIndex = it, status = DownloadStatus.COMPLETED) },
            )
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadChapterResult.AlreadyComplete, useCase("lib-1", "book-1", 0))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `wifi-only preference makes new downloads wifi-only`() =
        runTest {
            val (catalog, downloads) = setup()
            val settings = FakeAppSettingsStore()
            val useCase = DownloadChapterUseCase(catalog, downloads, DownloadScopeCalculator(), settings)
            settings.setDownloadWifiOnly(true)
            useCase("lib-1", "book-1", 0)
            assertEquals(listOf(Triple("book-1", listOf(0, 1), true)), downloads.enqueued)
        }
}
