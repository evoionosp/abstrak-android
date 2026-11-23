package org.evoionosp.abstrak.domain.library.usecase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.sync.LibrarySyncPolicy
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncLibraryBooksUseCaseTest {
    private val audiobooks = FakeAudiobookRepository()
    private val useCase = SyncLibraryBooksUseCase(audiobooks) { NOW }

    @Test
    fun `syncs a library that never synced`() =
        runTest {
            assertEquals(DomainResult.Success(Unit), sync(force = false))
            assertEquals(listOf("lib-1"), audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `skips a library that synced within the day`() =
        runTest {
            audiobooks.syncStatus = SyncStatus.Success(lastSyncedAtMillis = NOW - LibrarySyncPolicy.INTERVAL_MILLIS + 1)
            assertNull(sync(force = false))
            assertFalse(useCase.isDue("lib-1"))
            assertEquals(emptyList<String>(), audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `due follows the last sync even while one shows as running or failed`() =
        runTest {
            audiobooks.syncStatus = SyncStatus.Syncing(lastSyncedAtMillis = NOW - LibrarySyncPolicy.INTERVAL_MILLIS)
            assertTrue(useCase.isDue("lib-1"))
            audiobooks.syncStatus = SyncStatus.Failed(CatalogError.NETWORK, lastSyncedAtMillis = NOW)
            assertFalse(useCase.isDue("lib-1"))
        }

    @Test
    fun `a forced sync runs however recent the last one`() =
        runTest {
            audiobooks.syncStatus = SyncStatus.Success(lastSyncedAtMillis = NOW)
            assertEquals(DomainResult.Success(Unit), sync(force = true))
            assertEquals(listOf("lib-1"), audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `passes the repository's failure on`() =
        runTest {
            audiobooks.refreshBooksResult = DomainResult.Failure(CatalogError.NETWORK)
            assertEquals(DomainResult.Failure(CatalogError.NETWORK), sync(force = false))
        }

    @Test
    fun `a trigger arriving during a sync waits for it and then finds the library synced`() =
        runTest {
            val release = CompletableDeferred<Unit>()
            audiobooks.onRefreshBooks = { libraryId ->
                release.await()
                audiobooks.syncStatusByLibrary[libraryId] = SyncStatus.Success(lastSyncedAtMillis = NOW)
            }
            val first = async { sync(force = false) }
            runCurrent()
            val second = async { sync(force = false) }
            runCurrent()
            release.complete(Unit)
            assertEquals(DomainResult.Success(Unit), first.await())
            assertNull(second.await())
            assertEquals(listOf("lib-1"), audiobooks.refreshedBookLibraries)
        }

    private suspend fun sync(force: Boolean): DomainResult<Unit>? =
        useCase(baseUrl = "https://example.com", accessToken = "token", libraryId = "lib-1", force = force)

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
