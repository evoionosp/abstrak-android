package org.evoionosp.abstrak.domain.audiobook.playback
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPositionCalculatorTest {
    private val calculator = PlaybackPositionCalculator()
    private val tracks =
        listOf(
            track(index = 0, start = 0, duration = 100),
            track(index = 1, start = 100, duration = 100),
            track(index = 2, start = 200, duration = 100),
        )

    @Test
    fun `resolveSeekPosition maps absolute seconds into the containing track and offset`() {
        val position = calculator.resolveSeekPosition(absoluteSeconds = 150.0, tracks = tracks)
        assertEquals(1, position.trackIndex)
        assertEquals(50_000L, position.offsetMs)
    }

    @Test
    fun `resolveSeekPosition clamps negative positions to the first track`() {
        val position = calculator.resolveSeekPosition(absoluteSeconds = -10.0, tracks = tracks)
        assertEquals(0, position.trackIndex)
        assertEquals(0L, position.offsetMs)
    }

    @Test
    fun `resolveSeekPosition on empty tracks returns the start`() {
        val position = calculator.resolveSeekPosition(absoluteSeconds = 42.0, tracks = emptyList())
        assertEquals(0, position.trackIndex)
        assertEquals(0L, position.offsetMs)
    }

    @Test
    fun `absolutePosition adds the track start to the in-track offset`() {
        assertEquals(
            250.0,
            calculator.absolutePosition(trackIndex = 2, positionMs = 50_000, tracks = tracks),
            0.001,
        )
    }

    @Test
    fun `absolutePosition returns zero for an unset track index`() {
        assertEquals(
            0.0,
            calculator.absolutePosition(trackIndex = -1, positionMs = 5_000, tracks = tracks),
            0.001,
        )
    }

    @Test
    fun `totalDurationSeconds prefers the sum implied by tracks`() {
        val audiobook = audiobook(durationInSeconds = 999)
        assertEquals(300.0, calculator.totalDurationSeconds(audiobook, tracks), 0.001)
    }

    @Test
    fun `totalDurationSeconds falls back to the audiobook duration when there are no tracks`() {
        val audiobook = audiobook(durationInSeconds = 720)
        assertEquals(720.0, calculator.totalDurationSeconds(audiobook, emptyList()), 0.001)
    }

    @Test
    fun `chapterIndexAt returns the last chapter starting at or before the position`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 600), chapter("Three", 1200))
        assertEquals(0, calculator.chapterIndexAt(positionSeconds = 0.0, chapters = chapters))
        assertEquals(0, calculator.chapterIndexAt(positionSeconds = 599.9, chapters = chapters))
        assertEquals(1, calculator.chapterIndexAt(positionSeconds = 600.0, chapters = chapters))
        assertEquals(2, calculator.chapterIndexAt(positionSeconds = 5000.0, chapters = chapters))
    }

    @Test
    fun `chapterIndexAt is minus one without chapters or before the first starts`() {
        assertEquals(-1, calculator.chapterIndexAt(positionSeconds = 100.0, chapters = emptyList()))
        assertEquals(-1, calculator.chapterIndexAt(positionSeconds = 299.9, chapters = listOf(chapter("One", 300))))
    }

    @Test
    fun `chaptersPlayedThrough finishes the chapter playback runs off the end of`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100), chapter("Three", 200))
        assertEquals(0..0, calculator.chaptersPlayedThrough(99.5, 100.5, 1_000, 1f, chapters))
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(50.0, 51.0, 1_000, 1f, chapters))
        assertEquals(1..1, calculator.chaptersPlayedThrough(190.0, 205.0, 15_000, 1f, chapters))
    }

    @Test
    fun `chaptersPlayedThrough allows for the playback speed`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100))
        assertEquals(0..0, calculator.chaptersPlayedThrough(98.0, 102.0, 1_000, 3f, chapters))
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(98.0, 102.0, 1_000, 1f, chapters))
    }

    @Test
    fun `chaptersPlayedThrough finishes nothing on a skip or seek`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100), chapter("Three", 200))
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(10.0, 200.0, 1_000, 1f, chapters))
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(90.0, 120.0, 1_000, 1f, chapters))
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(150.0, 50.0, 1_000, 1f, chapters))
    }

    @Test
    fun `chaptersPlayedThrough finishes a chapter too short to see between looks`() {
        val chapters = listOf(chapter("One", 0), chapter("Interlude", 100), chapter("Two", 101))
        assertEquals(0..1, calculator.chaptersPlayedThrough(99.5, 101.5, 1_000, 1f, chapters))
    }

    @Test
    fun `chaptersPlayedThrough finishes nothing without chapters or before the first starts`() {
        assertEquals(IntRange.EMPTY, calculator.chaptersPlayedThrough(99.5, 100.5, 1_000, 1f, emptyList()))
        assertEquals(
            IntRange.EMPTY,
            calculator.chaptersPlayedThrough(4.5, 5.5, 1_000, 1f, listOf(chapter("One", 5), chapter("Two", 100))),
        )
    }

    @Test
    fun `nextChapterStart returns the first chapter beyond the position`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100), chapter("Three", 200))
        assertEquals(
            200.0,
            calculator.nextChapterStart(positionSeconds = 150.0, chapters = chapters),
        )
    }

    @Test
    fun `nextChapterStart returns null past the last chapter`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100))
        assertNull(calculator.nextChapterStart(positionSeconds = 150.0, chapters = chapters))
    }

    @Test
    fun `previousChapterTarget restarts the current chapter when well into it`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100), chapter("Three", 200))
        assertEquals(
            100.0,
            calculator.previousChapterTarget(positionSeconds = 130.0, chapters = chapters),
            0.001,
        )
    }

    @Test
    fun `previousChapterTarget jumps to the previous chapter near the start`() {
        val chapters = listOf(chapter("One", 0), chapter("Two", 100), chapter("Three", 200))
        assertEquals(
            100.0,
            calculator.previousChapterTarget(positionSeconds = 205.0, chapters = chapters),
            0.001,
        )
    }

    @Test
    fun `isFinished is true within the completion threshold of the end`() {
        assertTrue(calculator.isFinished(absoluteSeconds = 297.0, totalSeconds = 300.0))
        assertFalse(calculator.isFinished(absoluteSeconds = 250.0, totalSeconds = 300.0))
        assertFalse(calculator.isFinished(absoluteSeconds = 297.0, totalSeconds = null))
    }

    @Test
    fun `chapterTitleForTrack falls back to the track title without chapter metadata`() {
        val track = track(index = 0, start = 0, duration = 100, title = "Track 1")
        assertEquals("Track 1", calculator.chapterTitleForTrack(track, emptyList()))
    }

    private fun track(
        index: Int,
        start: Long,
        duration: Long,
        title: String = "Track $index",
    ) = AudiobookTrack(
        index = index,
        startOffsetInSeconds = start,
        durationInSeconds = duration,
        title = title,
        remoteUrl = "https://example.com/$index.mp3",
        mimeType = "audio/mpeg",
    )

    private fun chapter(
        title: String,
        start: Long,
    ) = AudiobookChapter(
        title = title,
        startInSeconds = start,
        endInSeconds = null,
    )

    private fun audiobook(durationInSeconds: Long?) =
        Audiobook(
            id = "book-1",
            libraryId = "lib-1",
            title = "Book",
            author = "Author",
            coverUrl = "",
            series = null,
            durationInSeconds = durationInSeconds,
        )
}
