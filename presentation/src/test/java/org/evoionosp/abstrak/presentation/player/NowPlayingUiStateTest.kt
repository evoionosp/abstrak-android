package org.evoionosp.abstrak.presentation.player
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.playback.PlaybackState
import org.evoionosp.abstrak.playback.SleepTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingUiStateTest {
    private val chapters =
        listOf(
            AudiobookChapter(title = "A", startInSeconds = 10, endInSeconds = 100),
            AudiobookChapter(title = "B", startInSeconds = 100, endInSeconds = 200),
        )

    private fun state(
        positionSeconds: Double,
        totalSeconds: Double = 200.0,
        chapters: List<AudiobookChapter> = this.chapters,
        playback: PlaybackState = PlaybackState(currentBookPositionSeconds = positionSeconds),
    ): NowPlayingUiState =
        nowPlayingUiState(
            playback = playback,
            totalSeconds = totalSeconds,
            chapters = chapters,
            positionCalculator = PlaybackPositionCalculator(),
        )

    @Test
    fun `the player is loading while the book prepares or buffers`() {
        assertFalse(state(positionSeconds = 0.0).isLoading)
        assertTrue(state(positionSeconds = 0.0, playback = PlaybackState(isPreparing = true)).isLoading)
        assertTrue(state(positionSeconds = 0.0, playback = PlaybackState(isBuffering = true)).isLoading)
    }

    @Test
    fun `book progress is the live position over the total`() {
        assertEquals(0.25f, state(positionSeconds = 50.0).bookProgress, 0.001f)
        assertEquals(1f, state(positionSeconds = 250.0).bookProgress, 0.001f)
        assertEquals(0f, state(positionSeconds = 50.0, totalSeconds = 0.0).bookProgress, 0.001f)
    }

    @Test
    fun `chapter follows the live position`() {
        val state = state(positionSeconds = 150.0)
        assertEquals(1, state.currentChapterIndex)
        assertEquals("B", state.chapterTitle)
    }

    @Test
    fun `chapter is unknown before the first chapter starts`() {
        val state = state(positionSeconds = 5.0)
        assertEquals(-1, state.currentChapterIndex)
        assertNull(state.chapterTitle)
    }

    @Test
    fun `chapter time left runs to the chapter end at the current speed`() {
        assertEquals(50_000L, state(positionSeconds = 150.0).chapterTimeLeftMs)
        val fast = PlaybackState(currentBookPositionSeconds = 150.0, playbackSpeed = 2f)
        assertEquals(25_000L, state(positionSeconds = 150.0, playback = fast).chapterTimeLeftMs)
        assertNull(state(positionSeconds = 200.0).chapterTimeLeftMs)
    }

    @Test
    fun `end of chapter needs a boundary ahead`() {
        assertTrue(state(positionSeconds = 150.0).endOfChapterAvailable)
        assertFalse(state(positionSeconds = 50.0, totalSeconds = 0.0, chapters = emptyList()).endOfChapterAvailable)
        assertFalse(state(positionSeconds = 200.0).endOfChapterAvailable)
    }

    @Test
    fun `sleep footer reflects the armed timer`() {
        val armed =
            state(
                positionSeconds = 0.0,
                playback =
                    PlaybackState(
                        sleepTimer = SleepTimerState.Timer(endsAtMs = 1_801_000L),
                        sleepRemainingMs = 1_800_000L,
                    ),
            )
        assertEquals(SleepFooter.Minutes(30), armed.sleepFooter)
        assertEquals(SleepFooter.Off, state(positionSeconds = 0.0).sleepFooter)
    }

    @Test
    fun `sleep footer rounds remaining minutes up`() {
        assertEquals(SleepFooter.Off, sleepFooter(SleepTimerState.Off, null))
        assertEquals(SleepFooter.EndOfChapter, sleepFooter(SleepTimerState.EndOfChapter(50.0), null))
        assertEquals(SleepFooter.Minutes(30), sleepFooter(SleepTimerState.Timer(endsAtMs = 1_801_000L), 1_800_000L))
        assertEquals(SleepFooter.Minutes(2), sleepFooter(SleepTimerState.Timer(endsAtMs = 91_000L), 90_000L))
        assertEquals(SleepFooter.Minutes(1), sleepFooter(SleepTimerState.Timer(endsAtMs = 500L), 0L))
        assertEquals(SleepFooter.Minutes(1), sleepFooter(SleepTimerState.Timer(endsAtMs = 500L), null))
    }
}
