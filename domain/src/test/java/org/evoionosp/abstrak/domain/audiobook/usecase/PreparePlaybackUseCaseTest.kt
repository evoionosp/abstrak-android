package org.evoionosp.abstrak.domain.audiobook.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.testAudiobook
import org.evoionosp.abstrak.domain.testing.testDetail
import org.evoionosp.abstrak.domain.testing.testTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class PreparePlaybackUseCaseTest {
    private val audiobook = testAudiobook()

    @Test
    fun `returns cached detail without refreshing when tracks are present`() =
        runTest {
            val repository = FakeAudiobookRepository()
            repository.setDetail("lib-1", "book-1", testDetail(audiobook))
            val useCase = PreparePlaybackUseCase(repository)
            val result = useCase("https://example.com", "token", "lib-1", "book-1")
            assertEquals(PlaybackPreparation.Ready(testDetail(audiobook)), result)
            assertEquals(emptyList<Pair<String, String>>(), repository.refreshedDetails)
        }

    @Test
    fun `refreshes on a cache miss and returns the newly cached detail`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val fresh = testDetail(tracks = listOf(testTrack(), testTrack(index = 1, startOffsetInSeconds = 300)))
            repository.onRefreshDetail = { libraryId, audiobookId ->
                repository.setDetail(libraryId, audiobookId, fresh)
            }
            val useCase = PreparePlaybackUseCase(repository)
            val result = useCase("https://example.com", "token", "lib-1", "book-1")
            assertEquals(PlaybackPreparation.Ready(fresh), result)
            assertEquals(listOf("lib-1" to "book-1"), repository.refreshedDetails)
        }

    @Test
    fun `refreshes when the cached detail has no tracks`() =
        runTest {
            val repository = FakeAudiobookRepository()
            repository.setDetail("lib-1", "book-1", testDetail(tracks = emptyList()))
            val fresh = testDetail()
            repository.onRefreshDetail = { libraryId, audiobookId ->
                repository.setDetail(libraryId, audiobookId, fresh)
            }
            val useCase = PreparePlaybackUseCase(repository)
            val result = useCase("https://example.com", "token", "lib-1", "book-1")
            assertEquals(PlaybackPreparation.Ready(fresh), result)
            assertEquals(listOf("lib-1" to "book-1"), repository.refreshedDetails)
        }

    @Test
    fun `a book the refresh leaves without tracks has no audio`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val useCase = PreparePlaybackUseCase(repository)
            assertEquals(PlaybackPreparation.NoAudio, useCase("https://example.com", "token", "lib-1", "book-1"))
        }

    @Test
    fun `says why when the refresh fails and nothing is cached`() =
        runTest {
            for (error in listOf(CatalogError.CONNECTIVITY_UNAVAILABLE, CatalogError.NETWORK, CatalogError.NOT_FOUND)) {
                val repository = FakeAudiobookRepository(refreshDetailResult = DomainResult.Failure(error))
                val useCase = PreparePlaybackUseCase(repository)
                assertEquals(
                    PlaybackPreparation.Failed(error),
                    useCase("https://example.com", "token", "lib-1", "book-1"),
                )
            }
        }

    @Test
    fun `cached detail without tracks and a failed refresh says why`() =
        runTest {
            val repository =
                FakeAudiobookRepository(refreshDetailResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE))
            repository.setDetail("lib-1", "book-1", testDetail(tracks = emptyList()))
            val useCase = PreparePlaybackUseCase(repository)
            assertEquals(
                PlaybackPreparation.Failed(CatalogError.CONNECTIVITY_UNAVAILABLE),
                useCase("https://example.com", "token", "lib-1", "book-1"),
            )
        }
}
