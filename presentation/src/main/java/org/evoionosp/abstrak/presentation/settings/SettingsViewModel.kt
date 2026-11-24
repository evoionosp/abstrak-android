package org.evoionosp.abstrak.presentation.settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val appSettingsStore: AppSettingsStore,
    ) : ViewModel() {
        private val appIconChanges = Mutex()

        val uiState: StateFlow<SettingsUiState> =
            appSettingsStore.settings
                .map { settings ->
                    SettingsUiState(
                        themePreference = ThemePreference.fromName(settings.themePreference),
                        useDynamicColor = settings.useDynamicColor,
                        useCoverTheme = settings.useCoverTheme,
                        useMoonlessNight = settings.useMoonlessNight,
                        appIcon = settings.appIcon,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = SettingsUiState(),
                )

        fun onAppearanceAction(action: AppearanceAction) {
            when (action) {
                is AppearanceAction.ThemePreferenceChange -> save { setThemePreference(action.preference.name) }
                is AppearanceAction.MoonlessNightChange -> save { setMoonlessNight(action.enabled) }
                is AppearanceAction.DynamicColorChange -> save { setDynamicColor(action.enabled) }
                is AppearanceAction.CoverThemeChange -> save { setCoverTheme(action.enabled) }
                is AppearanceAction.AppIconVariantChange -> changeAppIcon { it.withVariant(action.variant) }
                is AppearanceAction.AppIconPaletteChange -> changeAppIcon { it.withPalette(action.palette) }
            }
        }

        private fun save(write: suspend AppSettingsStore.() -> Unit) {
            viewModelScope.launch { appSettingsStore.write() }
        }

        /** Changes the stored icon one change at a time, so a variant and a palette picked together both stick. */
        private fun changeAppIcon(change: (AppIcon) -> AppIcon) {
            viewModelScope.launch {
                appIconChanges.withLock {
                    appSettingsStore.setAppIcon(change(appSettingsStore.settings.first().appIcon))
                }
            }
        }
    }
