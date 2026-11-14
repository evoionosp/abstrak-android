package org.evoionosp.abstrak.presentation.settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSpaceBarTest {
    @Test
    fun `segments are sized by their share with a gap between neighbours`() {
        val segments = barSegments(listOf(25L, 25L, 50L), width = 108f, gap = 4f, minWidth = 0f)
        assertEquals(listOf(BarSegment(0f, 25f), BarSegment(29f, 54f), BarSegment(58f, 108f)), segments)
    }

    @Test
    fun `a value of zero gets no segment and no gap`() {
        val segments = barSegments(listOf(60L, 0L, 40L), width = 104f, gap = 4f, minWidth = 0f)
        assertEquals(listOf(BarSegment(0f, 60f), null, BarSegment(64f, 104f)), segments)
    }

    @Test
    fun `a sliver is widened to the minimum and its neighbour gives up the difference`() {
        val (wide, sliver) = barSegments(listOf(999L, 1L), width = 104f, gap = 4f, minWidth = 8f).map { it!! }
        assertEquals(0f, wide.start, 0f)
        assertEquals(92f, wide.end, TOLERANCE)
        assertEquals(96f, sliver.start, TOLERANCE)
        assertEquals(104f, sliver.end, 0f)
    }

    @Test
    fun `wider segments give up the difference in proportion and keep their order`() {
        val (big, sliver, small) =
            barSegments(listOf(700L, 1L, 299L), width = 108f, gap = 4f, minWidth = 10f).map { it!! }
        assertEquals(10f, sliver.width, TOLERANCE)
        assertEquals(60f / 19.9f, (big.width - 10f) / (small.width - 10f), TOLERANCE)
        assertTrue(big.width > small.width)
        assertEquals(108f, small.end, 0f)
    }

    @Test
    fun `the minimum is capped at an equal share so every segment fits`() {
        val segments =
            barSegments(listOf(5_000L, 5_000L, 1L), width = 108f, gap = 4f, minWidth = 50f).map { it!! }
        segments.forEach { assertEquals(100f / 3, it.width, TOLERANCE) }
        assertEquals(108f, segments.last().end, 0f)
    }

    @Test
    fun `a lone value fills the bar`() {
        val segments = barSegments(listOf(0L, 0L, 7L), width = 100f, gap = 4f, minWidth = 8f)
        assertEquals(listOf(null, null, BarSegment(0f, 100f)), segments)
    }

    @Test
    fun `nothing is drawn when every value is zero or the gaps leave no room`() {
        assertEquals(listOf(null, null, null), barSegments(listOf(0L, 0L, 0L), width = 100f, gap = 4f, minWidth = 8f))
        assertEquals(listOf(null, null, null), barSegments(listOf(1L, 1L, 1L), width = 8f, gap = 4f, minWidth = 8f))
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
