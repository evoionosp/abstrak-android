package org.evoionosp.abstrak.domain.download.usecase
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.evoionosp.abstrak.domain.testing.testAudiobook
import org.evoionosp.abstrak.domain.testing.testBookDownloadSummary
import org.evoionosp.abstrak.domain.testing.testTrackDownload
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadManageUseCasesTest {
    @Test
    fun `observe use cases emit the repository state`() =
        runTest {
            val repository = FakeDownloadRepository()
            val tracks = listOf(testTrackDownload(status = DownloadStatus.DOWNLOADING))
            val summary = testBookDownloadSummary(status = DownloadStatus.DOWNLOADING)
            repository.setTracks("book-1", tracks)
            repository.setSummary("book-1", summary)
            ObserveBookDownloadsUseCase(repository)("book-1").test {
                assertEquals(tracks, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            ObserveBookDownloadSummaryUseCase(repository)("book-1").test {
                assertEquals(summary, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `downloaded books emit the repository state`() =
        runTest {
            val repository = FakeDownloadRepository()
            val books =
                listOf(
                    DownloadedBook(
                        audiobook = testAudiobook(id = "book-1", libraryId = "lib-2"),
                        summary = testBookDownloadSummary(status = DownloadStatus.COMPLETED, bytesDownloaded = 42),
                    ),
                )
            repository.downloadedBooksFlow.value = books
            ObserveDownloadedBooksUseCase(repository)().test {
                assertEquals(books, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cancel retry and delete delegate to the repository`() =
        runTest {
            val repository = FakeDownloadRepository()
            CancelBookDownloadUseCase(repository)("book-1")
            RetryBookDownloadUseCase(repository)("book-1")
            DeleteBookDownloadUseCase(repository)("book-1")
            DeleteTrackDownloadUseCase(repository)("book-1", 2)
            assertEquals(listOf("book-1"), repository.cancelledBooks)
            assertEquals(listOf("book-1"), repository.retriedBooks)
            assertEquals(listOf("book-1"), repository.deletedBooks)
            assertEquals(listOf("book-1" to 2), repository.deletedTracks)
        }

    @Test
    fun `storage usage emits the repository state`() =
        runTest {
            val repository = FakeDownloadRepository()
            val usage = StorageUsage(downloadsBytes = 100, cacheBytes = 20, deviceFreeBytes = 30)
            repository.storageFlow.value = usage
            GetStorageUsageUseCase(repository)().test {
                assertEquals(usage, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
