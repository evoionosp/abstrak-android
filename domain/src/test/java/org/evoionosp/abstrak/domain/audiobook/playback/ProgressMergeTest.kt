package org.evoionosp.abstrak.domain.audiobook.playback
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMergeTest {
    @Test
    fun `the device wins when the server has no progress`() {
        assertTrue(ProgressMerge.isDeviceNewer(at(300.0, lastUpdateMillis = 1L), server = null))
    }

    @Test
    fun `the more recently updated position wins, whichever side it is on`() {
        assertTrue(ProgressMerge.isDeviceNewer(at(300.0, lastUpdateMillis = 2L), at(300.0, lastUpdateMillis = 1L)))
        assertFalse(ProgressMerge.isDeviceNewer(at(300.0, lastUpdateMillis = 1L), at(300.0, lastUpdateMillis = 2L)))
    }

    @Test
    fun `a further device position loses to a more recent server one`() {
        assertFalse(ProgressMerge.isDeviceNewer(at(600.0, lastUpdateMillis = 1L), at(200.0, lastUpdateMillis = 2L)))
    }

    @Test
    fun `a tie goes to the server`() {
        assertFalse(ProgressMerge.isDeviceNewer(at(600.0, lastUpdateMillis = 1L), at(300.0, lastUpdateMillis = 1L)))
    }

    @Test
    fun `a record with no update time loses to any dated record`() {
        assertFalse(ProgressMerge.isDeviceNewer(at(600.0, lastUpdateMillis = 0L), at(300.0, lastUpdateMillis = 1L)))
        assertTrue(ProgressMerge.isDeviceNewer(at(600.0, lastUpdateMillis = 1L), at(300.0, lastUpdateMillis = 0L)))
    }

    @Test
    fun `a finish loses to a more recent server position`() {
        assertFalse(ProgressMerge.isDeviceNewer(finished(lastUpdateMillis = 1L), at(7_000.0, lastUpdateMillis = 2L)))
        assertTrue(ProgressMerge.isDeviceNewer(at(300.0, lastUpdateMillis = 2L), finished(lastUpdateMillis = 1L)))
    }

    @Test
    fun `newest picks the more recently updated side by each record's own time`() {
        val older = at(600.0, lastUpdateMillis = 1L)
        val server = at(300.0, lastUpdateMillis = 2L)
        val newer = at(600.0, lastUpdateMillis = 3L)
        assertEquals(server, ProgressMerge.newest(older, server))
        assertEquals(newer, ProgressMerge.newest(newer, server))
        assertEquals(older, ProgressMerge.newest(older, server = null))
        assertEquals(server, ProgressMerge.newest(device = null, server))
        assertNull(ProgressMerge.newest(device = null, server = null))
    }

    private fun at(
        seconds: Double,
        lastUpdateMillis: Long = 0L,
    ) = PlaybackProgress(seconds, durationSeconds = 7_200.0, isFinished = false, lastUpdateMillis = lastUpdateMillis)

    private fun finished(lastUpdateMillis: Long = 0L) =
        PlaybackProgress(7_190.0, durationSeconds = 7_200.0, isFinished = true, lastUpdateMillis = lastUpdateMillis)
}
