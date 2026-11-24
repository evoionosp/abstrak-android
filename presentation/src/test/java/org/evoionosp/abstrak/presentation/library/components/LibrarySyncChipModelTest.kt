package org.evoionosp.abstrak.presentation.library.components
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySyncChipModelTest {
    @Test
    fun `a running sync shows as syncing`() {
        assertEquals(
            LibrarySyncChipModel(LibrarySyncTone.SYNCING, LibrarySyncLabel.Syncing),
            librarySyncChipModel(SyncStatus.Syncing(lastSyncedAtMillis = NOW - 3 * DAY), NOW),
        )
    }

    @Test
    fun `within a day of the last sync it is recent and counts minutes, then hours`() {
        assertEquals(model(LibrarySyncTone.RECENT, LibrarySyncLabel.JustNow), at(NOW - 59_000))
        assertEquals(model(LibrarySyncTone.RECENT, LibrarySyncLabel.MinutesAgo(1)), at(NOW - MINUTE))
        assertEquals(model(LibrarySyncTone.RECENT, LibrarySyncLabel.MinutesAgo(59)), at(NOW - HOUR + 1))
        assertEquals(model(LibrarySyncTone.RECENT, LibrarySyncLabel.HoursAgo(1)), at(NOW - HOUR))
        assertEquals(model(LibrarySyncTone.RECENT, LibrarySyncLabel.HoursAgo(23)), at(NOW - DAY + 1))
    }

    @Test
    fun `from a day to two days it is aging, counted in days`() {
        assertEquals(model(LibrarySyncTone.AGING, LibrarySyncLabel.DaysAgo(1)), at(NOW - DAY))
        assertEquals(model(LibrarySyncTone.AGING, LibrarySyncLabel.DaysAgo(2)), at(NOW - 2 * DAY))
    }

    @Test
    fun `after two days, or never synced, it is outdated`() {
        assertEquals(model(LibrarySyncTone.OUTDATED, LibrarySyncLabel.DaysAgo(2)), at(NOW - 2 * DAY - 1))
        assertEquals(
            model(LibrarySyncTone.OUTDATED, LibrarySyncLabel.Never),
            librarySyncChipModel(SyncStatus.Idle, NOW),
        )
        assertEquals(
            model(LibrarySyncTone.OUTDATED, LibrarySyncLabel.Never),
            librarySyncChipModel(SyncStatus.Failed(CatalogError.NETWORK), NOW),
        )
    }

    @Test
    fun `a failed sync still reads by the last successful one`() {
        assertEquals(
            model(LibrarySyncTone.RECENT, LibrarySyncLabel.HoursAgo(2)),
            librarySyncChipModel(SyncStatus.Stale(NOW - 2 * HOUR, CatalogError.CONNECTIVITY_UNAVAILABLE), NOW),
        )
    }

    private fun at(lastSyncedAtMillis: Long) = librarySyncChipModel(SyncStatus.Success(lastSyncedAtMillis), NOW)

    private fun model(
        tone: LibrarySyncTone,
        label: LibrarySyncLabel,
    ) = LibrarySyncChipModel(tone, label)

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
