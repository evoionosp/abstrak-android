package org.evoionosp.abstrak.presentation.designsystem.component.snackbar
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.abstrakExtendedColors
import org.evoionosp.abstrak.presentation.designsystem.theme.primaryDark
import org.evoionosp.abstrak.presentation.designsystem.theme.primaryLight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AbstrakSnackbarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `each type takes Material's roles for it`() {
        listOf(
            lightColorScheme() to abstrakExtendedColors(primaryLight, isLight = true),
            darkColorScheme() to abstrakExtendedColors(primaryDark, isLight = false),
        ).forEach { (scheme, extended) ->
            assertEquals(
                SnackbarColors(scheme.inverseSurface, scheme.inverseOnSurface),
                snackbarColors(SnackbarType.INFO, scheme, extended),
            )
            assertEquals(SnackbarColors(scheme.error, scheme.onError), snackbarColors(SnackbarType.ERROR, scheme, extended))
            assertEquals(
                SnackbarColors(extended.success, extended.onSuccess),
                snackbarColors(SnackbarType.SUCCESS, scheme, extended),
            )
            assertEquals(
                SnackbarColors(extended.warning, extended.onWarning),
                snackbarColors(SnackbarType.WARNING, scheme, extended),
            )
        }
    }

    @Test
    fun `a snackbar shows its message and names its type`() {
        val hostState = SnackbarHostState()
        composeRule.setContent {
            SnackbarHost(hostState) { AbstrakSnackbar(it) }
            LaunchedEffect(Unit) {
                hostState.showSnackbar(AbstrakSnackbarVisuals("Couldn't play the book", SnackbarType.ERROR))
            }
        }
        composeRule.onNodeWithText("Couldn't play the book").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.snackbar_error)).assertIsDisplayed()
    }

    @Test
    fun `every type has its own icon and label`() {
        assertEquals(
            SnackbarType.entries.size,
            SnackbarType.entries
                .map { it.icon }
                .toSet()
                .size,
        )
        assertEquals(
            SnackbarType.entries.size,
            SnackbarType.entries
                .map { it.label }
                .toSet()
                .size,
        )
    }
}
