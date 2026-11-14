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

class DownloadBookUseCaseTest {
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
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadBookResult.NoPlayableTracks, useCase("lib-1", "book-1", 150.0))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `returns NoPlayableTracks when the detail has no tracks`() =
        runTest {
            val (catalog, downloads) = setup()
            catalog.setDetail("lib-1", "book-1", testDetail(tracks = emptyList(), chapters = chapters))
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadBookResult.NoPlayableTracks, useCase("lib-1", "book-1", null))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `enqueues remaining tracks from the current chapter`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadBookResult.Enqueued(4), useCase("lib-1", "book-1", 150.0))
            assertEquals(listOf(Triple("book-1", listOf(1, 2, 3, 4), false)), downloads.enqueued)
        }

    @Test
    fun `null position enqueues every track`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadBookResult.Enqueued(5), useCase("lib-1", "book-1", null))
            assertEquals(listOf(Triple("book-1", listOf(0, 1, 2, 3, 4), false)), downloads.enqueued)
        }

    @Test
    fun `finished book enqueues every track`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            val result = useCase(libraryId = "lib-1", audiobookId = "book-1", positionSeconds = 299.0, isFinished = true)
            assertEquals(DownloadBookResult.Enqueued(5), result)
            assertEquals(listOf(Triple("book-1", listOf(0, 1, 2, 3, 4), false)), downloads.enqueued)
        }

    @Test
    fun `reports AlreadyComplete when the remaining scope is on device`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks(
                "book-1",
                listOf(1, 2, 3, 4).map { testTrackDownload(trackIndex = it, status = DownloadStatus.COMPLETED) },
            )
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            assertEquals(DownloadBookResult.AlreadyComplete, useCase("lib-1", "book-1", 150.0))
            assertEquals(emptyList<Triple<String, List<Int>, Boolean>>(), downloads.enqueued)
        }

    @Test
    fun `passes wifiOnly through to the repository`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), FakeAppSettingsStore())
            useCase(libraryId = "lib-1", audiobookId = "book-1", positionSeconds = null, wifiOnly = true)
            assertEquals(listOf(Triple("book-1", listOf(0, 1, 2, 3, 4), true)), downloads.enqueued)
        }

    @Test
    fun `wifi-only preference makes new downloads wifi-only`() =
        runTest {
            val (catalog, downloads) = setup()
            val settings = FakeAppSettingsStore()
            val useCase = DownloadBookUseCase(catalog, downloads, DownloadScopeCalculator(), settings)
            settings.setDownloadWifiOnly(true)
            useCase(libraryId = "lib-1", audiobookId = "book-1", positionSeconds = null)
            assertEquals(listOf(Triple("book-1", listOf(0, 1, 2, 3, 4), true)), downloads.enqueued)
        }
}
