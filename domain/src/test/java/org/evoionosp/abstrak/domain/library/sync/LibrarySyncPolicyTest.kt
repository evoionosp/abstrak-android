package org.evoionosp.abstrak.domain.library.sync
import org.evoionosp.abstrak.domain.library.sync.LibrarySyncPolicy.INTERVAL_MILLIS
import org.evoionosp.abstrak.domain.library.sync.LibrarySyncPolicy.OUTDATED_AFTER_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySyncPolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `a library that never synced is due`() {
        assertTrue(LibrarySyncPolicy.isDue(lastSyncedAtMillis = null, nowMillis = now))
    }

    @Test
    fun `a library is due again a day after its last sync, not before`() {
        assertFalse(LibrarySyncPolicy.isDue(now, now))
        assertFalse(LibrarySyncPolicy.isDue(now - INTERVAL_MILLIS + 1, now))
        assertTrue(LibrarySyncPolicy.isDue(now - INTERVAL_MILLIS, now))
    }

    @Test
    fun `a last sync after now, from a clock moved back, makes the library due`() {
        assertTrue(LibrarySyncPolicy.isDue(now + 1, now))
    }

    @Test
    fun `freshness is recent within the day, aging up to two days, outdated after`() {
        assertEquals(SyncFreshness.RECENT, LibrarySyncPolicy.freshness(now, now))
        assertEquals(SyncFreshness.RECENT, LibrarySyncPolicy.freshness(now - INTERVAL_MILLIS + 1, now))
        assertEquals(SyncFreshness.AGING, LibrarySyncPolicy.freshness(now - INTERVAL_MILLIS, now))
        assertEquals(SyncFreshness.AGING, LibrarySyncPolicy.freshness(now - OUTDATED_AFTER_MILLIS, now))
        assertEquals(SyncFreshness.OUTDATED, LibrarySyncPolicy.freshness(now - OUTDATED_AFTER_MILLIS - 1, now))
    }

    @Test
    fun `a library that never synced is outdated, and a future last sync counts as recent`() {
        assertEquals(SyncFreshness.OUTDATED, LibrarySyncPolicy.freshness(null, now))
        assertEquals(SyncFreshness.RECENT, LibrarySyncPolicy.freshness(now + 60_000, now))
    }
}
