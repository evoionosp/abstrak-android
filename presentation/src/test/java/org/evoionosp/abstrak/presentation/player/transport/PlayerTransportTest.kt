package org.evoionosp.abstrak.presentation.player.transport
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerTransportTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a row with room keeps the full sizes`() {
        assertEquals(TransportButtonSizes(seek = 64.dp, playPause = 96.dp), transportButtonSizes(400.dp))
    }

    @Test
    fun `a narrow row shrinks seek and play-pause to keep the gaps`() {
        val sizes = transportButtonSizes(312.dp)
        assertEquals(48f, sizes.seek.value, 0.01f)
        assertEquals(72f, sizes.playPause.value, 0.01f)
        assertEquals(42f, sizes.playPauseIcon.value, 0.01f)
    }

    @Test
    fun `buttons never drop below the touch target and always fit with their gaps`() {
        listOf(304.dp, 312.dp, 345.dp, 363.dp, 368.dp, 500.dp).forEach { width ->
            val sizes = transportButtonSizes(width)
            assertTrue("$width: seek ${sizes.seek}", sizes.seek >= 48.dp)
            assertTrue("$width: play ${sizes.playPause}", sizes.playPause >= sizes.seek)
            val used = 48.dp * 2 + sizes.seek * 2 + sizes.playPause + TransportMinGap * 4
            assertTrue("$width: uses $used", used <= width + 0.01.dp)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `on a narrow phone play-pause keeps its distance from the seek buttons`() {
        assertGapsAroundPlayPause()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `on a typical phone play-pause keeps its distance from the seek buttons`() {
        assertGapsAroundPlayPause()
    }

    private fun assertGapsAroundPlayPause() {
        composeRule.setContent {
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                PlayerTransport(
                    chapterTitle = "Chapter One",
                    chapterTimeLeftMs = 60_000L,
                    positionMs = 30_000L,
                    durationMs = 300_000L,
                    isPlaying = true,
                    isLoading = false,
                    onAction = {},
                )
            }
        }
        val resources = composeRule.activity.resources
        val rewind = bounds(resources.getQuantityString(R.plurals.now_playing_seek_backward, 15, 15))
        val playPause = bounds(resources.getString(R.string.now_playing_pause))
        val forward = bounds(resources.getQuantityString(R.plurals.now_playing_seek_forward, 30, 30))
        assertAtLeast(TransportMinGap, playPause.left - rewind.right)
        assertAtLeast(TransportMinGap, forward.left - playPause.right)
    }

    private fun bounds(description: String): DpRect = composeRule.onNodeWithContentDescription(description).getBoundsInRoot()

    private fun assertAtLeast(
        expected: Dp,
        actual: Dp,
    ) {
        assertTrue("gap $actual, wanted at least $expected", actual >= expected - 0.5.dp)
    }
}
