package org.evoionosp.abstrak.presentation.search
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.catalog.usecase.CatalogRefreshStage
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshLibraryCatalogsUseCase
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.search.model.AudiobookSearchResult
import org.evoionosp.abstrak.domain.search.usecase.SearchAudiobooksUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val dune =
        Audiobook(
            id = "dune",
            libraryId = "lib-2",
            title = "Dune",
            author = "Frank Herbert",
            coverUrl = "",
            series = null,
            durationInSeconds = 3_600L,
        )
    private val queries = mutableListOf<String>()
    private val searchAudiobooks = mockk<SearchAudiobooksUseCase>()
    private val refreshLibraryCatalogs = mockk<RefreshLibraryCatalogsUseCase>()
    private val selectedLibrary = MutableStateFlow<AudiobookLibrary?>(AudiobookLibrary("lib-1", "Audiobooks", true))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { searchAudiobooks(any()) } answers {
            firstArg<Flow<String>>().map { query ->
                queries += query
                if (query.isBlank()) null else listOf(AudiobookSearchResult(dune, libraryName = "All"))
            }
        }
        every { refreshLibraryCatalogs() } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nothing typed shows no results`() =
        runTest {
            val viewModel = subscribedViewModel()
            assertNull(viewModel.uiState.value.results)
        }

    @Test
    fun `typing searches and shows each match with its library`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.onQueryChange("dune")
            assertEquals("dune", queries.last())
            val results =
                viewModel.uiState.value.results
                    .orEmpty()
            assertEquals(listOf("Dune"), results.map { it.audiobook.title })
            assertEquals(listOf("All"), results.map { it.libraryName })
            viewModel.onQueryChange("")
            assertNull(viewModel.uiState.value.results)
        }

    @Test
    fun `opening search refreshes the catalogs, showing progress until the selected library is done`() =
        runTest {
            val selectedDone = CompletableDeferred<Unit>()
            val othersDone = CompletableDeferred<Unit>()
            every { refreshLibraryCatalogs() } returns
                flow {
                    emit(CatalogRefreshStage.SELECTED_LIBRARY)
                    selectedDone.await()
                    emit(CatalogRefreshStage.OTHER_LIBRARIES)
                    othersDone.await()
                }
            val viewModel = subscribedViewModel()
            assertTrue(viewModel.uiState.value.isRefreshing)
            selectedDone.complete(Unit)
            assertFalse(viewModel.uiState.value.isRefreshing)
            othersDone.complete(Unit)
            assertFalse(viewModel.uiState.value.isRefreshing)
            verify(exactly = 1) { refreshLibraryCatalogs() }
        }

    @Test
    fun `with the preference off nothing is fetched until it is turned on`() =
        runTest {
            val settings = FakeAppSettingsStore(AppSettings(searchAllLibraries = false))
            val viewModel = subscribedViewModel(settings)
            verify(exactly = 0) { refreshLibraryCatalogs() }
            assertEquals(false, viewModel.uiState.value.searchAllLibraries)
            settings.setSearchAllLibraries(true)
            verify(exactly = 1) { refreshLibraryCatalogs() }
            assertEquals(true, viewModel.uiState.value.searchAllLibraries)
        }

    @Test
    fun `turning the preference off stops the refresh and its indicator`() =
        runTest {
            val settings = FakeAppSettingsStore()
            var stopped = false
            every { refreshLibraryCatalogs() } returns
                flow {
                    emit(CatalogRefreshStage.SELECTED_LIBRARY)
                    try {
                        awaitCancellation()
                    } finally {
                        stopped = true
                    }
                }
            val viewModel = subscribedViewModel(settings)
            assertTrue(viewModel.uiState.value.isRefreshing)
            settings.setSearchAllLibraries(false)
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertTrue(stopped)
        }

    @Test
    fun `the state names the selected library for the narrower scope`() =
        runTest {
            val viewModel = subscribedViewModel(FakeAppSettingsStore(AppSettings(searchAllLibraries = false)))
            assertEquals("Audiobooks", viewModel.uiState.value.selectedLibraryName)
            selectedLibrary.value = AudiobookLibrary("lib-3", "OSHO", true)
            assertEquals("OSHO", viewModel.uiState.value.selectedLibraryName)
        }

    @Test
    fun `the results mapping runs on the injected default dispatcher`() =
        runTest {
            val viewModel =
                subscribedViewModel(
                    defaultDispatcher = StandardTestDispatcher(testScheduler),
                )
            viewModel.onQueryChange("dune")
            assertNull(viewModel.uiState.value.results)
            advanceUntilIdle()
            val results = viewModel.uiState.value.results
            assertEquals(listOf("dune"), results?.map { it.audiobook.id })
        }

    private fun TestScope.subscribedViewModel(
        settings: FakeAppSettingsStore = FakeAppSettingsStore(),
        defaultDispatcher: CoroutineDispatcher = dispatcher,
    ): SearchViewModel {
        val observeSelectedLibrary = mockk<ObserveSelectedLibraryUseCase>()
        every { observeSelectedLibrary() } returns selectedLibrary
        val viewModel =
            SearchViewModel(
                searchAudiobooks = searchAudiobooks,
                observeSelectedLibrary = observeSelectedLibrary,
                appSettingsStore = settings,
                refreshLibraryCatalogs = refreshLibraryCatalogs,
                defaultDispatcher = defaultDispatcher,
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }
}
