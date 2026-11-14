package org.evoionosp.abstrak.presentation.player.transport
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
class PlayPauseButtonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var isPlaying by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)

    @Test
    fun `paused corner is half the button size at every size`() {
        listOf(44.dp, 72.dp, 96.dp).forEach { size ->
            assertEquals(
                size.value / 2f,
                playPauseCorner(isPlaying = false, buttonSize = size).value,
                0.001f,
            )
        }
    }

    @Test
    fun `playing corner is a squircle smaller than the paused circle`() {
        listOf(44.dp, 72.dp, 96.dp).forEach { size ->
            val playing = playPauseCorner(isPlaying = true, buttonSize = size).value
            val paused = playPauseCorner(isPlaying = false, buttonSize = size).value
            assertTrue(playing > 0f)
            assertTrue(playing < paused)
        }
    }

    @Test
    fun `loading shows over play and pause alike`() {
        assertEquals(PlayPauseGlyph.LOADING, playPauseGlyph(isPlaying = false, isLoading = true))
        assertEquals(PlayPauseGlyph.LOADING, playPauseGlyph(isPlaying = true, isLoading = true))
        assertEquals(PlayPauseGlyph.PAUSE, playPauseGlyph(isPlaying = true, isLoading = false))
        assertEquals(PlayPauseGlyph.PLAY, playPauseGlyph(isPlaying = false, isLoading = false))
    }

    @Test
    fun `while loading the button spins in place of its glyph and stays tappable`() {
        var clicks = 0
        composeRule.setContent {
            PlayPauseButton(isPlaying = isPlaying, isLoading = isLoading, onClick = { clicks++ })
        }
        val loading = composeRule.activity.getString(R.string.now_playing_loading)
        val play = composeRule.activity.getString(R.string.now_playing_play)
        val pause = composeRule.activity.getString(R.string.now_playing_pause)
        isLoading = true
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(loading).assertExists()
        composeRule.onNodeWithContentDescription(play).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(loading).performClick()
        assertEquals(1, clicks)
        isLoading = false
        isPlaying = true
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(pause).assertExists()
        composeRule.onNodeWithContentDescription(loading).assertDoesNotExist()
    }
}
