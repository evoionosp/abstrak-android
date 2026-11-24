package org.evoionosp.abstrak.domain.audiobook.usecase
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgress
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
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.evoionosp.abstrak.domain.testing.RecordedListeningProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPendingProgressUseCaseTest {
    private val store = FakePendingProgressStore()
    private val repository = FakeAudiobookRepository()
    private val events = PulledProgressEvents()

    @Test
    fun `sends queued progress updated more recently than the server's, dated when it was saved`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(300.0, lastUpdateMillis = 4L))
            val result = sync()()
            assertEquals(ProgressSyncResult.DONE, result)
            assertEquals(listOf(at(600.0, lastUpdateMillis = 5L)), repository.savedProgress)
            assertTrue(store.queued.isEmpty())
            assertTrue(repository.recordedProgress.isEmpty())
        }

    @Test
    fun `never sends an older device position when the server's is more recent`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(200.0, lastUpdateMillis = 6L))
            events.pulled.test {
                val result = sync()()
                assertEquals(ProgressSyncResult.DONE, result)
                assertTrue(repository.savedProgress.isEmpty())
                assertTrue(store.queued.isEmpty())
                assertEquals(
                    listOf(RecordedListeningProgress("lib-1", "book-1", 200.0, isFinished = false)),
                    repository.recordedProgress,
                )
                assertEquals(PulledProgress("book-1", at(200.0, lastUpdateMillis = 6L)), awaitItem())
            }
        }

    @Test
    fun `sends queued progress the server has none of`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(null)
            sync()()
            assertEquals(listOf(at(600.0, lastUpdateMillis = 5L)), repository.savedProgress)
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `pulls server progress updated more recently than the queued instead of sending it`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(600.0, lastUpdateMillis = 6L))
            events.pulled.test {
                val result = sync()()
                assertEquals(ProgressSyncResult.DONE, result)
                assertTrue(repository.savedProgress.isEmpty())
                assertTrue(store.queued.isEmpty())
                assertEquals(
                    listOf(RecordedListeningProgress("lib-1", "book-1", 600.0, isFinished = false)),
                    repository.recordedProgress,
                )
                assertEquals(PulledProgress("book-1", at(600.0, lastUpdateMillis = 6L)), awaitItem())
            }
        }

    @Test
    fun `a tie with the server's update keeps the server's progress`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(300.0, lastUpdateMillis = 5L))
            sync()()
            assertTrue(repository.savedProgress.isEmpty())
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `pulls a book finished on another device`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(finished(lastUpdateMillis = 6L))
            sync()()
            assertTrue(repository.savedProgress.isEmpty())
            assertTrue(repository.recordedProgress.single().isFinished)
        }

    @Test
    fun `sends a book finished on this device more recently`() =
        runTest {
            store.put("lib-1", "book-1", finished(lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(600.0, lastUpdateMillis = 4L))
            sync()()
            assertEquals(listOf(finished(lastUpdateMillis = 5L)), repository.savedProgress)
        }

    @Test
    fun `keeps the queue while the server can't be reached`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            val result = sync()()
            assertEquals(ProgressSyncResult.RETRY, result)
            assertEquals(1, store.queued.size)
            assertTrue(repository.savedProgress.isEmpty())
        }

    @Test
    fun `keeps the queue when the server fails the save`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            repository.saveProgressResult = DomainResult.Failure(CatalogError.UNKNOWN)
            assertEquals(ProgressSyncResult.RETRY, sync()())
            assertEquals(1, store.queued.size)
        }

    @Test
    fun `stops without a session, keeping the queue`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            val result = sync(session = null)()
            assertEquals(ProgressSyncResult.SIGNED_OUT, result)
            assertEquals(1, store.queued.size)
            assertTrue(repository.fetchedProgressBooks.isEmpty())
        }

    @Test
    fun `stops when the server refuses the session`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            repository.fetchProgressResult = DomainResult.Failure(CatalogError.AUTH)
            assertEquals(ProgressSyncResult.SIGNED_OUT, sync()())
            assertEquals(1, store.queued.size)
        }

    @Test
    fun `drops progress of a book gone from the server`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            repository.saveProgressResult = DomainResult.Failure(CatalogError.NOT_FOUND)
            assertEquals(ProgressSyncResult.DONE, sync()())
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `a save made while the progress was being sent stays queued`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0, lastUpdateMillis = 5L))
            store.beforeRemove = { store.put("lib-1", "book-1", at(615.0, lastUpdateMillis = 7L)) }
            val result = sync()()
            assertEquals(ProgressSyncResult.RETRY, result)
            assertEquals(at(615.0, lastUpdateMillis = 7L), store.queued.single().progress)
        }

    @Test
    fun `a save made while the server's progress was being pulled is not overwritten`() =
        runTest {
            store.put("lib-1", "book-1", at(300.0, lastUpdateMillis = 5L))
            repository.fetchProgressResult = DomainResult.Success(at(600.0, lastUpdateMillis = 6L))
            store.beforeRemove = { store.put("lib-1", "book-1", at(900.0, lastUpdateMillis = 7L)) }
            sync()()
            assertTrue(repository.recordedProgress.isEmpty())
            assertEquals(at(900.0, lastUpdateMillis = 7L), store.queued.single().progress)
        }

    @Test
    fun `syncs every queued book`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            store.put("lib-1", "book-2", at(100.0))
            repository.fetchProgressByBook = mapOf("book-2" to DomainResult.Success(at(500.0, lastUpdateMillis = 3L)))
            assertEquals(ProgressSyncResult.DONE, sync()())
            assertEquals(listOf("book-1"), repository.savedProgressBooks)
            assertEquals(listOf("book-2"), repository.recordedProgress.map { it.audiobookId })
            assertTrue(store.queued.isEmpty())
        }

    @Test
    fun `syncs only the book asked for`() =
        runTest {
            store.put("lib-1", "book-1", at(600.0))
            store.put("lib-1", "book-2", at(100.0))
            assertEquals(ProgressSyncResult.DONE, sync()("book-2"))
            assertEquals(listOf("book-2"), repository.fetchedProgressBooks)
            assertEquals(listOf("book-1"), store.queued.map { it.audiobookId })
        }

    @Test
    fun `nothing queued is done without asking the server`() =
        runTest {
            assertEquals(ProgressSyncResult.DONE, sync()())
            assertTrue(repository.fetchedProgressBooks.isEmpty())
        }

    private fun sync(session: LoginSession? = testSession()): SyncPendingProgressUseCase {
        val sessionStore = FakeSessionStore(session)
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        return SyncPendingProgressUseCase(
            pendingProgressStore = store,
            audiobookRepository = repository,
            getValidSessionUseCase = GetValidSessionUseCase(sessionStore, SessionRefreshCoordinator(sessionStore, auth)),
            pulledProgressEvents = events,
        )
    }

    private fun testSession() =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    private fun at(
        seconds: Double,
        lastUpdateMillis: Long = 0L,
    ) = PlaybackProgress(seconds, durationSeconds = 7_200.0, isFinished = false, lastUpdateMillis = lastUpdateMillis)

    private fun finished(lastUpdateMillis: Long = 0L) =
        PlaybackProgress(7_190.0, durationSeconds = 7_200.0, isFinished = true, lastUpdateMillis = lastUpdateMillis)
}
