package org.evoionosp.abstrak.data.library.local.db
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Settles library syncs that the previous process left running. A sync cannot outlive the
 * process that ran it, so one still marked as syncing when the database opens was cut short. It
 * becomes a failed sync (stale while the library has cached books, failed when it has none) and
 * keeps its last sync time. Left alone, the library would show as syncing until its next sync,
 * which can be a day away.
 */
internal object InterruptedSyncReset : RoomDatabase.Callback() {
    internal const val SQL =
        "UPDATE library_sync_state SET error = 'UNKNOWN', status = CASE " +
            "WHEN EXISTS (SELECT 1 FROM audiobooks WHERE audiobooks.libraryId = library_sync_state.libraryId) " +
            "THEN 'STALE' ELSE 'FAILED' END " +
            "WHERE status = 'SYNCING'"

    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL(SQL)
    }
}
