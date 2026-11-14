package org.evoionosp.abstrak.presentation.downloads
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.usecase.DeleteBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveDownloadedBooksUseCase
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.message.UiText

@HiltViewModel
class DownloadsViewModel
    @Inject
    constructor(
        private val observeDownloadedBooks: ObserveDownloadedBooksUseCase,
        private val deleteBookDownload: DeleteBookDownloadUseCase,
        @param:Named("default") private val defaultDispatcher: CoroutineDispatcher,
        private val snackbar: SnackbarController,
    ) : ViewModel() {
        private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
        private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())
        val uiState: StateFlow<DownloadsUiState> =
            combine(observeDownloadedBooks(), selectedIds, pendingDeleteIds, ::downloadsUiState)
                .flowOn(defaultDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

        init {
            viewModelScope.launch {
                combine(selectedIds, pendingDeleteIds) { selected, pending ->
                    selected.isNotEmpty() || pending.isNotEmpty()
                }.distinctUntilChanged()
                    .collectLatest { holdsBooks ->
                        if (holdsBooks) observeDownloadedBooks().collect(::forgetUnlistedBooks)
                    }
            }
        }

        fun toggleSelection(audiobookId: String) {
            val selected = uiState.value.selectedIds
            selectedIds.value = if (audiobookId in selected) selected - audiobookId else selected + audiobookId
        }

        fun selectAll() {
            selectedIds.value = uiState.value.books.mapTo(HashSet()) { it.audiobook.id }
        }

        fun clearSelection() {
            selectedIds.value = emptySet()
        }

        fun requestDelete(audiobookId: String) {
            pendingDeleteIds.value = setOf(audiobookId)
        }

        fun requestDeleteSelected() {
            pendingDeleteIds.value = uiState.value.selectedIds
        }

        fun dismissDelete() {
            pendingDeleteIds.value = emptySet()
        }

        fun confirmDelete() {
            val listedIds = uiState.value.books.mapTo(HashSet()) { it.audiobook.id }
            val ids = pendingDeleteIds.value intersect listedIds
            pendingDeleteIds.value = emptySet()
            if (ids.isEmpty()) return
            selectedIds.update { it - ids }
            viewModelScope.launch {
                val deletions =
                    ids.map { id ->
                        async {
                            try {
                                deleteBookDownload(id)
                                true
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                val succeeded = deletions.awaitAll().count { it }
                val failed = ids.size - succeeded
                if (succeeded > 0) {
                    snackbar.success(UiText.Plural(R.plurals.downloads_deleted, succeeded))
                }
                if (failed > 0) {
                    snackbar.error(UiText.Plural(R.plurals.downloads_delete_failed, failed))
                }
            }
        }

        private fun forgetUnlistedBooks(books: List<DownloadedBook>) {
            val listedIds = books.mapTo(HashSet()) { it.audiobook.id }
            selectedIds.update { it intersect listedIds }
            pendingDeleteIds.update { it intersect listedIds }
        }
    }
