package org.evoionosp.abstrak.presentation.settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.usecase.DeleteAllDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.GetStorageUsageUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveDownloadedBooksUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController

data class StorageDownloadsUiState(
    val isLoaded: Boolean = false,
    val downloadWifiOnly: Boolean = false,
    val autoDeleteFinishedDownloads: Boolean = false,
    val downloadedBookTitles: List<String> = emptyList(),
    val storage: StorageUsage = StorageUsage(downloadsBytes = 0L, cacheBytes = 0L, deviceFreeBytes = 0L),
    val isClearing: Boolean = false,
    val showClearAllDialog: Boolean = false,
) {
    val hasDownloads: Boolean get() = downloadedBookTitles.isNotEmpty()
}

@HiltViewModel
class StorageDownloadsViewModel
    @Inject
    constructor(
        private val appSettingsStore: AppSettingsStore,
        private val deleteAllDownloads: DeleteAllDownloadsUseCase,
        observeDownloadedBooks: ObserveDownloadedBooksUseCase,
        getStorageUsage: GetStorageUsageUseCase,
        private val snackbar: SnackbarController,
    ) : ViewModel() {
        private val isClearing = MutableStateFlow(false)
        private val showClearAllDialog = MutableStateFlow(false)
        val uiState: StateFlow<StorageDownloadsUiState> =
            combine(
                appSettingsStore.settings,
                getStorageUsage(),
                observeDownloadedBooks(),
                isClearing,
                showClearAllDialog,
            ) { settings, usage, books, clearing, showDialog ->
                storageDownloadsUiState(settings, usage, books.map { it.audiobook.title }, clearing, showDialog)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StorageDownloadsUiState(),
            )

        fun onDownloadWifiOnlyChange(enabled: Boolean) {
            viewModelScope.launch {
                appSettingsStore.setDownloadWifiOnly(enabled)
            }
        }

        fun onAutoDeleteFinishedDownloadsChange(enabled: Boolean) {
            viewModelScope.launch {
                appSettingsStore.setAutoDeleteFinishedDownloads(enabled)
            }
        }

        fun requestClearAll() {
            if (uiState.value.hasDownloads) {
                showClearAllDialog.value = true
            }
        }

        fun dismissClearAll() {
            if (!isClearing.value) {
                showClearAllDialog.value = false
            }
        }

        fun confirmClearAll() {
            if (isClearing.value) return
            viewModelScope.launch {
                isClearing.value = true
                try {
                    deleteAllDownloads()
                    snackbar.success(R.string.storage_clear_all_done)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    snackbar.error(R.string.storage_clear_all_failed)
                } finally {
                    isClearing.value = false
                    showClearAllDialog.value = false
                }
            }
        }
    }

internal fun storageDownloadsUiState(
    settings: AppSettings,
    usage: StorageUsage,
    downloadedBookTitles: List<String>,
    isClearing: Boolean,
    showClearAllDialog: Boolean,
): StorageDownloadsUiState =
    StorageDownloadsUiState(
        isLoaded = true,
        downloadWifiOnly = settings.downloadWifiOnly,
        autoDeleteFinishedDownloads = settings.autoDeleteFinishedDownloads,
        downloadedBookTitles = downloadedBookTitles,
        storage = usage,
        isClearing = isClearing,
        showClearAllDialog = showClearAllDialog,
    )
