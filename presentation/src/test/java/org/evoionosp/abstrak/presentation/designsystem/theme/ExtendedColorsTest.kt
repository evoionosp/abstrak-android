package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtendedColorsTest {
    private val staticLight = abstrakExtendedColors(primaryLight, isLight = true)
    private val staticDark = abstrakExtendedColors(primaryDark, isLight = false)
    private val dynamicLight = abstrakExtendedColors(Color(0xFF3F5AA9), isLight = true)

    @Test
    fun `every role pair reads clearly, light and dark, static and dynamic`() {
        listOf(staticLight, staticDark, dynamicLight).forEach { colors ->
            assertReadable(colors.success, colors.onSuccess)
            assertReadable(colors.successContainer, colors.onSuccessContainer)
            assertReadable(colors.warning, colors.onWarning)
            assertReadable(colors.warningContainer, colors.onWarningContainer)
        }
    }

    @Test
    fun `like the error role, light themes get strong colors with light content and dark themes the reverse`() {
        assertTrue(staticLight.success.luminance() < staticDark.success.luminance())
        assertTrue(staticLight.warning.luminance() < staticDark.warning.luminance())
        assertTrue(staticLight.onSuccess.luminance() > 0.9f && staticLight.onWarning.luminance() > 0.9f)
        assertTrue(staticDark.onSuccess.luminance() < 0.1f && staticDark.onWarning.luminance() < 0.1f)
    }

    @Test
    fun `success stays green and warning orange after harmonizing`() {
        listOf(staticLight, staticDark, dynamicLight).forEach { colors ->
            assertTrue("success hue ${hue(colors.success)}", hue(colors.success) in 70f..170f)
            assertTrue("warning hue ${hue(colors.warning)}", hue(colors.warning) in 10f..60f)
        }
    }

    @Test
    fun `they follow the theme's primary`() {
        assertNotEquals(staticLight.warning, dynamicLight.warning)
    }

    private fun assertReadable(
        container: Color,
        content: Color,
    ) {
        val lighter = max(container.luminance(), content.luminance())
        val darker = min(container.luminance(), content.luminance())
        val contrast = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("contrast $contrast for $content on $container", contrast >= 4.5f)
    }

    private fun hue(color: Color): Float {
        val maxChannel = max(color.red, max(color.green, color.blue))
        val minChannel = min(color.red, min(color.green, color.blue))
        val delta = maxChannel - minChannel
        if (delta == 0f) return 0f
        val hue =
            when (maxChannel) {
                color.red -> 60f * (((color.green - color.blue) / delta) % 6f)
                color.green -> 60f * ((color.blue - color.red) / delta + 2f)
                else -> 60f * ((color.red - color.green) / delta + 4f)
            }
        return (hue + 360f) % 360f
    }
}
