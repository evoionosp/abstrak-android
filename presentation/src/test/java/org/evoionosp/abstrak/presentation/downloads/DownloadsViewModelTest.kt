package org.evoionosp.abstrak.presentation.downloads
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.usecase.DeleteBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveDownloadedBooksUseCase
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.message.SnackbarMessage
import org.evoionosp.abstrak.presentation.ui.message.UiText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val dune = downloaded("dune", libraryId = "lib-1", bytes = 300)
    private val hailMary = downloaded("hail-mary", libraryId = "lib-2", bytes = 200, status = DownloadStatus.DOWNLOADING)
    private val dracula = downloaded("dracula", libraryId = "lib-3", bytes = 100)
    private val books = MutableStateFlow(listOf(dune, hailMary, dracula))
    private val deleteBookDownload = mockk<DeleteBookDownloadUseCase>()
    private val snackbar = SnackbarController()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { deleteBookDownload(any()) } coAnswers {
            val id = firstArg<String>()
            books.update { list -> list.filterNot { it.audiobook.id == id } }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists every downloaded book`() =
        runTest {
            val viewModel = subscribedViewModel()
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            assertEquals(listOf("dune", "hail-mary", "dracula"), state.books.ids())
            assertEquals(listOf("lib-1", "lib-2", "lib-3"), state.books.map { it.audiobook.libraryId })
            assertEquals(listOf(300L, 200L, 100L), state.books.map { it.sizeBytes })
            assertFalse(state.isSelectionMode)
        }

    @Test
    fun `nothing is listed before the first read and then the empty list shows`() =
        runTest {
            val source = MutableSharedFlow<List<DownloadedBook>>()
            val viewModel = subscribedViewModel(source)
            assertFalse(viewModel.uiState.value.isLoaded)
            source.emit(emptyList())
            assertTrue(viewModel.uiState.value.isLoaded)
            assertEquals(emptyList<DownloadedBookUiModel>(), viewModel.uiState.value.books)
        }

    @Test
    fun `rows show download progress and failures`() =
        runTest {
            books.value =
                listOf(
                    downloaded("done", bytes = 100, totalBytes = 100),
                    downloaded("running", bytes = 25, totalBytes = 100, status = DownloadStatus.DOWNLOADING),
                    downloaded("almost", bytes = 999, totalBytes = 1000, status = DownloadStatus.DOWNLOADING),
                    downloaded("queued", bytes = 0, totalBytes = null, status = DownloadStatus.QUEUED),
                    downloaded("failed", bytes = 40, totalBytes = 100, status = DownloadStatus.FAILED),
                )
            val viewModel = subscribedViewModel()
            val rows = viewModel.uiState.value.books
            assertEquals(
                listOf(
                    DownloadedBookStatus.DOWNLOADED,
                    DownloadedBookStatus.DOWNLOADING,
                    DownloadedBookStatus.DOWNLOADING,
                    DownloadedBookStatus.DOWNLOADING,
                    DownloadedBookStatus.FAILED,
                ),
                rows.map { it.status },
            )
            assertEquals(listOf(null, 25, 99, null, null), rows.map { it.progressPercent })
        }

    @Test
    fun `selecting the first book starts selection mode and deselecting the last ends it`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.toggleSelection("dune")
            assertTrue(viewModel.uiState.value.isSelectionMode)
            assertEquals(setOf("dune"), viewModel.uiState.value.selectedIds)
            viewModel.toggleSelection("dracula")
            assertEquals(setOf("dune", "dracula"), viewModel.uiState.value.selectedIds)
            viewModel.toggleSelection("dune")
            viewModel.toggleSelection("dracula")
            assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
            assertFalse(viewModel.uiState.value.isSelectionMode)
        }

    @Test
    fun `select all picks every book and clearing leaves selection mode`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.toggleSelection("dune")
            viewModel.selectAll()
            assertEquals(setOf("dune", "hail-mary", "dracula"), viewModel.uiState.value.selectedIds)
            viewModel.clearSelection()
            assertFalse(viewModel.uiState.value.isSelectionMode)
        }

    @Test
    fun `deleting from a row menu asks first and dismissing keeps the book`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.requestDelete("dune")
            assertEquals(PendingDownloadsDelete(titles = listOf("dune"), bytes = 300), viewModel.uiState.value.pendingDelete)
            viewModel.dismissDelete()
            assertNull(viewModel.uiState.value.pendingDelete)
            coVerify(exactly = 0) { deleteBookDownload(any()) }
            assertEquals(3, viewModel.uiState.value.books.size)
        }

    @Test
    fun `confirming a row delete removes the book and updates the total`() =
        runTest {
            val viewModel = subscribedViewModel()
            snackbar.messages.test {
                viewModel.requestDelete("dune")
                viewModel.confirmDelete()
                assertEquals(deleted(1), awaitItem())
            }
            coVerify(exactly = 1) { deleteBookDownload("dune") }
            val state = viewModel.uiState.value
            assertNull(state.pendingDelete)
            assertEquals(listOf("hail-mary", "dracula"), state.books.ids())
        }

    @Test
    fun `deleting the selection removes every selected book, running ones included, and ends selection mode`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.toggleSelection("dune")
            viewModel.toggleSelection("hail-mary")
            viewModel.requestDeleteSelected()
            assertEquals(
                PendingDownloadsDelete(titles = listOf("dune", "hail-mary"), bytes = 500),
                viewModel.uiState.value.pendingDelete,
            )
            snackbar.messages.test {
                viewModel.confirmDelete()
                assertEquals(deleted(2), awaitItem())
            }
            coVerify(exactly = 1) { deleteBookDownload("dune") }
            coVerify(exactly = 1) { deleteBookDownload("hail-mary") }
            val state = viewModel.uiState.value
            assertEquals(listOf("dracula"), state.books.ids())
            assertFalse(state.isSelectionMode)
            assertNull(state.pendingDelete)
        }

    @Test
    fun `a delete that fails for one book deletes the others and reports both outcomes`() =
        runTest {
            coEvery { deleteBookDownload("hail-mary") } throws RuntimeException("database or disk is full")
            val viewModel = subscribedViewModel()
            viewModel.selectAll()
            viewModel.requestDeleteSelected()
            snackbar.messages.test {
                viewModel.confirmDelete()
                assertEquals(deleted(2), awaitItem())
                assertEquals(
                    SnackbarMessage(UiText.Plural(R.plurals.downloads_delete_failed, 1), SnackbarType.ERROR),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) { deleteBookDownload("dune") }
            coVerify(exactly = 1) { deleteBookDownload("hail-mary") }
            coVerify(exactly = 1) { deleteBookDownload("dracula") }
            val state = viewModel.uiState.value
            assertEquals(listOf("hail-mary"), state.books.ids())
            assertFalse(state.isSelectionMode)
            assertNull(state.pendingDelete)
        }

    @Test
    fun `a delete that finishes with no collector still delivers its message when collected`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.selectAll()
            viewModel.requestDeleteSelected()
            viewModel.confirmDelete()
            snackbar.messages.test {
                assertEquals(deleted(3), awaitItem())
            }
        }

    @Test
    fun `the selection leaves the list before the files are gone`() =
        runTest {
            coEvery { deleteBookDownload(any()) } coAnswers { awaitCancellation() }
            val viewModel = subscribedViewModel()
            viewModel.selectAll()
            viewModel.requestDeleteSelected()
            viewModel.confirmDelete()
            assertNull(viewModel.uiState.value.pendingDelete)
            assertFalse(viewModel.uiState.value.isSelectionMode)
        }

    @Test
    fun `confirming starts every selected delete at once`() =
        runTest {
            val started = mutableListOf<String>()
            coEvery { deleteBookDownload(any()) } coAnswers {
                started += firstArg<String>()
                awaitCancellation()
            }
            val viewModel = subscribedViewModel()
            viewModel.selectAll()
            viewModel.requestDeleteSelected()
            viewModel.confirmDelete()
            assertEquals(setOf("dune", "hail-mary", "dracula"), started.toSet())
        }

    @Test
    fun `books that go away drop out of the selection and the open dialog`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.toggleSelection("dune")
            viewModel.toggleSelection("dracula")
            viewModel.requestDeleteSelected()
            books.update { list -> list.filterNot { it.audiobook.id == "dune" } }
            assertEquals(setOf("dracula"), viewModel.uiState.value.selectedIds)
            assertEquals(PendingDownloadsDelete(titles = listOf("dracula"), bytes = 100), viewModel.uiState.value.pendingDelete)
            books.update { list -> list.filterNot { it.audiobook.id == "dracula" } }
            assertFalse(viewModel.uiState.value.isSelectionMode)
            assertNull(viewModel.uiState.value.pendingDelete)
        }

    @Test
    fun `a book cancelled elsewhere comes back unselected when downloaded again`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.toggleSelection("dune")
            viewModel.toggleSelection("dracula")
            viewModel.requestDeleteSelected()
            books.update { list -> list.filterNot { it.audiobook.id == "dune" } }
            books.value = listOf(dune, hailMary, dracula)
            assertEquals(setOf("dracula"), viewModel.uiState.value.selectedIds)
            assertEquals(PendingDownloadsDelete(titles = listOf("dracula"), bytes = 100), viewModel.uiState.value.pendingDelete)
        }

    @Test
    fun `a book cancelled while away from the screen comes back unselected when downloaded again`() =
        runTest {
            val viewModel = viewModel()
            val screen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            viewModel.toggleSelection("dune")
            viewModel.requestDelete("dracula")
            screen.cancel()
            advanceTimeBy(10_000)
            books.update { list -> list.filterNot { it.audiobook.id == "dune" || it.audiobook.id == "dracula" } }
            books.value = listOf(dune, hailMary, dracula)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertFalse(viewModel.uiState.value.isSelectionMode)
            assertNull(viewModel.uiState.value.pendingDelete)
        }

    @Test
    fun `the list mapping runs on the injected default dispatcher`() =
        runTest {
            val viewModel = subscribedViewModel(defaultDispatcher = StandardTestDispatcher(testScheduler))
            assertFalse(viewModel.uiState.value.isLoaded)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            assertEquals(listOf("dune", "hail-mary", "dracula"), state.books.ids())
        }

    private fun TestScope.subscribedViewModel(
        source: Flow<List<DownloadedBook>> = books,
        defaultDispatcher: CoroutineDispatcher = dispatcher,
    ): DownloadsViewModel {
        val viewModel = viewModel(source, defaultDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun viewModel(
        source: Flow<List<DownloadedBook>> = books,
        defaultDispatcher: CoroutineDispatcher = dispatcher,
    ): DownloadsViewModel {
        val observe = mockk<ObserveDownloadedBooksUseCase>()
        every { observe() } returns source
        return DownloadsViewModel(observe, deleteBookDownload, defaultDispatcher, snackbar)
    }

    private fun deleted(count: Int) = SnackbarMessage(UiText.Plural(R.plurals.downloads_deleted, count), SnackbarType.SUCCESS)

    private fun List<DownloadedBookUiModel>.ids(): List<String> = map { it.audiobook.id }

    private fun downloaded(
        id: String,
        libraryId: String = "lib-1",
        bytes: Long,
        totalBytes: Long? = bytes,
        status: DownloadStatus = DownloadStatus.COMPLETED,
    ) = DownloadedBook(
        audiobook =
            Audiobook(
                id = id,
                libraryId = libraryId,
                title = id,
                author = "Author",
                coverUrl = "",
                series = null,
                durationInSeconds = 3_600L,
            ),
        summary =
            BookDownloadSummary(
                audiobookId = id,
                totalTracks = 1,
                completedTracks = if (status == DownloadStatus.COMPLETED) 1 else 0,
                activeTracks = if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.QUEUED) 1 else 0,
                failedTracks = if (status == DownloadStatus.FAILED) 1 else 0,
                bytesDownloaded = bytes,
                totalBytes = totalBytes,
                status = status,
            ),
    )
}
