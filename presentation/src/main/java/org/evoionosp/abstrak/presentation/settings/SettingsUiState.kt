package org.evoionosp.abstrak.presentation.settings
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val useDynamicColor: Boolean = true,
    val useCoverTheme: Boolean = true,
    val useMoonlessNight: Boolean = false,
)
