package org.evoionosp.abstrak.presentation.settings
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.usecase.DeleteAllDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.GetStorageUsageUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveDownloadedBooksUseCase
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
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
class StorageDownloadsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val books =
        MutableStateFlow(
            listOf(
                downloaded("dune", bytes = 300),
                downloaded("hail-mary", bytes = 200),
            ),
        )
    private val storage =
        MutableStateFlow(StorageUsage(downloadsBytes = 500, cacheBytes = 40, deviceFreeBytes = 10_000))
    private val deleteAllDownloads = mockk<DeleteAllDownloadsUseCase>()
    private val snackbar = SnackbarController()
    private val clearAllFailed = SnackbarMessage(UiText.Resource(R.string.storage_clear_all_failed), SnackbarType.ERROR)
    private val settings = FakeAppSettingsStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { deleteAllDownloads() } coAnswers {
            books.update { emptyList() }
            storage.update { it.copy(downloadsBytes = 0) }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLoaded is false until the first combined state, then true`() =
        runTest {
            val observe = mockk<ObserveDownloadedBooksUseCase>()
            every { observe() } returns books
            val getStorageUsage = mockk<GetStorageUsageUseCase>()
            every { getStorageUsage() } returns storage
            val viewModel = StorageDownloadsViewModel(settings, deleteAllDownloads, observe, getStorageUsage, snackbar)
            assertFalse(viewModel.uiState.value.isLoaded)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertTrue(viewModel.uiState.value.isLoaded)
        }

    @Test
    fun `exposes the toggles, the downloaded books and the storage usage`() =
        runTest {
            val viewModel = subscribedViewModel()
            val state = viewModel.uiState.value
            assertFalse(state.downloadWifiOnly)
            assertFalse(state.autoDeleteFinishedDownloads)
            assertEquals(listOf("dune", "hail-mary"), state.downloadedBookTitles)
            assertEquals(StorageUsage(downloadsBytes = 500, cacheBytes = 40, deviceFreeBytes = 10_000), state.storage)
            assertFalse(state.showClearAllDialog)
        }

    @Test
    fun `toggling wifi-only and auto-delete saves the new values`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.onDownloadWifiOnlyChange(true)
            viewModel.onAutoDeleteFinishedDownloadsChange(true)
            assertTrue(viewModel.uiState.value.downloadWifiOnly)
            assertTrue(viewModel.uiState.value.autoDeleteFinishedDownloads)
        }

    @Test
    fun `clear-all dialog shows and dismisses without deleting`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.requestClearAll()
            assertTrue(viewModel.uiState.value.showClearAllDialog)
            viewModel.dismissClearAll()
            assertFalse(viewModel.uiState.value.showClearAllDialog)
            coVerify(exactly = 0) { deleteAllDownloads() }
        }

    @Test
    fun `clear-all dialog never shows when nothing is downloaded`() =
        runTest {
            books.value = emptyList()
            val viewModel = subscribedViewModel()
            viewModel.requestClearAll()
            assertFalse(viewModel.uiState.value.showClearAllDialog)
        }

    @Test
    fun `confirming clear-all deletes everything, closes the dialog and confirms it`() =
        runTest {
            val viewModel = subscribedViewModel()
            viewModel.requestClearAll()
            viewModel.confirmClearAll()
            assertEquals(
                SnackbarMessage(UiText.Resource(R.string.storage_clear_all_done), SnackbarType.SUCCESS),
                snackbar.messages.first(),
            )
            coVerify(exactly = 1) { deleteAllDownloads() }
            assertFalse(viewModel.uiState.value.showClearAllDialog)
            assertFalse(viewModel.uiState.value.isClearing)
            assertFalse(viewModel.uiState.value.hasDownloads)
            assertEquals(0L, viewModel.uiState.value.storage.downloadsBytes)
        }

    @Test
    fun `a clear-all that throws does not crash, resets, and surfaces the failure`() =
        runTest {
            coEvery { deleteAllDownloads() } throws RuntimeException("database or disk is full")
            val viewModel = subscribedViewModel()
            snackbar.messages.test {
                viewModel.requestClearAll()
                viewModel.confirmClearAll()
                assertEquals(clearAllFailed, awaitItem())
            }
            coVerify(exactly = 1) { deleteAllDownloads() }
            val state = viewModel.uiState.value
            assertFalse(state.isClearing)
            assertFalse(state.showClearAllDialog)
            assertTrue(state.hasDownloads)
            assertEquals(listOf("dune", "hail-mary"), state.downloadedBookTitles)
        }

    @Test
    fun `a clear-all failure with no collector still delivers its message when collected`() =
        runTest {
            coEvery { deleteAllDownloads() } throws RuntimeException("database or disk is full")
            val viewModel = subscribedViewModel()
            viewModel.requestClearAll()
            viewModel.confirmClearAll()
            snackbar.messages.test {
                assertEquals(clearAllFailed, awaitItem())
            }
        }

    @Test
    fun `state follows later settings and storage changes`() =
        runTest {
            val viewModel = subscribedViewModel()
            val titles = listOf("dune", "hail-mary")
            val initialStorage = StorageUsage(downloadsBytes = 500, cacheBytes = 40, deviceFreeBytes = 10_000)
            viewModel.uiState.test {
                assertEquals(
                    StorageDownloadsUiState(
                        isLoaded = true,
                        downloadedBookTitles = titles,
                        storage = initialStorage,
                    ),
                    awaitItem(),
                )
                settings.setDownloadWifiOnly(true)
                assertEquals(
                    StorageDownloadsUiState(
                        isLoaded = true,
                        downloadWifiOnly = true,
                        downloadedBookTitles = titles,
                        storage = initialStorage,
                    ),
                    awaitItem(),
                )
                val laterStorage = StorageUsage(downloadsBytes = 100, cacheBytes = 0, deviceFreeBytes = 0)
                storage.value = laterStorage
                assertEquals(
                    StorageDownloadsUiState(
                        isLoaded = true,
                        downloadWifiOnly = true,
                        downloadedBookTitles = titles,
                        storage = laterStorage,
                    ),
                    awaitItem(),
                )
            }
        }

    private fun TestScope.subscribedViewModel(): StorageDownloadsViewModel {
        val observe = mockk<ObserveDownloadedBooksUseCase>()
        every { observe() } returns books
        val getStorageUsage = mockk<GetStorageUsageUseCase>()
        every { getStorageUsage() } returns storage
        val viewModel = StorageDownloadsViewModel(settings, deleteAllDownloads, observe, getStorageUsage, snackbar)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun downloaded(
        id: String,
        bytes: Long,
    ) = DownloadedBook(
        audiobook =
            Audiobook(
                id = id,
                libraryId = "lib-1",
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
                completedTracks = 1,
                activeTracks = 0,
                failedTracks = 0,
                bytesDownloaded = bytes,
                totalBytes = bytes,
                status = DownloadStatus.COMPLETED,
            ),
    )
}
