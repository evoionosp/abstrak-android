package org.evoionosp.abstrak.presentation.designsystem.theme

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
    ;

    companion object {
        /** Parses a stored preference name, falling back to following the system. */
        fun fromName(name: String): ThemePreference =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
