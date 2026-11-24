package org.evoionosp.abstrak.data.library.local.db
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.abstrak.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.abstrak.data.test.TestDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterruptedSyncResetTest {
    private val database = TestDatabase.create()
    private val syncStateDao = database.librarySyncStateDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a sync left running settles as failed, stale when the library has books, keeping its time`() =
        runTest {
            database.audiobookDao().upsertAudiobooks(listOf(book(libraryId = "with-books")))
            syncStateDao.upsert(state("with-books", status = "SYNCING", lastSyncedAtMillis = 5L))
            syncStateDao.upsert(state("empty", status = "SYNCING", lastSyncedAtMillis = null))
            syncStateDao.upsert(state("done", status = "SUCCESS", lastSyncedAtMillis = 9L))

            InterruptedSyncReset.onOpen(database.openHelper.writableDatabase)

            assertEquals(state("with-books", "STALE", 5L, error = "UNKNOWN"), syncStateDao.getSyncState("with-books"))
            assertEquals(state("empty", "FAILED", null, error = "UNKNOWN"), syncStateDao.getSyncState("empty"))
            assertEquals(state("done", "SUCCESS", 9L), syncStateDao.getSyncState("done"))
        }

    private fun state(
        libraryId: String,
        status: String,
        lastSyncedAtMillis: Long?,
        error: String? = null,
    ) = LibrarySyncStateEntity(libraryId, status, lastSyncedAtMillis, error)

    private fun book(libraryId: String) =
        AudiobookEntity(
            id = "book-1",
            libraryId = libraryId,
            title = "Dune",
            author = "Frank Herbert",
            coverUrl = "https://host/cover",
            series = null,
            durationInSeconds = 10L,
        )
}
