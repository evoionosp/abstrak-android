package org.evoionosp.abstrak.presentation.player.overlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetMorphTest {
    private val metrics =
        SheetMorphMetrics(
            maxHeightPx = 2_000f,
            miniHeightPx = 160f,
            bottomInsetPx = 240f,
            miniSidePaddingPx = 20f,
            miniCornerRadiusPx = 40f,
            miniShadowElevationPx = 20f,
            parallaxDistancePx = 70f,
        )

    @Test
    fun `collapsed card is the mini card above the bottom inset`() {
        val geometry = sheetMorphGeometry(0f, metrics)
        assertEquals(1_600f, geometry.cardTopPx, 0.01f)
        assertEquals(160f, geometry.cardHeightPx, 0.01f)
        assertEquals(20f, geometry.sidePaddingPx, 0.01f)
        assertEquals(40f, geometry.cornerRadiusPx, 0.01f)
        assertEquals(20f, geometry.shadowElevationPx, 0.01f)
        assertEquals(1f, geometry.outlineAlpha, 0.001f)
        assertEquals(1f, geometry.miniAlpha, 0.001f)
        assertEquals(0f, geometry.fullAlpha, 0.001f)
        assertEquals(70f, geometry.parallaxPx, 0.01f)
    }

    @Test
    fun `expanded card is the flat full screen`() {
        val geometry = sheetMorphGeometry(1f, metrics)
        assertEquals(0f, geometry.cardTopPx, 0.01f)
        assertEquals(2_000f, geometry.cardHeightPx, 0.01f)
        assertEquals(0f, geometry.sidePaddingPx, 0.01f)
        assertEquals(0f, geometry.cornerRadiusPx, 0.01f)
        assertEquals(0f, geometry.shadowElevationPx, 0.01f)
        assertEquals(0f, geometry.outlineAlpha, 0.001f)
        assertEquals(0f, geometry.miniAlpha, 0.001f)
        assertEquals(1f, geometry.fullAlpha, 0.001f)
        assertEquals(0f, geometry.parallaxPx, 0.01f)
    }

    @Test
    fun `the outline fades out with the corners as the card becomes the full player`() {
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val geometry = sheetMorphGeometry(fraction, metrics)
            assertEquals(geometry.cornerRadiusPx / metrics.miniCornerRadiusPx, geometry.outlineAlpha, 0.001f)
        }
    }

    @Test
    fun `mini and full content never show at once`() {
        listOf(0.15f, 0.18f, 0.2f, 0.22f).forEach { fraction ->
            val geometry = sheetMorphGeometry(fraction, metrics)
            assertEquals(0f, geometry.miniAlpha, 0.001f)
            assertEquals(0f, geometry.fullAlpha, 0.001f)
        }
        assertTrue(sheetMorphGeometry(0.1f, metrics).miniAlpha > 0f)
        assertTrue(sheetMorphGeometry(0.3f, metrics).fullAlpha > 0f)
    }

    @Test
    fun `card grows steadily from mini to full`() {
        val heights = (0..10).map { sheetMorphGeometry(it / 10f, metrics).cardHeightPx }
        heights.zipWithNext().forEach { (smaller, larger) -> assertTrue(larger >= smaller) }
    }

    @Test
    fun `card top moves linearly so it tracks the finger`() {
        assertEquals(1_600f, metrics.collapsedTopPx, 0.01f)
        assertEquals(800f, sheetMorphGeometry(0.5f, metrics).cardTopPx, 0.01f)
        assertEquals(400f, sheetMorphGeometry(0.75f, metrics).cardTopPx, 0.01f)
    }

    @Test
    fun `card never starts above the screen when the mini outgrows it`() {
        val cramped = metrics.copy(maxHeightPx = 300f)
        assertEquals(0f, sheetMorphGeometry(0f, cramped).cardTopPx, 0.01f)
    }
}
