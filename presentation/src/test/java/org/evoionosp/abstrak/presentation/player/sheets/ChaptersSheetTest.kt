package org.evoionosp.abstrak.presentation.player.sheets
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChaptersSheetTest {
    @Test
    fun `a current chapter already in view stays put`() {
        assertNull(chapterListScrollTarget(currentIndex = 3, chapterCount = 25, fullyVisible = (0..7).toList()))
    }

    @Test
    fun `a current chapter further down scrolls up with the one before it on top`() {
        assertEquals(14, chapterListScrollTarget(currentIndex = 15, chapterCount = 25, fullyVisible = (0..7).toList()))
    }

    @Test
    fun `a current chapter cut off at the bottom edge counts as out of view`() {
        assertEquals(7, chapterListScrollTarget(currentIndex = 8, chapterCount = 25, fullyVisible = (0..7).toList()))
    }

    @Test
    fun `an unknown current chapter scrolls nowhere`() {
        assertNull(chapterListScrollTarget(currentIndex = -1, chapterCount = 25, fullyVisible = (0..7).toList()))
        assertNull(chapterListScrollTarget(currentIndex = 25, chapterCount = 25, fullyVisible = (0..7).toList()))
    }

    @Test
    fun `progress is the position into the chapter over its own length, after its start`() {
        val chapters = listOf(AudiobookChapter("One", 0, 60), AudiobookChapter("Two", 60, 150))
        assertEquals(
            "1:00 · 0:42\u202F/\u202F1:30",
            chapterProgressLabel(chapters, index = 1, positionSeconds = 102.7, totalSeconds = 150.0),
        )
    }

    @Test
    fun `a chapter without an end runs to the next chapter, the last one to the book's end`() {
        val chapters = listOf(AudiobookChapter("One", 0, null), AudiobookChapter("Two", 89, null))
        assertEquals(
            "0:00 · 0:10\u202F/\u202F1:29",
            chapterProgressLabel(chapters, index = 0, positionSeconds = 10.0, totalSeconds = 3_700.0),
        )
        assertEquals(
            "1:29 · 0:01\u202F/\u202F1:00:11",
            chapterProgressLabel(chapters, index = 1, positionSeconds = 90.0, totalSeconds = 3_700.0),
        )
    }

    @Test
    fun `progress stays inside the chapter`() {
        val chapters = listOf(AudiobookChapter("One", 60, 120))
        assertEquals(
            "1:00 · 0:00\u202F/\u202F1:00",
            chapterProgressLabel(chapters, index = 0, positionSeconds = 30.0, totalSeconds = 120.0),
        )
        assertEquals(
            "1:00 · 1:00\u202F/\u202F1:00",
            chapterProgressLabel(chapters, index = 0, positionSeconds = 500.0, totalSeconds = 120.0),
        )
    }

    @Test
    fun `no progress for an unknown chapter or one with no length`() {
        val chapters = listOf(AudiobookChapter("One", 0, null))
        assertNull(chapterProgressLabel(chapters, index = -1, positionSeconds = 10.0, totalSeconds = 100.0))
        assertNull(chapterProgressLabel(chapters, index = 0, positionSeconds = 10.0, totalSeconds = 0.0))
    }
}
