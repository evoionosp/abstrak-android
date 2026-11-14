package org.evoionosp.abstrak.presentation.settings
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.session.CoverArtCache
import org.evoionosp.abstrak.domain.session.DownloadStore
import org.evoionosp.abstrak.domain.session.LocalCatalogCleaner
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.PlayerLogoutHandler
import org.evoionosp.abstrak.domain.session.SessionStore
import org.evoionosp.abstrak.domain.session.usecase.LogoutUserUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout runs the flow exposes progress and completes`() =
        runTest {
            val sessionStore = FakeSessionStore(signedInSession())
            val viewModel = PreferencesViewModel(logoutUseCase(sessionStore), FakeAppSettingsStore())
            var completed = false
            viewModel.isLoggingOut.test {
                assertEquals(false, awaitItem())
                viewModel.logout { completed = true }
                runCurrent()
                assertEquals(true, awaitItem())
                advanceUntilIdle()
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(completed)
            assertNull(sessionStore.session.first())
        }

    @Test
    fun `second logout while running is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            var invocations = 0
            val useCase =
                logoutUseCase(
                    FakeSessionStore(signedInSession()),
                    playerLogoutHandler =
                        PlayerLogoutHandler {
                            invocations++
                            gate.await()
                        },
                )
            val viewModel = PreferencesViewModel(useCase, FakeAppSettingsStore())
            viewModel.logout { }
            runCurrent()
            viewModel.logout { }
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, invocations)
        }

    @Test
    fun `search preference shows the stored value and toggling saves it`() =
        runTest {
            val settings = FakeAppSettingsStore(AppSettings(searchAllLibraries = false))
            val viewModel = PreferencesViewModel(logoutUseCase(FakeSessionStore(signedInSession())), settings)
            viewModel.searchAllLibraries.test {
                assertNull(awaitItem())
                assertEquals(false, awaitItem())
                viewModel.onSearchAllLibrariesChange(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(settings.settings.first().searchAllLibraries)
        }

    private fun logoutUseCase(
        sessionStore: SessionStore,
        playerLogoutHandler: PlayerLogoutHandler = PlayerLogoutHandler { },
    ) = LogoutUserUseCase(
        playerLogoutHandler = playerLogoutHandler,
        sessionStore = sessionStore,
        downloadStore = DownloadStore { },
        coverArtCache = CoverArtCache { },
        localCatalogCleaner = LocalCatalogCleaner { },
    )

    private fun signedInSession() =
        LoginSession(
            accessToken = "access",
            refreshToken = "refresh",
            userId = "user-1",
            username = "abstrak",
            baseUrl = "https://example.com",
            accessTokenExpiresAtEpochSeconds = null,
        )
}
