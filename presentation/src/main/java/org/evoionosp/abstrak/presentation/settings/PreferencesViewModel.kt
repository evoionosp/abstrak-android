package org.evoionosp.abstrak.presentation.settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.session.usecase.LogoutUserUseCase
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

@HiltViewModel
class PreferencesViewModel
    @Inject
    constructor(
        private val logoutUserUseCase: LogoutUserUseCase,
        private val appSettingsStore: AppSettingsStore,
    ) : ViewModel() {
        private val _isLoggingOut = MutableStateFlow(false)
        val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()
        val searchAllLibraries: StateFlow<Boolean?> =
            appSettingsStore.settings
                .map { it.searchAllLibraries }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        fun onSearchAllLibrariesChange(enabled: Boolean) {
            viewModelScope.launch {
                appSettingsStore.setSearchAllLibraries(enabled)
            }
        }

        fun logout(onComplete: () -> Unit) {
            if (_isLoggingOut.value) return
            viewModelScope.launch {
                _isLoggingOut.value = true
                try {
                    logoutUserUseCase()
                } finally {
                    _isLoggingOut.value = false
                }
                onComplete()
            }
        }
    }
