package org.evoionosp.abstrak.domain.catalog.usecase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.model.LoginData
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.RefreshLibrariesUseCase
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
        }

    @Test
    fun `syncs the current selection when connectivity is restored`() =
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
    fun `refreshes books and continue listening when the library changes online`() =
        runTest {
            val graph = graph()
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            graph.connectivity.setConnected(true)
            advanceUntilIdle()
            graph.libraries.selectLibrary("lib-2")
            advanceUntilIdle()
            assertEquals(listOf("lib-1", "lib-2"), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1", "lib-2"), graph.audiobooks.refreshedContinueLibraries)
            assertEquals(1, graph.libraries.refreshCallCount)
        }

    @Test
    fun `ignores library changes while offline`() =
        runTest {
            val graph = graph()
            backgroundScope.launch(collectImmediately()) { graph.useCase().collect {} }
            advanceUntilIdle()
            graph.libraries.selectLibrary("lib-2")
            advanceUntilIdle()
            assertEquals(emptyList<String>(), graph.audiobooks.refreshedBookLibraries)
            assertEquals(0, graph.libraries.refreshCallCount)
        }

    private fun TestScope.collectImmediately() = UnconfinedTestDispatcher(testScheduler)

    private fun graph(): Graph {
        val store = FakeSessionStore(session)
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        val connectivity = FakeConnectivityObserver(initiallyConnected = false)
        val libraryRepository =
            FakeLibraryRepository(
                libraries =
                    listOf(
                        testLibrary(isSelected = true),
                        testLibrary(id = "lib-2", name = "Second"),
                    ),
                selectedLibrary = testLibrary(isSelected = true),
            )
        val audiobookRepository = FakeAudiobookRepository()
        val useCase =
            ObserveAndSyncCatalogUseCase(
                connectivityObserver = connectivity,
                observeSelectedLibraryUseCase = ObserveSelectedLibraryUseCase(libraryRepository),
                getValidSessionUseCase = GetValidSessionUseCase(store, SessionRefreshCoordinator(store, auth)),
                refreshLibrariesUseCase = RefreshLibrariesUseCase(libraryRepository),
                refreshSelectedLibraryAudiobooksUseCase = RefreshSelectedLibraryAudiobooksUseCase(audiobookRepository),
                refreshContinueListeningUseCase = RefreshContinueListeningUseCase(audiobookRepository),
            )
        return Graph(useCase, connectivity, libraryRepository, audiobookRepository)
    }

    private data class Graph(
        val useCase: ObserveAndSyncCatalogUseCase,
        val connectivity: FakeConnectivityObserver,
        val libraries: FakeLibraryRepository,
        val audiobooks: FakeAudiobookRepository,
    )
}
