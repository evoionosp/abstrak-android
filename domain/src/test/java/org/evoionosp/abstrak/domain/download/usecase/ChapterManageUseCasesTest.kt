package org.evoionosp.abstrak.domain.download.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.evoionosp.abstrak.domain.testing.testChapter
import org.evoionosp.abstrak.domain.testing.testDetail
import org.evoionosp.abstrak.domain.testing.testTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterManageUseCasesTest {
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
    fun `cancel chapter cancels its overlapping tracks`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = CancelChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(CancelChapterDownloadResult.Cancelled(2), useCase("lib-1", "book-1", 0))
            assertEquals(listOf("book-1" to listOf(0, 1)), downloads.cancelledTracks)
        }

    @Test
    fun `cancel chapter reports unknown chapter without touching the repo`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = CancelChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(CancelChapterDownloadResult.UnknownChapter, useCase("lib-1", "book-1", 9))
            assertEquals(emptyList<Pair<String, List<Int>>>(), downloads.cancelledTracks)
        }

    @Test
    fun `cancel chapter reports missing detail without touching the repo`() =
        runTest {
            val (catalog, downloads) = setup()
            catalog.setDetail("lib-1", "book-1", null)
            val useCase = CancelChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(CancelChapterDownloadResult.NoPlayableTracks, useCase("lib-1", "book-1", 0))
            assertEquals(emptyList<Pair<String, List<Int>>>(), downloads.cancelledTracks)
        }

    @Test
    fun `delete chapter deletes its overlapping tracks`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DeleteChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(DeleteChapterDownloadResult.Deleted(2), useCase("lib-1", "book-1", 0))
            assertEquals(listOf("book-1" to 0, "book-1" to 1), downloads.deletedTracks)
        }

    @Test
    fun `delete chapter reports unknown chapter without touching the repo`() =
        runTest {
            val (catalog, downloads) = setup()
            val useCase = DeleteChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(DeleteChapterDownloadResult.UnknownChapter, useCase("lib-1", "book-1", 9))
            assertEquals(emptyList<Pair<String, Int>>(), downloads.deletedTracks)
        }

    @Test
    fun `delete chapter reports missing detail without touching the repo`() =
        runTest {
            val (catalog, downloads) = setup()
            catalog.setDetail("lib-1", "book-1", null)
            val useCase = DeleteChapterDownloadUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(DeleteChapterDownloadResult.NoPlayableTracks, useCase("lib-1", "book-1", 0))
            assertEquals(emptyList<Pair<String, Int>>(), downloads.deletedTracks)
        }
}
