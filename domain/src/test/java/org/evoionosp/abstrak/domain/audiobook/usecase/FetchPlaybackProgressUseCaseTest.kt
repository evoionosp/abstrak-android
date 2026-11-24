package org.evoionosp.abstrak.domain.audiobook.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakePendingProgressStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FetchPlaybackProgressUseCaseTest {
    private val store = FakePendingProgressStore()

    @Test
    fun `returns recorded progress for resume`() =
        runTest {
            val useCase = useCase(DomainResult.Success(at(120.0)))
            assertEquals(at(120.0), useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `returns null when the server has no progress recorded`() =
        runTest {
            val useCase = useCase(DomainResult.Success(null))
            assertNull(useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `returns null instead of failing when the fetch errors`() =
        runTest {
            val useCase = useCase(DomainResult.Failure(CatalogError.NETWORK))
            assertNull(useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `queued progress updated more recently than the server's wins`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0, lastUpdateMillis = 5L))
            val useCase = useCase(DomainResult.Success(at(120.0, lastUpdateMillis = 4L)))
            assertEquals(at(600.0, lastUpdateMillis = 5L), useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `the server's progress updated more recently than the queued wins`() =
        runTest {
            store.put("lib-1", "book-1", at(60.0))
            val useCase = useCase(DomainResult.Success(at(120.0, lastUpdateMillis = 2L)))
            assertEquals(at(120.0, lastUpdateMillis = 2L), useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `an older queued position never beats a more recent server one`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            val useCase = useCase(DomainResult.Success(at(200.0, lastUpdateMillis = 2L)))
            assertEquals(at(200.0, lastUpdateMillis = 2L), useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `a tie with the server's update keeps the server's progress`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            val useCase = useCase(DomainResult.Success(at(120.0, lastUpdateMillis = 1L)))
            assertEquals(at(120.0, lastUpdateMillis = 1L), useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `returns queued progress when the server can't be reached`() =
        runTest {
            store.put("lib-1", "book-1", at(60.0, lastUpdateMillis = 5L))
            val useCase = useCase(DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE))
            assertEquals(at(60.0, lastUpdateMillis = 5L), useCase("https://example.com", "token", "book-1"))
        }

    private fun useCase(server: DomainResult<PlaybackProgress?>) =
        FetchPlaybackProgressUseCase(FakeAudiobookRepository(fetchProgressResult = server), store)

    private fun at(
        seconds: Double,
        lastUpdateMillis: Long = 0L,
    ) = PlaybackProgress(seconds, durationSeconds = 300.0, isFinished = false, lastUpdateMillis = lastUpdateMillis)
}
