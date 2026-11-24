package org.evoionosp.abstrak.domain.settings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIconTest {
    @Test
    fun `the app ships with island light`() {
        assertEquals(AppIcon.ISLAND_LIGHT, AppIcon.DEFAULT)
    }

    @Test
    fun `every palette comes in a light and a dark icon`() {
        AppIconPalette.entries.forEach { palette ->
            AppIconVariant.entries.forEach { variant ->
                val icon = AppIcon.of(palette, variant)
                assertEquals(palette, icon.palette)
                assertEquals(variant, icon.variant)
            }
        }
        assertEquals(AppIconPalette.entries.size * AppIconVariant.entries.size, AppIcon.entries.size)
    }

    @Test
    fun `switching between light and dark keeps the palette`() {
        assertEquals(AppIcon.FOREST_DARK, AppIcon.FOREST_LIGHT.withVariant(AppIconVariant.DARK))
        assertEquals(AppIcon.ISLAND_LIGHT, AppIcon.ISLAND_DARK.withVariant(AppIconVariant.LIGHT))
        assertEquals(AppIcon.ISLAND_DARK, AppIcon.ISLAND_DARK.withVariant(AppIconVariant.DARK))
    }

    @Test
    fun `picking a palette keeps light or dark`() {
        assertEquals(AppIcon.FOREST_DARK, AppIcon.ISLAND_DARK.withPalette(AppIconPalette.FOREST))
        assertEquals(AppIcon.ISLAND_LIGHT, AppIcon.FOREST_LIGHT.withPalette(AppIconPalette.ISLAND))
    }

    @Test
    fun `a stored name maps back to its icon, anything else to the default`() {
        AppIcon.entries.forEach { assertEquals(it, AppIcon.fromName(it.name)) }
        assertEquals(AppIcon.DEFAULT, AppIcon.fromName(null))
        assertEquals(AppIcon.DEFAULT, AppIcon.fromName("SUNSET_DARK"))
    }
}
