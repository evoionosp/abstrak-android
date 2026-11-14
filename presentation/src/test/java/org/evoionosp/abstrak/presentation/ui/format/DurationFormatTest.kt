package org.evoionosp.abstrak.presentation.ui.format
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun `durations under an hour render minutes and seconds`() {
        assertEquals("0:00", 0L.toDurationLabel())
        assertEquals("0:59", 59L.toDurationLabel())
        assertEquals("1:05", 65L.toDurationLabel())
        assertEquals("59:59", 3_599L.toDurationLabel())
    }

    @Test
    fun `durations from an hour up render hours too`() {
        assertEquals("1:00:00", 3_600L.toDurationLabel())
        assertEquals("10:02:03", 36_123L.toDurationLabel())
    }

    @Test
    fun `milliseconds truncate to the whole second`() {
        assertEquals("1:05", 65_999L.msToDurationLabel())
        assertEquals("0:00", 999L.msToDurationLabel())
    }
}
