package org.evoionosp.abstrak.domain.audiobook.playback
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMergeTest {
    @Test
    fun `the device is ahead of a server with no progress`() {
        assertTrue(ProgressMerge.isDeviceAhead(at(10.0), server = null))
    }

    @Test
    fun `the further position wins`() {
        assertTrue(ProgressMerge.isDeviceAhead(at(600.0), at(300.0)))
        assertFalse(ProgressMerge.isDeviceAhead(at(300.0), at(600.0)))
    }

    @Test
    fun `a tie goes to the server`() {
        assertFalse(ProgressMerge.isDeviceAhead(at(300.0), at(300.0)))
    }

    @Test
    fun `a finished book is further along than any position in it`() {
        assertTrue(ProgressMerge.isDeviceAhead(finished(), at(7_000.0)))
        assertFalse(ProgressMerge.isDeviceAhead(at(7_000.0), finished()))
    }

    @Test
    fun `a book finished on both sides is the server's`() {
        assertFalse(ProgressMerge.isDeviceAhead(finished(), finished()))
    }

    @Test
    fun `furthest picks whichever is further along`() {
        assertEquals(at(600.0), ProgressMerge.furthest(at(600.0), at(300.0)))
        assertEquals(at(600.0), ProgressMerge.furthest(at(300.0), at(600.0)))
        assertEquals(at(300.0), ProgressMerge.furthest(at(300.0), server = null))
        assertEquals(at(300.0), ProgressMerge.furthest(device = null, at(300.0)))
        assertNull(ProgressMerge.furthest(device = null, server = null))
    }

    private fun at(seconds: Double) = PlaybackProgress(seconds, durationSeconds = 7_200.0, isFinished = false)

    private fun finished() = PlaybackProgress(7_190.0, durationSeconds = 7_200.0, isFinished = true)
}
