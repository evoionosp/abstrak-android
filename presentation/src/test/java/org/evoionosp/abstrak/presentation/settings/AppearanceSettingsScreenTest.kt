package org.evoionosp.abstrak.presentation.settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppIconPalette
import org.evoionosp.abstrak.domain.settings.AppIconVariant
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val actions = mutableListOf<AppearanceAction>()
        show(SettingsUiState(themePreference = ThemePreference.DARK), onAction = { actions += it })
        moonlessNight().assertIsOff().assertIsEnabled()
        composeRule.onNode(hasText(string(R.string.theme_moonless_night_desc))).assertExists()
        moonlessNight().performClick()
        assertEquals(listOf(AppearanceAction.MoonlessNightChange(true)), actions)
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

    @Test
    @Config(qualifiers = "h2400dp")
    fun `the app icon group follows color, showing the selected icon`() {
        show(SettingsUiState(appIcon = AppIcon.ISLAND_LIGHT))
        assertTrue(topOf(R.string.preferences_app_icon) > topOf(R.string.preferences_color))
        variant(R.string.app_icon_light).assertIsSelected()
        variant(R.string.app_icon_dark).assertIsNotSelected()
        palette(R.string.app_icon_island).assertIsSelected()
        palette(R.string.app_icon_forest).assertIsNotSelected()
        composeRule.onNodeWithText(string(R.string.app_icon_applies_in_background)).assertExists()
    }

    @Test
    @Config(qualifiers = "h2400dp")
    fun `the pill switches the app icon between light and dark`() {
        val actions = mutableListOf<AppearanceAction>()
        show(SettingsUiState(appIcon = AppIcon.FOREST_LIGHT), onAction = { actions += it })
        variant(R.string.app_icon_dark).performClick()
        assertEquals(listOf(AppearanceAction.AppIconVariantChange(AppIconVariant.DARK)), actions)
    }

    @Test
    @Config(qualifiers = "h2400dp")
    fun `tapping a preview picks its palette`() {
        val actions = mutableListOf<AppearanceAction>()
        show(SettingsUiState(appIcon = AppIcon.ISLAND_DARK), onAction = { actions += it })
        variant(R.string.app_icon_dark).assertIsSelected()
        palette(R.string.app_icon_forest).performClick()
        assertEquals(listOf(AppearanceAction.AppIconPaletteChange(AppIconPalette.FOREST)), actions)
    }

    private fun show(
        state: SettingsUiState,
        onAction: (AppearanceAction) -> Unit = {},
    ) {
        composeRule.setContent {
            AppearanceSettingsScreen(
                settingsState = state,
                onBackClick = {},
                onAction = onAction,
            )
        }
    }

    private fun moonlessNight(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(R.string.theme_moonless_night)) and isToggleable())

    /** A segment of the app icon's Light/Dark pill: its label is its only text, unlike the theme options. */
    private fun variant(label: Int): SemanticsNodeInteraction = composeRule.onNode(hasOnlyText(string(label)) and isSelectable())

    private fun palette(label: Int): SemanticsNodeInteraction = composeRule.onNode(hasText(string(label)) and isSelectable())

    private fun topOf(label: Int): Float =
        composeRule
            .onNodeWithText(string(label))
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun hasOnlyText(text: String) =
        SemanticsMatcher("has only the text '$text'") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == listOf(text)
        }

    private fun string(id: Int): String = composeRule.activity.getString(id)
}
