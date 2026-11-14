package org.evoionosp.abstrak.presentation.player
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.width
import org.evoionosp.abstrak.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerFooterActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the actions share one width, each centered in a third of the row`() {
        composeRule.setContent {
            PlayerFooterActions(sleepLabel = "Zz", speedLabel = "1x", onOpenSheet = {})
        }
        val activity = composeRule.activity
        val bounds =
            listOf(R.string.now_playing_sleep, R.string.now_playing_speed, R.string.now_playing_chapters)
                .map { action(activity.getString(it)) }
        val rowWidth =
            composeRule
                .onRoot()
                .getBoundsInRoot()
                .width.value
        bounds.forEach { assertEquals(bounds[2].width.value, it.width.value, 0.5f) }
        bounds.forEachIndexed { index, rect ->
            val center = (rect.left.value + rect.right.value) / 2
            assertEquals(rowWidth * (2 * index + 1) / 6, center, 1f)
        }
    }

    private fun action(label: String): DpRect = composeRule.onNode(hasText(label) and hasClickAction()).getBoundsInRoot()
}
