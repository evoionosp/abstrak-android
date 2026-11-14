package org.evoionosp.abstrak.presentation.home
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveHomeAudiobooksUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveLibrarySyncStatusUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshHomeCatalogResult
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshHomeCatalogUseCase
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.SelectLibraryUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.LogoutUserUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.toDomain
import org.evoionosp.abstrak.presentation.ui.model.toUiModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val observeSessionUseCase: ObserveSessionUseCase,
        private val logoutUserUseCase: LogoutUserUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val observeLibrariesUseCase: ObserveLibrariesUseCase,
        private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
        private val observeHomeAudiobooksUseCase: ObserveHomeAudiobooksUseCase,
        private val observeContinueListeningUseCase: ObserveContinueListeningUseCase,
        private val observeLibrarySyncStatusUseCase: ObserveLibrarySyncStatusUseCase,
        private val refreshHomeCatalogUseCase: RefreshHomeCatalogUseCase,
        private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
        private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
        private val selectLibraryUseCase: SelectLibraryUseCase,
        private val playbackConnection: PlaybackConnection,
        private val snackbar: SnackbarController,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

        init {
            viewModelScope.launch {
                observeSessionUseCase().collect { session ->
                    val username = session?.username ?: ""
                    val firstName =
                        username
                            .substringBefore(
                                " ",
                            ).substringBefore("@")
                            .ifBlank { username }
                    _uiState.update { it.copy(username = firstName) }
                }
            }
            viewModelScope.launch {
                observeLibrariesUseCase().collect { libraries ->
                    _uiState.update {
                        it.copy(
                            libraries =
                                libraries.map { lib ->
                                    lib.toUiModel()
                                },
                        )
                    }
                }
            }
            viewModelScope.launch {
                observeSelectedLibraryUseCase().collect { selectedLibrary ->
                    _uiState.update {
                        it.copy(
                            selectedLibraryId = selectedLibrary?.id,
                            selectedLibraryName = selectedLibrary?.name,
                        )
                    }
                }
            }
            viewModelScope.launch {
                observeSelectedLibraryUseCase()
                    .map { it?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { libraryId ->
                        if (libraryId ==
                            null
                        ) {
                            emptyFlow()
                        } else {
                            observeHomeAudiobooksUseCase(libraryId)
                        }
                    }.collectLatest { audiobooks ->
                        val uiModels = audiobooks.map { it.toUiModel() }
                        val recentlyAdded = uiModels.take(12)
                        val discover =
                            uiModels
                                .sortedBy { it.title.lowercase() }
                                .filterIndexed { index, _ -> index % 2 == 0 }
                                .take(12)
                        val authorCount = uiModels.flatMap { it.authorNames }.distinct().size
                        val durationHours = uiModels.sumOf { it.durationInSeconds ?: 0L } / 3600.0
                        val (withAudio, ebooks) = uiModels.partition { it.hasAudio }
                        _uiState.update {
                            it.copy(
                                audiobooks = withAudio,
                                ebooks = ebooks,
                                recentlyAdded = recentlyAdded,
                                discover = discover,
                                authorCount = authorCount,
                                durationHours = durationHours,
                            )
                        }
                    }
            }
            viewModelScope.launch {
                observeSelectedLibraryUseCase()
                    .map { it?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { libraryId ->
                        if (libraryId ==
                            null
                        ) {
                            emptyFlow()
                        } else {
                            observeContinueListeningUseCase(libraryId)
                        }
                    }.collectLatest { continueListening ->
                        _uiState.update {
                            it.copy(
                                continueListening = continueListening.map { it.toUiModel() },
                            )
                        }
                    }
            }
            viewModelScope.launch {
                observeSelectedLibraryUseCase()
                    .map { it?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { libraryId ->
                        if (libraryId ==
                            null
                        ) {
                            emptyFlow()
                        } else {
                            observeLibrarySyncStatusUseCase(libraryId)
                        }
                    }.collectLatest { syncStatus ->
                        _uiState.update { it.copy(syncStatus = syncStatus) }
                    }
            }
        }

        fun playAudiobook(audiobook: AudiobookUiModel) {
            if (playbackConnection.playbackState.value.audiobook
                    ?.id == audiobook.id
            ) {
                playbackConnection.play()
            } else {
                playbackConnection.playAudiobook(audiobook.toDomain())
            }
        }

        fun onLibrarySelected(libraryId: String) {
            if (_uiState.value.selectedLibraryId == libraryId) return
            viewModelScope.launch {
                when (selectLibraryUseCase(libraryId)) {
                    is DomainResult.Success -> {
                        val session =
                            getValidSessionUseCase() ?: run {
                                expireSession()
                                return@launch
                            }
                        val refreshResult =
                            refreshSelectedLibraryAudiobooks(
                                session = session,
                                libraryId = libraryId,
                            )
                        val continueResult =
                            refreshContinueListening(
                                session = session,
                                libraryId = libraryId,
                            )
                        if (refreshResult.isAuthFailure() || continueResult.isAuthFailure()) {
                            expireSession()
                        }
                    }

                    is DomainResult.Failure -> {
                        snackbar.error(R.string.error_home_library_select)
                    }
                }
            }
        }

        fun refresh() {
            performRefresh(silent = false)
        }

        fun refreshSilently() {
            performRefresh(silent = true)
        }

        private fun performRefresh(silent: Boolean) {
            viewModelScope.launch {
                if (!silent) {
                    _uiState.update { it.copy(isRefreshing = true) }
                }
                when (val result = refreshHomeCatalogUseCase()) {
                    is RefreshHomeCatalogResult.Success -> {
                    }

                    is RefreshHomeCatalogResult.SessionExpired -> {
                        stopRefreshing(silent)
                        expireSession()
                        return@launch
                    }

                    is RefreshHomeCatalogResult.Failure -> {
                        if (!silent) {
                            showRefreshFailure(result.error)
                        }
                    }
                }
                stopRefreshing(silent)
            }
        }

        private fun stopRefreshing(silent: Boolean) {
            if (!silent) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }

        fun clearExpiredSession() {
            viewModelScope.launch {
                logoutUserUseCase()
            }
        }

        private fun showRefreshFailure(error: CatalogError) {
            when (error) {
                CatalogError.CONNECTIVITY_UNAVAILABLE -> snackbar.warning(R.string.catalog_offline)
                CatalogError.NO_AUDIOBOOK_LIBRARIES -> snackbar.warning(R.string.home_no_libraries)
                else -> snackbar.error(toMessageRes(error))
            }
        }

        private fun expireSession() {
            snackbar.warning(R.string.error_session_expired)
            _events.tryEmit(HomeUiEvent.SessionExpired)
            viewModelScope.launch {
                logoutUserUseCase()
            }
        }

        private suspend fun refreshSelectedLibraryAudiobooks(
            session: LoginSession,
            libraryId: String,
        ): DomainResult<Unit> =
            refreshSelectedLibraryAudiobooksUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = libraryId,
            )

        private suspend fun refreshContinueListening(
            session: LoginSession,
            libraryId: String,
        ): DomainResult<Unit> =
            refreshContinueListeningUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = libraryId,
            )

        private fun DomainResult<Unit>.isAuthFailure(): Boolean =
            this is DomainResult.Failure && error == CatalogError.AUTH

        private fun toMessageRes(error: CatalogError): Int =
            when (error) {
                CatalogError.AUTH -> R.string.error_login_failed
                CatalogError.NETWORK -> R.string.error_network
                CatalogError.CONNECTIVITY_UNAVAILABLE -> R.string.error_network
                CatalogError.NO_AUDIOBOOK_LIBRARIES -> R.string.home_no_libraries
                CatalogError.NOT_FOUND -> R.string.error_home_library_select
                CatalogError.UNKNOWN -> R.string.error_unknown
            }
    }
