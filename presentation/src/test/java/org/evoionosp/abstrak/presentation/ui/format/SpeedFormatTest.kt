package org.evoionosp.abstrak.presentation.ui.format
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {
    @Test
    fun `whole speeds render without decimals`() {
        assertEquals("1x", formatPlaybackSpeed(1f, Locale.US))
        assertEquals("2x", formatPlaybackSpeed(2f, Locale.US))
    }

    @Test
    fun `fractional speeds keep up to two decimals`() {
        assertEquals("0.75x", formatPlaybackSpeed(0.75f, Locale.US))
        assertEquals("0.9x", formatPlaybackSpeed(0.9f, Locale.US))
        assertEquals("1.25x", formatPlaybackSpeed(1.25f, Locale.US))
        assertEquals("1.05x", formatPlaybackSpeed(1.05f, Locale.US))
    }

    @Test
    fun `drag quantization dust does not leak into the label`() {
        assertEquals("1.15x", formatPlaybackSpeed(1.1500001f, Locale.US))
    }

    @Test
    fun `labels use the locale decimal separator`() {
        assertEquals("1,25x", formatPlaybackSpeed(1.25f, Locale.GERMANY))
        assertEquals("1x", formatPlaybackSpeed(1f, Locale.GERMANY))
    }
}
