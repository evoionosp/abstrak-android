package org.evoionosp.abstrak.presentation.ui.message
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbar
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbarVisuals
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowSnackbarMessagesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val controller = SnackbarController()
    private val hostState = SnackbarHostState()

    private fun showHost() {
        composeRule.setContent {
            ShowSnackbarMessages(controller = controller, hostState = hostState)
            SnackbarHost(hostState) { AbstrakSnackbar(it) }
        }
    }

    @Test
    fun `a message from anywhere shows in the uniform snackbar with its type`() {
        showHost()
        controller.error(UiText.Plain("Couldn't refresh"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Couldn't refresh").assertIsDisplayed()
        assertEquals(SnackbarType.ERROR, (hostState.currentSnackbarData?.visuals as AbstrakSnackbarVisuals).type)
    }

    @Test
    fun `a newer message replaces the one showing`() {
        showHost()
        controller.warning(UiText.Plain("You're offline"))
        composeRule.waitForIdle()
        controller.success(UiText.Plain("Deleted all downloads"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Deleted all downloads").assertIsDisplayed()
        composeRule.onNodeWithText("You're offline").assertDoesNotExist()
    }
}
