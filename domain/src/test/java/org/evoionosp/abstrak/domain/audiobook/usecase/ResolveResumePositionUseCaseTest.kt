package org.evoionosp.abstrak.domain.audiobook.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakePendingProgressStore
import org.evoionosp.abstrak.domain.testing.testAudiobook
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveResumePositionUseCaseTest {
    private val repository = FakeAudiobookRepository()
    private val store = FakePendingProgressStore()
    private val useCase = ResolveResumePositionUseCase(repository, store)

    @Test
    fun `resumes from the server's progress`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Success(at(120.0))
            assertEquals(120.0, resume(), 0.001)
        }

    @Test
    fun `resumes from queued progress further along than the server's`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            repository.fetchProgressResult = DomainResult.Success(at(120.0))
            assertEquals(600.0, resume(), 0.001)
        }

    @Test
    fun `resumes from the server's progress further along than the queued`() =
        runTest {
            store.put("lib-1", "book-1", at(60.0))
            repository.fetchProgressResult = DomainResult.Success(at(120.0))
            assertEquals(120.0, resume(), 0.001)
        }

    @Test
    fun `a finished book starts over`() =
        runTest {
            repository.fetchProgressResult =
                DomainResult.Success(PlaybackProgress(298.0, durationSeconds = 300.0, isFinished = true))
            assertEquals(0.0, resume(), 0.001)
        }

    @Test
    fun `starts at the beginning when nothing is recorded anywhere`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Success(null)
            assertEquals(0.0, resume(), 0.001)
        }

    @Test
    fun `offline, resumes from queued progress`() =
        runTest {
            store.put("lib-1", "book-1", at(60.0))
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            repository.continueListening = listOf(testAudiobook().copy(progressSeconds = 30.0))
            assertEquals(60.0, resume(), 0.001)
        }

    @Test
    fun `offline with nothing queued, resumes from Continue Listening`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.NETWORK)
            repository.continueListening =
                listOf(
                    testAudiobook(id = "book-2").copy(progressSeconds = 10.0),
                    testAudiobook().copy(progressSeconds = 42.0),
                )
            assertEquals(42.0, resume(hint = 5.0), 0.001)
        }

    @Test
    fun `offline and off the shelf, resumes where the caller showed the book`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.NETWORK)
            assertEquals(5.0, resume(hint = 5.0), 0.001)
        }

    @Test
    fun `offline with nothing recorded starts at the beginning`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.NETWORK)
            assertEquals(0.0, resume(), 0.001)
        }

    private suspend fun resume(hint: Double? = null): Double =
        useCase("https://example.com", "token", testAudiobook().copy(progressSeconds = hint))

    private fun at(seconds: Double) = PlaybackProgress(seconds, durationSeconds = 300.0, isFinished = false)
}
