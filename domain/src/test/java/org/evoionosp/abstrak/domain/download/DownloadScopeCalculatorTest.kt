package org.evoionosp.abstrak.domain.download
import org.evoionosp.abstrak.domain.testing.testChapter
import org.evoionosp.abstrak.domain.testing.testTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadScopeCalculatorTest {
    private val calculator = DownloadScopeCalculator()
    private val chapters =
        listOf(
            testChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 100),
            testChapter(title = "Ch 2", startInSeconds = 100, endInSeconds = 200),
            testChapter(title = "Ch 3", startInSeconds = 200, endInSeconds = 300),
        )
    private val tracks =
        listOf(
            testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 60),
            testTrack(index = 1, startOffsetInSeconds = 60, durationInSeconds = 60),
            testTrack(index = 2, startOffsetInSeconds = 120, durationInSeconds = 60),
            testTrack(index = 3, startOffsetInSeconds = 180, durationInSeconds = 60),
            testTrack(index = 4, startOffsetInSeconds = 240, durationInSeconds = 60),
        )

    @Test
    fun `totalDurationSeconds prefers the declared duration`() {
        assertEquals(
            600.0,
            calculator.totalDurationSeconds(
                tracks = listOf(testTrack(durationInSeconds = 300)),
                declaredDurationSeconds = 600,
            ),
        )
    }

    @Test
    fun `totalDurationSeconds falls back to the track span`() {
        val spanned =
            listOf(
                testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 300),
                testTrack(index = 1, startOffsetInSeconds = 300, durationInSeconds = 100),
            )
        assertEquals(400.0, calculator.totalDurationSeconds(tracks = spanned, declaredDurationSeconds = null))
    }

    @Test
    fun `totalDurationSeconds ignores a zero declared duration`() {
        assertEquals(
            300.0,
            calculator.totalDurationSeconds(
                tracks = listOf(testTrack(durationInSeconds = 300)),
                declaredDurationSeconds = 0,
            ),
        )
    }

    @Test
    fun `totalDurationSeconds is null when nothing is known`() {
        assertNull(calculator.totalDurationSeconds(tracks = emptyList(), declaredDurationSeconds = null))
    }

    @Test
    fun `book download with unknown position covers every track`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = null,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `finished book downloads everything`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = 299.0,
                isFinished = true,
            ),
        )
    }

    @Test
    fun `book download starts at the current chapter`() {
        assertEquals(
            listOf(1, 2, 3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = 150.0,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `book download in the last chapter covers its tail only`() {
        assertEquals(
            listOf(3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = 250.0,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `book download in the first chapter covers everything`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = 50.0,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `book download excludes completed tracks`() {
        assertEquals(
            listOf(3, 4),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                positionSeconds = 150.0,
                isFinished = false,
                completedTrackIndexes = setOf(1, 2),
            ),
        )
    }

    @Test
    fun `book download without chapters covers every track`() {
        assertEquals(
            listOf(0, 4),
            calculator.bookTrackIndexes(
                chapters = emptyList(),
                tracks = tracks,
                positionSeconds = 150.0,
                isFinished = false,
                completedTrackIndexes = setOf(1, 2, 3),
            ),
        )
    }

    @Test
    fun `book download with no tracks is empty`() {
        assertEquals(
            emptyList<Int>(),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = emptyList(),
                positionSeconds = null,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `position before the first chapter downloads everything`() {
        val lateChapters = listOf(testChapter(title = "Ch 1", startInSeconds = 100, endInSeconds = 200))
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            calculator.bookTrackIndexes(
                chapters = lateChapters,
                tracks = tracks,
                positionSeconds = 10.0,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `book scope places zero-duration tracks by start point`() {
        val withUnknown =
            tracks +
                testTrack(index = 7, startOffsetInSeconds = 150, durationInSeconds = 0) +
                testTrack(index = 8, startOffsetInSeconds = 50, durationInSeconds = 0)
        assertEquals(
            listOf(1, 2, 3, 4, 7),
            calculator.bookTrackIndexes(
                chapters = chapters,
                tracks = withUnknown,
                positionSeconds = 150.0,
                isFinished = false,
            ),
        )
    }

    @Test
    fun `chapter download covers only overlapping tracks`() {
        assertEquals(
            listOf(0, 1),
            calculator.chapterTrackIndexes(
                chapterIndex = 0,
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
            ),
        )
    }

    @Test
    fun `chapter download respects an explicit end`() {
        val short = listOf(testChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 50))
        assertEquals(
            listOf(0),
            calculator.chapterTrackIndexes(
                chapterIndex = 0,
                chapters = short,
                tracks = tracks,
                totalDurationSeconds = 300.0,
            ),
        )
    }

    @Test
    fun `chapter download of a single-track book fetches the whole file`() {
        assertEquals(
            listOf(0),
            calculator.chapterTrackIndexes(
                chapterIndex = 1,
                chapters = chapters,
                tracks = listOf(testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 300)),
                totalDurationSeconds = 300.0,
            ),
        )
    }

    @Test
    fun `unknown chapter index yields nothing`() {
        assertEquals(
            emptyList<Int>(),
            calculator.chapterTrackIndexes(
                chapterIndex = 9,
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
            ),
        )
    }

    @Test
    fun `chapter download skips completed tracks`() {
        assertEquals(
            listOf(1),
            calculator.chapterTrackIndexes(
                chapterIndex = 0,
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                completedTrackIndexes = setOf(0),
            ),
        )
        assertEquals(
            emptyList<Int>(),
            calculator.chapterTrackIndexes(
                chapterIndex = 0,
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                completedTrackIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun `last chapter without end uses the book duration`() {
        val openEnded =
            listOf(
                testChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 100),
                testChapter(title = "Ch 2", startInSeconds = 100, endInSeconds = null),
            )
        assertEquals(
            listOf(1, 2, 3, 4),
            calculator.chapterTrackIndexes(
                chapterIndex = 1,
                chapters = openEnded,
                tracks = tracks,
                totalDurationSeconds = 300.0,
            ),
        )
    }

    @Test
    fun `last chapter without end or duration is unbounded`() {
        val openEnded = listOf(testChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = null))
        val farFlung =
            listOf(
                testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 60),
                testTrack(index = 9, startOffsetInSeconds = 10_000, durationInSeconds = 60),
            )
        assertEquals(
            listOf(0, 9),
            calculator.chapterTrackIndexes(
                chapterIndex = 0,
                chapters = openEnded,
                tracks = farFlung,
                totalDurationSeconds = null,
            ),
        )
    }

    @Test
    fun `no finished chapters delete nothing`() {
        assertEquals(
            emptyList<Int>(),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = emptySet(),
                completedTrackIndexes = setOf(0, 1, 2, 3, 4),
            ),
        )
    }

    @Test
    fun `finished chapters keep tracks shared with an unfinished chapter`() {
        assertEquals(
            listOf(0),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0),
                completedTrackIndexes = setOf(0, 1, 2),
            ),
        )
        assertEquals(
            listOf(0, 1, 2),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0, 1),
                completedTrackIndexes = setOf(0, 1, 2, 3, 4),
            ),
        )
    }

    @Test
    fun `skipped chapters keep their tracks`() {
        assertEquals(
            listOf(2),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(1),
                completedTrackIndexes = setOf(0, 1, 2, 3, 4),
            ),
        )
    }

    @Test
    fun `finished chapters delete only completed tracks`() {
        assertEquals(
            listOf(0),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0, 1),
                completedTrackIndexes = setOf(0),
            ),
        )
    }

    @Test
    fun `finished chapters with no chapter data or an unknown chapter delete nothing`() {
        assertEquals(
            emptyList<Int>(),
            calculator.finishedChapterTrackIndexes(
                chapters = emptyList(),
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0),
                completedTrackIndexes = setOf(0, 1),
            ),
        )
        assertEquals(
            emptyList<Int>(),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = tracks,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(9),
                completedTrackIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun `finished chapters keep a single track spanning the whole book until every chapter is heard`() {
        val wholeBook = listOf(testTrack(index = 0, startOffsetInSeconds = 0, durationInSeconds = 300))
        assertEquals(
            emptyList<Int>(),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = wholeBook,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0, 1),
                completedTrackIndexes = setOf(0),
            ),
        )
        assertEquals(
            listOf(0),
            calculator.finishedChapterTrackIndexes(
                chapters = chapters,
                tracks = wholeBook,
                totalDurationSeconds = 300.0,
                finishedChapterIndexes = setOf(0, 1, 2),
                completedTrackIndexes = setOf(0),
            ),
        )
    }

    @Test
    fun `zero-duration tracks match by start point within chapter bounds`() {
        val pointed = listOf(testTrack(index = 7, startOffsetInSeconds = 200, durationInSeconds = 0))
        assertEquals(
            emptyList<Int>(),
            calculator.chapterTrackIndexes(
                chapterIndex = 1,
                chapters = chapters,
                tracks = pointed,
                totalDurationSeconds = 300.0,
            ),
        )
        assertEquals(
            listOf(7),
            calculator.chapterTrackIndexes(
                chapterIndex = 2,
                chapters = chapters,
                tracks = pointed,
                totalDurationSeconds = 300.0,
            ),
        )
    }
}
