package org.evoionosp.abstrak.domain.catalog.usecase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.model.LoginData
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.SyncLibraryBooksUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeAuthRepository
import org.evoionosp.abstrak.domain.testing.FakeConnectivityObserver
import org.evoionosp.abstrak.domain.testing.FakeLibraryRepository
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.evoionosp.abstrak.domain.testing.testLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveAndSyncCatalogUseCaseTest {
    private val session =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    @Test
    fun `does nothing while offline`() =
        runTest {
            val graph = graph()
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            advanceUntilIdle()
            assertEquals(0, graph.libraries.refreshCallCount)
            assertEquals(emptyList<String>(), graph.audiobooks.refreshedBookLibraries)
            assertEquals(emptyList<String>(), graph.audiobooks.refreshedContinueLibraries)
        }

    @Test
    fun `starting online syncs a due library once`() =
        runTest {
            val graph = graph(initiallyConnected = true)
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            advanceUntilIdle()
            assertEquals(1, graph.libraries.refreshCallCount)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedContinueLibraries)
        }

    @Test
    fun `syncs a due library when connectivity is restored`() =
        runTest {
            val graph = graph()
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            advanceUntilIdle()
            graph.connectivity.setConnected(true)
            advanceUntilIdle()
            assertEquals(1, graph.libraries.refreshCallCount)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedContinueLibraries)
        }

    @Test
    fun `coming online leaves a library synced within the day alone but refreshes continue listening`() =
        runTest {
            val graph = graph(syncStatus = SyncStatus.Success(lastSyncedAtMillis = NOW - 60_000))
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            graph.connectivity.setConnected(true)
            advanceUntilIdle()
            assertEquals(0, graph.libraries.refreshCallCount)
            assertEquals(emptyList<String>(), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedContinueLibraries)
        }

    @Test
    fun `switching library does not sync from here`() =
        runTest {
            val graph = graph(syncStatus = SyncStatus.Success(lastSyncedAtMillis = NOW))
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            graph.connectivity.setConnected(true)
            advanceUntilIdle()
            graph.libraries.selectLibrary("lib-2")
            advanceUntilIdle()
            assertEquals(emptyList<String>(), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedContinueLibraries)
        }

    private fun TestScope.collectImmediately() = UnconfinedTestDispatcher(testScheduler)

    private fun graph(
        initiallyConnected: Boolean = false,
        syncStatus: SyncStatus = SyncStatus.Idle,
    ): Graph {
        val store = FakeSessionStore(session)
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        val connectivity = FakeConnectivityObserver(initiallyConnected = initiallyConnected)
        val libraryRepository =
            FakeLibraryRepository(
                libraries =
                    listOf(
                        testLibrary(isSelected = true),
                        testLibrary(id = "lib-2", name = "Second"),
                    ),
                selectedLibrary = testLibrary(isSelected = true),
            )
        val audiobookRepository = FakeAudiobookRepository().also { it.syncStatus = syncStatus }
        val refreshHomeCatalog =
            RefreshHomeCatalogUseCase(
                getValidSessionUseCase = GetValidSessionUseCase(store, SessionRefreshCoordinator(store, auth)),
                refreshLibrariesUseCase = RefreshLibrariesUseCase(libraryRepository),
                observeSelectedLibraryUseCase = ObserveSelectedLibraryUseCase(libraryRepository),
                observeLibrariesUseCase = ObserveLibrariesUseCase(libraryRepository),
                syncLibraryBooksUseCase = SyncLibraryBooksUseCase(audiobookRepository) { NOW },
                refreshContinueListeningUseCase = RefreshContinueListeningUseCase(audiobookRepository),
            )
        val useCase =
            ObserveAndSyncCatalogUseCase(
                connectivityObserver = connectivity,
                refreshHomeCatalogUseCase = refreshHomeCatalog,
            )
        return Graph(useCase, connectivity, libraryRepository, audiobookRepository)
    }

    private data class Graph(
        val useCase: ObserveAndSyncCatalogUseCase,
        val connectivity: FakeConnectivityObserver,
        val libraries: FakeLibraryRepository,
        val audiobooks: FakeAudiobookRepository,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
