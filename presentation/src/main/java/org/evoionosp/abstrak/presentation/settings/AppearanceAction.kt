package org.evoionosp.abstrak.presentation.settings
import org.evoionosp.abstrak.domain.settings.AppIconPalette
import org.evoionosp.abstrak.domain.settings.AppIconVariant
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference

/** What the Appearance settings screen asks for. */
sealed interface AppearanceAction {
    data class ThemePreferenceChange(
        val preference: ThemePreference,
    ) : AppearanceAction

    data class MoonlessNightChange(
        val enabled: Boolean,
    ) : AppearanceAction

    data class DynamicColorChange(
        val enabled: Boolean,
    ) : AppearanceAction

    data class CoverThemeChange(
        val enabled: Boolean,
    ) : AppearanceAction

    /** Light or Dark for the app icon, keeping its palette. */
    data class AppIconVariantChange(
        val variant: AppIconVariant,
    ) : AppearanceAction

    /** The app icon's palette, keeping Light or Dark. */
    data class AppIconPaletteChange(
        val palette: AppIconPalette,
    ) : AppearanceAction
}
