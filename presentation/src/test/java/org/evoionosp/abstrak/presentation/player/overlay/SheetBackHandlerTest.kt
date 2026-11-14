package org.evoionosp.abstrak.presentation.player.overlay
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheetBackHandlerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var sheetOpen by mutableStateOf(false)
    private var minimized = 0
    private var screenBacks = 0

    @Test
    fun `back minimizes the open sheet before the screen beneath it`() {
        showScreenAfterLaunch()
        sheetOpen = true
        composeRule.waitForIdle()
        pressBack()
        assertEquals(1, minimized)
        assertEquals(0, screenBacks)
    }

    @Test
    fun `back reaches the screen once the sheet is closed`() {
        showScreenAfterLaunch()
        sheetOpen = true
        composeRule.waitForIdle()
        sheetOpen = false
        composeRule.waitForIdle()
        pressBack()
        assertEquals(0, minimized)
        assertEquals(1, screenBacks)
    }

    private fun showScreenAfterLaunch() {
        var screenShown by mutableStateOf(false)
        composeRule.setContent {
            if (screenShown) BackHandler { screenBacks++ }
            SheetBackHandler(open = sheetOpen) { minimized++ }
        }
        screenShown = true
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }
}
