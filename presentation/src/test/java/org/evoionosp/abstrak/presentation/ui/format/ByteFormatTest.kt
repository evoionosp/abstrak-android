package org.evoionosp.abstrak.presentation.ui.format
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatTest {
    @Test
    fun `formatStorageSize uses megabytes below a gigabyte and gigabytes from there`() {
        assertEquals("1.0 MB", formatStorageSize(MB))
        assertEquals("845.3 MB", formatStorageSize((845.3 * MB).toLong()))
        assertEquals("1023.9 MB", formatStorageSize((1023.94 * MB).toLong()))
        assertEquals("1.0 GB", formatStorageSize((1023.96 * MB).toLong()))
        assertEquals("1.0 GB", formatStorageSize(1024 * MB))
        assertEquals("1.2 GB", formatStorageSize((1.2 * 1024 * MB).toLong()))
        assertEquals("2.5 GB", formatStorageSize(2_684_354_560))
    }

    @Test
    fun `formatStorageSize reads nothing as 0 MB and any bytes as at least 0_1 MB`() {
        assertEquals("0 MB", formatStorageSize(0))
        assertEquals("0 MB", formatStorageSize(-50))
        assertEquals("0.1 MB", formatStorageSize(1))
        assertEquals("0.1 MB", formatStorageSize(40_000))
    }

    private companion object {
        const val MB = 1024L * 1024L
    }
}
