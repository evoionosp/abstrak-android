package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackgroundsTest {
    private val light = lightColorScheme()
    private val dark = darkColorScheme()

    @Test
    fun `a light theme's pages fade to its brightest container`() {
        assertEquals(
            AbstrakBackgrounds(screenTop = light.surface, screenBottom = light.surfaceContainerLowest),
            abstrakBackgrounds(light, darkTheme = false),
        )
    }

    @Test
    fun `a dark theme's pages are flat surface, the player's color`() {
        assertEquals(
            AbstrakBackgrounds(screenTop = dark.surface, screenBottom = dark.surface),
            abstrakBackgrounds(dark, darkTheme = true),
        )
    }

    @Test
    fun `moonless night turns every page black`() {
        val backgrounds = abstrakBackgrounds(dark.moonlessNight(), darkTheme = true)
        assertEquals(AbstrakBackgrounds(Color.Black, Color.Black), backgrounds)
    }

    @Test
    fun `moonless night turns the background roles black`() {
        val moonless = dark.moonlessNight()
        assertEquals(Color.Black, moonless.surface)
        assertEquals(Color.Black, moonless.background)
        assertEquals(Color.Black, moonless.surfaceDim)
        assertEquals(Color.Black, moonless.surfaceContainerLowest)
    }

    @Test
    fun `moonless night moves every container one step darker, keeping their order`() {
        val moonless = dark.moonlessNight()
        assertEquals(dark.surface, moonless.surfaceContainerLow)
        assertEquals(dark.surfaceContainerLow, moonless.surfaceContainer)
        assertEquals(dark.surfaceContainer, moonless.surfaceContainerHigh)
        assertEquals(dark.surfaceContainerHigh, moonless.surfaceContainerHighest)
        assertEquals(dark.surfaceContainerHighest, moonless.surfaceBright)
        assertNotEquals(Color.Black, moonless.surfaceContainerLow)
    }

    @Test
    fun `moonless night leaves every other role alone`() {
        val moonless = dark.moonlessNight()
        val expected =
            dark.copy(
                background = moonless.background,
                surface = moonless.surface,
                surfaceDim = moonless.surfaceDim,
                surfaceContainerLowest = moonless.surfaceContainerLowest,
                surfaceContainerLow = moonless.surfaceContainerLow,
                surfaceContainer = moonless.surfaceContainer,
                surfaceContainerHigh = moonless.surfaceContainerHigh,
                surfaceContainerHighest = moonless.surfaceContainerHighest,
                surfaceBright = moonless.surfaceBright,
            )
        assertEquals(expected.toString(), moonless.toString())
    }
}
