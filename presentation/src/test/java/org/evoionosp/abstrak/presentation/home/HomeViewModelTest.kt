package org.evoionosp.abstrak.presentation.home
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveHomeAudiobooksUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveLibrarySyncStatusUseCase
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshHomeCatalogResult
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshHomeCatalogUseCase
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.SelectLibraryUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.playback.PlaybackState
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.message.SnackbarMessage
import org.evoionosp.abstrak.presentation.ui.message.UiText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the selected library's books split into audiobooks and ebooks`() {
        val viewModel = homeViewModel(books = listOf(book("dune"), book("manual", hasAudio = false), book("emma")))
        val state = viewModel.uiState.value
        assertEquals(listOf("dune", "emma"), state.audiobooks.map { it.id })
        assertEquals(listOf("manual"), state.ebooks.map { it.id })
    }

    @Test
    fun `home shelves and the library stats still cover every book`() {
        val viewModel = homeViewModel(books = listOf(book("dune"), book("manual", hasAudio = false)))
        val state = viewModel.uiState.value
        assertEquals(listOf("dune", "manual"), state.recentlyAdded.map { it.id })
        assertEquals(2, state.authorCount)
    }

    @Test
    fun `a refresh while offline warns that it shows what's saved`() =
        runTest {
            val snackbar = SnackbarController()
            val viewModel =
                homeViewModel(books = emptyList(), snackbar = snackbar, refresh = failure(CatalogError.CONNECTIVITY_UNAVAILABLE))
            viewModel.refresh()
            assertEquals(
                SnackbarMessage(UiText.Resource(R.string.catalog_offline), SnackbarType.WARNING),
                snackbar.messages.first(),
            )
        }

    @Test
    fun `a refresh that fails shows an error`() =
        runTest {
            val snackbar = SnackbarController()
            val viewModel = homeViewModel(books = emptyList(), snackbar = snackbar, refresh = failure(CatalogError.NETWORK))
            viewModel.refresh()
            assertEquals(
                SnackbarMessage(UiText.Resource(R.string.error_network), SnackbarType.ERROR),
                snackbar.messages.first(),
            )
        }

    @Test
    fun `pull to refresh forces a library sync`() =
        runTest {
            val refresh = succeeding()
            homeViewModel(books = emptyList(), refresh = refresh).refresh()
            coVerify(exactly = 1) { refresh(force = true) }
        }

    @Test
    fun `coming back to the app syncs the library only when it is due`() =
        runTest {
            val refresh = succeeding()
            homeViewModel(books = emptyList(), refresh = refresh).refreshSilently()
            coVerify(exactly = 1) { refresh(force = false) }
        }

    @Test
    fun `switching library syncs it only when it is due`() =
        runTest {
            val refresh = succeeding()
            val selectLibrary = mockk<SelectLibraryUseCase>()
            coEvery { selectLibrary("lib-2") } returns DomainResult.Success(Unit)
            val viewModel = homeViewModel(books = emptyList(), refresh = refresh, selectLibrary = selectLibrary)
            viewModel.onLibrarySelected("lib-2")
            coVerify(exactly = 1) { refresh(force = false) }
        }

    @Test
    fun `pull to refresh in offline mode explains why it cannot sync instead of trying`() =
        runTest {
            val snackbar = SnackbarController()
            val refresh = succeeding()
            val viewModel =
                homeViewModel(
                    books = emptyList(),
                    snackbar = snackbar,
                    refresh = refresh,
                    settings = FakeAppSettingsStore(AppSettings(useOfflineMode = true)),
                )
            viewModel.refresh()
            assertEquals(
                SnackbarMessage(UiText.Resource(R.string.library_sync_offline_mode_on), SnackbarType.WARNING),
                snackbar.messages.first(),
            )
            assertFalse(viewModel.uiState.value.isRefreshing)
            coVerify(exactly = 0) { refresh(any()) }
        }

    @Test
    fun `the offline mode switch is saved and shows in the state`() {
        val settings = FakeAppSettingsStore()
        val viewModel = homeViewModel(books = emptyList(), settings = settings)
        assertFalse(viewModel.uiState.value.isOfflineMode)
        viewModel.setOfflineMode(true)
        assertTrue(settings.state.value.useOfflineMode)
        assertTrue(viewModel.uiState.value.isOfflineMode)
    }

    private fun failure(error: CatalogError): RefreshHomeCatalogUseCase {
        val refresh = mockk<RefreshHomeCatalogUseCase>()
        coEvery { refresh(any()) } returns RefreshHomeCatalogResult.Failure(error)
        return refresh
    }

    private fun succeeding(): RefreshHomeCatalogUseCase {
        val refresh = mockk<RefreshHomeCatalogUseCase>()
        coEvery { refresh(any()) } returns RefreshHomeCatalogResult.Success
        return refresh
    }

    private fun homeViewModel(
        books: List<Audiobook>,
        snackbar: SnackbarController = SnackbarController(),
        refresh: RefreshHomeCatalogUseCase = mockk(),
        selectLibrary: SelectLibraryUseCase = mockk(),
        settings: FakeAppSettingsStore = FakeAppSettingsStore(),
    ): HomeViewModel {
        val library = AudiobookLibrary(id = "lib-1", name = "Shelf", isSelected = true)
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(null)
        val observeLibraries = mockk<ObserveLibrariesUseCase>()
        every { observeLibraries() } returns flowOf(listOf(library))
        val observeSelectedLibrary = mockk<ObserveSelectedLibraryUseCase>()
        every { observeSelectedLibrary() } returns flowOf(library)
        val observeHomeAudiobooks = mockk<ObserveHomeAudiobooksUseCase>()
        every { observeHomeAudiobooks("lib-1") } returns flowOf(books)
        val observeContinueListening = mockk<ObserveContinueListeningUseCase>()
        every { observeContinueListening("lib-1") } returns flowOf(emptyList())
        val observeSyncStatus = mockk<ObserveLibrarySyncStatusUseCase>()
        every { observeSyncStatus("lib-1") } returns flowOf(SyncStatus.Idle)
        val playbackConnection = mockk<PlaybackConnection>(relaxUnitFun = true)
        every { playbackConnection.playbackState } returns MutableStateFlow(PlaybackState())
        return HomeViewModel(
            observeSessionUseCase = observeSession,
            logoutUserUseCase = mockk(relaxed = true),
            observeLibrariesUseCase = observeLibraries,
            observeSelectedLibraryUseCase = observeSelectedLibrary,
            observeHomeAudiobooksUseCase = observeHomeAudiobooks,
            observeContinueListeningUseCase = observeContinueListening,
            observeLibrarySyncStatusUseCase = observeSyncStatus,
            refreshHomeCatalogUseCase = refresh,
            selectLibraryUseCase = selectLibrary,
            appSettingsStore = settings,
            playbackConnection = playbackConnection,
            snackbar = snackbar,
        )
    }

    private fun book(
        id: String,
        hasAudio: Boolean = true,
    ) = Audiobook(
        id = id,
        libraryId = "lib-1",
        title = id,
        author = "Author $id",
        coverUrl = "",
        series = null,
        durationInSeconds = if (hasAudio) 3_600L else 0L,
        hasAudio = hasAudio,
    )
}
