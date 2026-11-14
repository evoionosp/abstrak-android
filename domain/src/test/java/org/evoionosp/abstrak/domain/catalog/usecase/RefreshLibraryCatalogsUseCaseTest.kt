package org.evoionosp.abstrak.domain.catalog.usecase
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.model.LoginData
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeAuthRepository
import org.evoionosp.abstrak.domain.testing.FakeConnectivityObserver
import org.evoionosp.abstrak.domain.testing.FakeLibraryRepository
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.evoionosp.abstrak.domain.testing.testLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshLibraryCatalogsUseCaseTest {
    private val session =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )
    private val libraries =
        listOf(
            testLibrary(id = "lib-1"),
            testLibrary(id = "lib-2", isSelected = true),
            testLibrary(id = "lib-3"),
        )
    private val sessionStore = FakeSessionStore(session)
    private val connectivity = FakeConnectivityObserver(initiallyConnected = true)
    private val audiobooks = FakeAudiobookRepository()

    @Test
    fun `refreshes the selected library first, then the others one at a time`() =
        runTest {
            val gates = libraries.associate { it.id to CompletableDeferred<Unit>() }
            audiobooks.onRefreshBooks = { libraryId -> gates.getValue(libraryId).await() }
            useCase()().test {
                assertEquals(CatalogRefreshStage.SELECTED_LIBRARY, awaitItem())
                runCurrent()
                assertEquals(listOf("lib-2"), audiobooks.refreshedBookLibraries)
                gates.getValue("lib-2").complete(Unit)
                assertEquals(CatalogRefreshStage.OTHER_LIBRARIES, awaitItem())
                runCurrent()
                assertEquals(listOf("lib-2", "lib-1"), audiobooks.refreshedBookLibraries)
                gates.getValue("lib-1").complete(Unit)
                runCurrent()
                assertEquals(listOf("lib-2", "lib-1", "lib-3"), audiobooks.refreshedBookLibraries)
                gates.getValue("lib-3").complete(Unit)
                awaitComplete()
            }
        }

    @Test
    fun `a library that fails or throws does not stop the others`() =
        runTest {
            audiobooks.refreshBooksResult = DomainResult.Failure(CatalogError.NETWORK)
            audiobooks.onRefreshBooks = { libraryId ->
                if (libraryId == "lib-1") throw IllegalStateException("disk full")
            }
            val stages = useCase()().toList()
            assertEquals(listOf(CatalogRefreshStage.SELECTED_LIBRARY, CatalogRefreshStage.OTHER_LIBRARIES), stages)
            assertEquals(listOf("lib-2", "lib-1", "lib-3"), audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `logging out stops the pass and cancels the refresh in flight`() =
        runTest {
            val cancelled = mutableListOf<String>()
            audiobooks.onRefreshBooks = { libraryId ->
                try {
                    awaitCancellation()
                } finally {
                    cancelled += libraryId
                }
            }
            useCase()().test {
                assertEquals(CatalogRefreshStage.SELECTED_LIBRARY, awaitItem())
                runCurrent()
                sessionStore.clearSession()
                awaitComplete()
            }
            assertEquals(listOf("lib-2"), audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-2"), cancelled)
        }

    @Test
    fun `offline the pass waits for a connection`() =
        runTest {
            connectivity.setConnected(false)
            useCase()().test {
                runCurrent()
                assertEquals(emptyList<String>(), audiobooks.refreshedBookLibraries)
                connectivity.setConnected(true)
                assertEquals(CatalogRefreshStage.SELECTED_LIBRARY, awaitItem())
                assertEquals(CatalogRefreshStage.OTHER_LIBRARIES, awaitItem())
                awaitComplete()
            }
            assertEquals(listOf("lib-2", "lib-1", "lib-3"), audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `does nothing without a session`() =
        runTest {
            val stages = useCase(FakeSessionStore(null))().toList()
            assertEquals(emptyList<CatalogRefreshStage>(), stages)
            assertEquals(emptyList<String>(), audiobooks.refreshedBookLibraries)
        }

    private fun useCase(sessionStore: FakeSessionStore = this.sessionStore): RefreshLibraryCatalogsUseCase {
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        return RefreshLibraryCatalogsUseCase(
            connectivityObserver = connectivity,
            observeSessionUseCase = ObserveSessionUseCase(sessionStore),
            getValidSessionUseCase = GetValidSessionUseCase(sessionStore, SessionRefreshCoordinator(sessionStore, auth)),
            libraryRepository = FakeLibraryRepository(libraries = libraries),
            audiobookRepository = audiobooks,
        )
    }
}
