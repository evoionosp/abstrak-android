package org.evoionosp.abstrak.presentation.splash
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.abstrak.domain.library.usecase.BootstrapHomeCatalogUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.LogoutUserUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val observeSessionUseCase: ObserveSessionUseCase,
        private val logoutUserUseCase: LogoutUserUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val bootstrapHomeCatalogUseCase: BootstrapHomeCatalogUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SplashUiState())
        val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
        private var currentSession: LoginSession? = null

        init {
            viewModelScope.launch {
                observeSessionUseCase()
                    .onEach { currentSession = it }
                    .distinctUntilChanged { old, new -> old.isSameRouting(new) }
                    .collectLatest { session ->
                        if (session == null) {
                            _uiState.value =
                                SplashUiState(
                                    isLoading = false,
                                    startupDestination = StartupDestination.Auth,
                                )
                            return@collectLatest
                        }
                        bootstrapCatalog()
                    }
            }
            viewModelScope.launch {
                observeSessionUseCase().collect { session ->
                    if (session == null) return@collect
                    _uiState.update { state ->
                        when (val destination = state.startupDestination) {
                            is StartupDestination.Home -> {
                                if (destination.session == session) {
                                    state
                                } else {
                                    state.copy(
                                        startupDestination = StartupDestination.Home(session),
                                    )
                                }
                            }

                            is StartupDestination.CatalogLoadError -> {
                                if (destination.session == session) {
                                    state
                                } else {
                                    state.copy(
                                        startupDestination = destination.copy(session = session),
                                    )
                                }
                            }

                            StartupDestination.Auth -> {
                                state
                            }
                        }
                    }
                }
            }
        }

        fun retryCatalogBootstrap() {
            if (currentSession == null) return
            viewModelScope.launch {
                bootstrapCatalog()
            }
        }

        fun logout() {
            viewModelScope.launch {
                logoutUserUseCase()
            }
        }

        private suspend fun bootstrapCatalog() {
            _uiState.update { it.copy(isLoading = true) }
            val activeSession =
                getValidSessionUseCase() ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                    return
                }
            val bootstrapResult =
                bootstrapHomeCatalogUseCase(
                    baseUrl = activeSession.baseUrl,
                    accessToken = activeSession.accessToken,
                )
            currentCoroutineContext().ensureActive()
            _uiState.value =
                SplashUiState(
                    isLoading = false,
                    startupDestination =
                        when (bootstrapResult) {
                            is BootstrapHomeCatalogResult.Success,
                            BootstrapHomeCatalogResult.NoLibrariesAvailable,
                            -> {
                                StartupDestination.Home(activeSession)
                            }

                            is BootstrapHomeCatalogResult.Failure -> {
                                StartupDestination.CatalogLoadError(
                                    session = activeSession,
                                    error = bootstrapResult.error,
                                )
                            }
                        },
                )
        }

        private fun LoginSession?.isSameRouting(other: LoginSession?): Boolean {
            if (this == null || other == null) return this == null && other == null
            return baseUrl == other.baseUrl && username == other.username && userId == other.userId
        }
    }
