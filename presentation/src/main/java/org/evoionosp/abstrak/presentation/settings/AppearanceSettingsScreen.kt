package org.evoionosp.abstrak.presentation.settings
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.GroupDivider
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListGroup
import org.evoionosp.abstrak.presentation.designsystem.component.list.RadioListItem
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference

private data class ThemeOption(
    val preference: ThemePreference,
    @StringRes val label: Int,
    @StringRes val description: Int,
)

private val themeOptions =
    listOf(
        ThemeOption(ThemePreference.SYSTEM, R.string.theme_system_default, R.string.theme_system_default_desc),
        ThemeOption(ThemePreference.DARK, R.string.theme_dark, R.string.theme_dark_desc),
        ThemeOption(ThemePreference.LIGHT, R.string.theme_light, R.string.theme_light_desc),
    )

@Composable
fun AppearanceSettingsScreen(
    settingsState: SettingsUiState,
    onBackClick: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCoverThemeChange: (Boolean) -> Unit,
    onMoonlessNightChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    SettingsScaffold(
        title = stringResource(R.string.preferences_appearance),
        onBack = onBackClick,
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    ) {
        item {
            SettingsIntro(text = stringResource(R.string.preferences_appearance_intro))
        }
        item {
            ListGroup(title = stringResource(R.string.preferences_app_theme)) {
                themeOptions.forEachIndexed { index, option ->
                    if (index > 0) GroupDivider()
                    RadioListItem(
                        headline = stringResource(option.label),
                        supporting = stringResource(option.description),
                        selected = settingsState.themePreference == option.preference,
                        onClick = { onThemePreferenceChange(option.preference) },
                    )
                }
                GroupDivider()
                SwitchListItem(
                    headline = stringResource(R.string.theme_moonless_night),
                    supporting = stringResource(R.string.theme_moonless_night_desc),
                    checked = settingsState.useMoonlessNight,
                    onCheckedChange = onMoonlessNightChange,
                    enabled = settingsState.themePreference != ThemePreference.LIGHT,
                )
            }
        }
        item {
            ListGroup(title = stringResource(R.string.preferences_color)) {
                SwitchListItem(
                    headline = stringResource(R.string.use_dynamic_color),
                    supporting = stringResource(R.string.use_dynamic_color_desc),
                    checked = settingsState.useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
                GroupDivider()
                SwitchListItem(
                    headline = stringResource(R.string.now_playing_cover_theme),
                    supporting = stringResource(R.string.now_playing_cover_theme_desc),
                    checked = settingsState.useCoverTheme,
                    onCheckedChange = onCoverThemeChange,
                )
            }
        }
    }
}
