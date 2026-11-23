package org.evoionosp.abstrak.presentation.home
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.LibraryUiModel

data class HomeUiState(
    val username: String = "",
    val libraries: List<LibraryUiModel> = emptyList(),
    val selectedLibraryId: String? = null,
    val selectedLibraryName: String? = null,
    val audiobooks: List<AudiobookUiModel> = emptyList(),
    val ebooks: List<AudiobookUiModel> = emptyList(),
    val continueListening: List<AudiobookUiModel> = emptyList(),
    val recentlyAdded: List<AudiobookUiModel> = emptyList(),
    val discover: List<AudiobookUiModel> = emptyList(),
    val authorCount: Int = 0,
    val durationHours: Double = 0.0,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isOfflineMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
