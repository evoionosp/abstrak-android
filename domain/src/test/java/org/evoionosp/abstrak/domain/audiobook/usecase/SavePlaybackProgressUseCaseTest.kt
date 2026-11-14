package org.evoionosp.abstrak.domain.audiobook.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgressEvents
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.model.LoginData
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeAuthRepository
import org.evoionosp.abstrak.domain.testing.FakePendingProgressStore
import org.evoionosp.abstrak.domain.testing.FakeProgressSyncScheduler
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.evoionosp.abstrak.domain.testing.RecordedListeningProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavePlaybackProgressUseCaseTest {
    private val repository = FakeAudiobookRepository()
    private val store = FakePendingProgressStore()
    private val scheduler = FakeProgressSyncScheduler()

    @Test
    fun `marks progress finished near the end of the book`() =
        runTest {
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 297.0, totalSeconds = 300.0)
            assertEquals(DomainResult.Success(Unit), result)
            assertEquals(1, repository.savedProgress.size)
            assertTrue(repository.savedProgress.single().isFinished)
            assertEquals(297.0, repository.savedProgress.single().currentTimeSeconds, 0.001)
            assertEquals(300.0, repository.savedProgress.single().durationSeconds!!, 0.001)
        }

    @Test
    fun `leaves progress unfinished mid-book`() =
        runTest {
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 120.0, totalSeconds = 300.0)
            assertFalse(repository.savedProgress.single().isFinished)
        }

    @Test
    fun `leaves progress unfinished when the total duration is unknown`() =
        runTest {
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 297.0, totalSeconds = null)
            assertFalse(repository.savedProgress.single().isFinished)
        }

    @Test
    fun `passes a save failure through to the caller`() =
        runTest {
            repository.saveProgressResult = DomainResult.Failure(CatalogError.NETWORK)
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 10.0, totalSeconds = 300.0)
            assertEquals(DomainResult.Failure(CatalogError.NETWORK), result)
        }

    @Test
    fun `records the position for Continue Listening`() =
        runTest {
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 120.0, totalSeconds = 300.0)
            assertEquals(
                listOf(RecordedListeningProgress("lib-1", "book-1", 120.0, isFinished = false)),
                repository.recordedProgress,
            )
        }

    @Test
    fun `records a finished book as finished`() =
        runTest {
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 297.0, totalSeconds = 300.0)
            assertTrue(repository.recordedProgress.single().isFinished)
        }

    @Test
    fun `records the position even when the server save is skipped offline`() =
        runTest {
            repository.saveProgressResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 60.0, totalSeconds = 300.0)
            assertEquals(60.0, repository.recordedProgress.single().positionSeconds, 0.001)
        }

    @Test
    fun `sends progress made online straight to the server, going back included`() =
        runTest {
            repository.fetchProgressResult = DomainResult.Success(at(600.0))
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 100.0, totalSeconds = TOTAL)
            assertEquals(DomainResult.Success(Unit), result)
            assertEquals(listOf(at(100.0)), repository.savedProgress)
            assertTrue(repository.fetchedProgressBooks.isEmpty())
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `queues progress made offline and asks for a background sync`() =
        runTest {
            repository.saveProgressResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 60.0, totalSeconds = TOTAL)
            assertEquals(at(60.0), store.queued.single().progress)
            assertEquals("lib-1", store.queued.single().libraryId)
            assertEquals(1, scheduler.scheduleCount)
        }

    @Test
    fun `queues progress the server could not take`() =
        runTest {
            for (error in listOf(CatalogError.NETWORK, CatalogError.UNKNOWN)) {
                repository.saveProgressResult = DomainResult.Failure(error)
                useCase()(BASE_URL, TOKEN, "lib-1", "book-$error", absoluteSeconds = 60.0, totalSeconds = TOTAL)
            }
            assertEquals(listOf("book-NETWORK", "book-UNKNOWN"), store.queued.map { it.audiobookId })
        }

    @Test
    fun `does not queue progress the server refused for good`() =
        runTest {
            for (error in listOf(CatalogError.AUTH, CatalogError.NOT_FOUND)) {
                repository.saveProgressResult = DomainResult.Failure(error)
                useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 60.0, totalSeconds = TOTAL)
            }
            assertTrue(store.queued.isEmpty())
            assertEquals(0, scheduler.scheduleCount)
        }

    @Test
    fun `a book with queued progress takes the server's further progress instead of sending its own`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0))
            repository.fetchProgressResult = DomainResult.Success(at(600.0))
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 320.0, totalSeconds = TOTAL)
            assertEquals(DomainResult.Success(Unit), result)
            assertTrue(repository.savedProgress.isEmpty())
            assertTrue(store.queued.isEmpty())
            assertEquals(listOf(320.0, 600.0), repository.recordedProgress.map { it.positionSeconds })
        }

    @Test
    fun `a book with queued progress sends its own when it is further along`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0))
            repository.fetchProgressResult = DomainResult.Success(at(250.0))
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 320.0, totalSeconds = TOTAL)
            assertEquals(DomainResult.Success(Unit), result)
            assertEquals(listOf(at(320.0)), repository.savedProgress)
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `a book with queued progress stays queued while the server can't be reached`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0))
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            val result = useCase()(BASE_URL, TOKEN, "lib-1", "book-1", absoluteSeconds = 320.0, totalSeconds = TOTAL)
            assertEquals(DomainResult.Failure(CatalogError.NETWORK), result)
            assertEquals(at(320.0), store.queued.single().progress)
            assertEquals(1, scheduler.scheduleCount)
        }

    private fun useCase(): SavePlaybackProgressUseCase {
        val sessionStore = FakeSessionStore(testSession())
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        return SavePlaybackProgressUseCase(
            audiobookRepository = repository,
            pendingProgressStore = store,
            syncPendingProgress =
                SyncPendingProgressUseCase(
                    pendingProgressStore = store,
                    audiobookRepository = repository,
                    getValidSessionUseCase =
                        GetValidSessionUseCase(sessionStore, SessionRefreshCoordinator(sessionStore, auth)),
                    pulledProgressEvents = PulledProgressEvents(),
                ),
            progressSyncScheduler = scheduler,
            calculator = PlaybackPositionCalculator(),
        )
    }

    private fun testSession() =
        LoginSession(
            accessToken = TOKEN,
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = BASE_URL,
        )

    private fun at(seconds: Double) = PlaybackProgress(seconds, durationSeconds = TOTAL, isFinished = false)

    private companion object {
        const val BASE_URL = "https://example.com"
        const val TOKEN = "token"
        const val TOTAL = 7_200.0
    }
}
