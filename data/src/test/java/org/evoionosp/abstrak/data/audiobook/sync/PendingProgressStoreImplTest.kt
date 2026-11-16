package org.evoionosp.abstrak.data.audiobook.sync
import android.database.sqlite.SQLiteFullException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.audiobook.local.dao.PendingProgressDao
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingProgressStoreImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AbstrakDatabase
    private lateinit var store: PendingProgressStoreImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        store = PendingProgressStoreImpl(database.pendingProgressDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `queues progress for a book`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            val queued = store.get("book-1")!!
            assertEquals("lib-1", queued.libraryId)
            assertEquals(at(60.0, savedAt = 1_000L), queued.progress)
            assertEquals(listOf(queued), store.getAll())
        }

    @Test
    fun `keeps the time the progress was saved, not the time it was queued`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            assertEquals(1_000L, store.get("book-1")!!.progress.lastUpdateMillis)
        }

    @Test
    fun `dates progress that comes without a time`() =
        runTest(testDispatcher) {
            val before = System.currentTimeMillis()
            store.put("lib-1", "book-1", at(60.0))
            val savedAt = store.get("book-1")!!.progress.lastUpdateMillis
            assertTrue(savedAt in before..System.currentTimeMillis())
        }

    @Test
    fun `a newer save replaces the book's queued progress`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            store.put("lib-1", "book-1", at(75.0, savedAt = 2_000L))
            assertEquals(listOf(at(75.0, savedAt = 2_000L)), store.getAll().map { it.progress })
        }

    @Test
    fun `keeps books apart`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            store.put("lib-1", "book-2", at(30.0, savedAt = 2_000L))
            assertEquals(setOf("book-1", "book-2"), store.getAll().map { it.audiobookId }.toSet())
            assertNull(store.get("book-3"))
        }

    @Test
    fun `removes the progress it read`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            assertTrue(store.remove(store.get("book-1")!!))
            assertNull(store.get("book-1"))
        }

    @Test
    fun `keeps a save made after the progress was read`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0, savedAt = 1_000L))
            val read = store.get("book-1")!!
            store.put("lib-1", "book-1", at(75.0, savedAt = 2_000L))
            assertFalse(store.remove(read))
            assertEquals(at(75.0, savedAt = 2_000L), store.get("book-1")!!.progress)
        }

    @Test
    fun `a failing database loses the save instead of throwing`() =
        runTest(testDispatcher) {
            val fullDisk = mockk<PendingProgressDao>()
            coEvery { fullDisk.upsert(any()) } throws SQLiteFullException("disk full")
            coEvery { fullDisk.getAll() } throws SQLiteFullException("disk full")
            val failing = PendingProgressStoreImpl(fullDisk, testDispatcher)
            failing.put("lib-1", "book-1", at(60.0))
            assertTrue(failing.getAll().isEmpty())
        }

    private fun at(
        seconds: Double,
        savedAt: Long = 0L,
    ) = PlaybackProgress(seconds, durationSeconds = 300.0, isFinished = false, lastUpdateMillis = savedAt)
}
