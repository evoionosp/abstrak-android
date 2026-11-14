package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AbstrakThemeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `moonless night makes a dark theme's backgrounds black`() {
        val (scheme, backgrounds) = themed(ThemePreference.DARK, moonlessNight = true)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(AbstrakBackgrounds(Color.Black, Color.Black), backgrounds)
    }

    @Test
    fun `without moonless night a dark theme's pages are its own flat surface`() {
        val (scheme, backgrounds) = themed(ThemePreference.DARK, moonlessNight = false)
        assertNotEquals(Color.Black, scheme.surface)
        assertEquals(AbstrakBackgrounds(scheme.surface, scheme.surface), backgrounds)
    }

    @Test
    fun `moonless night leaves a light theme alone`() {
        val (scheme, backgrounds) = themed(ThemePreference.LIGHT, moonlessNight = true)
        assertNotEquals(Color.Black, scheme.surface)
        assertEquals(scheme.surfaceContainerLowest, backgrounds.screenBottom)
    }

    private fun themed(
        preference: ThemePreference,
        moonlessNight: Boolean,
    ): Pair<ColorScheme, AbstrakBackgrounds> {
        lateinit var themed: Pair<ColorScheme, AbstrakBackgrounds>
        composeRule.setContent {
            AbstrakTheme(themePreference = preference, dynamicColor = false, moonlessNight = moonlessNight) {
                themed = MaterialTheme.colorScheme to MaterialTheme.backgrounds
            }
        }
        composeRule.waitForIdle()
        return themed
    }
}
