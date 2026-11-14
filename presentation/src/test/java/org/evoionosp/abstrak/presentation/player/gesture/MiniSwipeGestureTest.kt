package org.evoionosp.abstrak.presentation.player.gesture
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiniSwipeGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a drag cut off by the swipe turning off springs the card back`() {
        var enabled by mutableStateOf(true)
        var dismissals = 0
        lateinit var miniSwipe: MiniSwipeState
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            miniSwipe = remember { MiniSwipeState(scope) { dismissals++ } }
            Box(
                modifier =
                    Modifier
                        .size(width = 300.dp, height = 80.dp)
                        .testTag(CARD)
                        .miniSwipeGesture(miniSwipe, enabled = enabled, distancePx = 1_000f),
            )
        }
        composeRule.onNodeWithTag(CARD).performTouchInput {
            down(center)
            moveBy(Offset(150f, 0f))
            moveBy(Offset(150f, 0f))
        }
        composeRule.runOnIdle { assertTrue(miniSwipe.offsetPx > 0f) }
        enabled = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CARD).performTouchInput { up() }
        composeRule.waitForIdle()
        assertEquals(0f, miniSwipe.offsetPx, 0.5f)
        assertEquals(0, dismissals)
    }

    private companion object {
        const val CARD = "card"
    }
}
