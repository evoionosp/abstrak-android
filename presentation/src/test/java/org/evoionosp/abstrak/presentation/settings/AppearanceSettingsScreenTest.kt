package org.evoionosp.abstrak.presentation.settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1000dp")
class AppearanceSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `moonless night is a switch in the app theme group, off by default`() {
        val changes = mutableListOf<Boolean>()
        show(SettingsUiState(themePreference = ThemePreference.DARK), onMoonlessNightChange = { changes += it })
        moonlessNight().assertIsOff().assertIsEnabled()
        composeRule.onNode(hasText(string(R.string.theme_moonless_night_desc))).assertExists()
        moonlessNight().performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun `moonless night is disabled under the light theme, which it doesn't change`() {
        show(SettingsUiState(themePreference = ThemePreference.LIGHT))
        moonlessNight().assertIsNotEnabled()
    }

    @Test
    fun `moonless night stays available when the theme follows the system`() {
        show(SettingsUiState(themePreference = ThemePreference.SYSTEM))
        moonlessNight().assertIsEnabled()
    }

    private fun show(
        state: SettingsUiState,
        onMoonlessNightChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            AppearanceSettingsScreen(
                settingsState = state,
                onBackClick = {},
                onThemePreferenceChange = {},
                onDynamicColorChange = {},
                onCoverThemeChange = {},
                onMoonlessNightChange = onMoonlessNightChange,
            )
        }
    }

    private fun moonlessNight(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(R.string.theme_moonless_night)) and isToggleable())

    private fun string(id: Int): String = composeRule.activity.getString(id)
}
