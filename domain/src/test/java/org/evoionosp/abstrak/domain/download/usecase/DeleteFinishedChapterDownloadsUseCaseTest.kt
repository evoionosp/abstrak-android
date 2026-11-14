package org.evoionosp.abstrak.domain.download.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.evoionosp.abstrak.domain.testing.testChapter
import org.evoionosp.abstrak.domain.testing.testDetail
import org.evoionosp.abstrak.domain.testing.testTrack
import org.evoionosp.abstrak.domain.testing.testTrackDownload
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteFinishedChapterDownloadsUseCaseTest {
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

    private fun completedDownloads(vararg trackIndexes: Int): List<TrackDownload> =
        trackIndexes.map { testTrackDownload(trackIndex = it, status = DownloadStatus.COMPLETED) }

    @Test
    fun `deletes completed tracks of finished chapters only`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks("book-1", completedDownloads(0, 1, 2, 3, 4))
            val useCase = DeleteFinishedChapterDownloadsUseCase(catalog, downloads, DownloadScopeCalculator())
            val result = useCase("lib-1", "book-1", setOf(0, 1))
            assertEquals(DeleteFinishedChapterDownloadsResult.Deleted(3), result)
            assertEquals(
                listOf("book-1" to 0, "book-1" to 1, "book-1" to 2),
                downloads.deletedTracks,
            )
        }

    @Test
    fun `keeps the tracks of a skipped chapter`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks("book-1", completedDownloads(0, 1, 2, 3, 4))
            val useCase = DeleteFinishedChapterDownloadsUseCase(catalog, downloads, DownloadScopeCalculator())
            val result = useCase("lib-1", "book-1", setOf(1))
            assertEquals(DeleteFinishedChapterDownloadsResult.Deleted(1), result)
            assertEquals(listOf("book-1" to 2), downloads.deletedTracks)
        }

    @Test
    fun `leaves unfinished tracks of finished chapters alone`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks(
                "book-1",
                listOf(
                    testTrackDownload(trackIndex = 0, status = DownloadStatus.COMPLETED),
                    testTrackDownload(trackIndex = 1, status = DownloadStatus.DOWNLOADING),
                ),
            )
            val useCase = DeleteFinishedChapterDownloadsUseCase(catalog, downloads, DownloadScopeCalculator())
            val result = useCase("lib-1", "book-1", setOf(0))
            assertEquals(DeleteFinishedChapterDownloadsResult.Deleted(1), result)
            assertEquals(listOf("book-1" to 0), downloads.deletedTracks)
        }

    @Test
    fun `unknown chapter deletes nothing`() =
        runTest {
            val (catalog, downloads) = setup()
            downloads.setTracks("book-1", completedDownloads(0))
            val useCase = DeleteFinishedChapterDownloadsUseCase(catalog, downloads, DownloadScopeCalculator())
            val result = useCase("lib-1", "book-1", setOf(9))
            assertEquals(DeleteFinishedChapterDownloadsResult.UnknownChapter, result)
            assertEquals(emptyList<Pair<String, Int>>(), downloads.deletedTracks)
        }

    @Test
    fun `returns NoPlayableTracks when no detail is cached`() =
        runTest {
            val (catalog, downloads) = setup()
            catalog.setDetail("lib-1", "book-1", null)
            val useCase = DeleteFinishedChapterDownloadsUseCase(catalog, downloads, DownloadScopeCalculator())
            assertEquals(
                DeleteFinishedChapterDownloadsResult.NoPlayableTracks,
                useCase("lib-1", "book-1", setOf(0)),
            )
            assertEquals(emptyList<Pair<String, Int>>(), downloads.deletedTracks)
        }
}
