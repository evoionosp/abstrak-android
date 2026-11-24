package org.evoionosp.abstrak.domain.settings

/** The color families the app icon comes in. */
enum class AppIconPalette {
    FOREST,
    ISLAND,
}

/** Whether the app icon sits on a light or a dark background. */
enum class AppIconVariant {
    LIGHT,
    DARK,
}

/**
 * The launcher icon picked in Settings > Appearance: a palette in its light or dark variant.
 * Every icon is the same Folded A in different colors.
 */
enum class AppIcon(
    val palette: AppIconPalette,
    val variant: AppIconVariant,
) {
    ISLAND_LIGHT(AppIconPalette.ISLAND, AppIconVariant.LIGHT),
    ISLAND_DARK(AppIconPalette.ISLAND, AppIconVariant.DARK),
    FOREST_LIGHT(AppIconPalette.FOREST, AppIconVariant.LIGHT),
    FOREST_DARK(AppIconPalette.FOREST, AppIconVariant.DARK),
    ;

    /** [palette] in this icon's variant. */
    fun withPalette(palette: AppIconPalette): AppIcon = of(palette, variant)

    /** This icon's palette in [variant]: switching Light and Dark keeps the chosen palette. */
    fun withVariant(variant: AppIconVariant): AppIcon = of(palette, variant)

    companion object {
        /** The icon the app ships with. */
        val DEFAULT = ISLAND_LIGHT

        fun of(
            palette: AppIconPalette,
            variant: AppIconVariant,
        ): AppIcon = entries.first { it.palette == palette && it.variant == variant }

        /** The icon stored as [name], or [DEFAULT] for none or one this version doesn't know. */
        fun fromName(name: String?): AppIcon = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
