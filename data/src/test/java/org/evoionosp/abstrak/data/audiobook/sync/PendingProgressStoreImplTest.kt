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
            store.put("lib-1", "book-1", at(60.0))
            val queued = store.get("book-1")!!
            assertEquals("lib-1", queued.libraryId)
            assertEquals(at(60.0), queued.progress)
            assertEquals(listOf(queued), store.getAll())
        }

    @Test
    fun `a newer save replaces the book's queued progress`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0))
            store.put("lib-1", "book-1", at(75.0))
            assertEquals(listOf(at(75.0)), store.getAll().map { it.progress })
        }

    @Test
    fun `keeps books apart`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0))
            store.put("lib-1", "book-2", at(30.0))
            assertEquals(setOf("book-1", "book-2"), store.getAll().map { it.audiobookId }.toSet())
            assertNull(store.get("book-3"))
        }

    @Test
    fun `removes the progress it read`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0))
            assertTrue(store.remove(store.get("book-1")!!))
            assertNull(store.get("book-1"))
        }

    @Test
    fun `keeps a save made after the progress was read`() =
        runTest(testDispatcher) {
            store.put("lib-1", "book-1", at(60.0))
            val read = store.get("book-1")!!
            store.put("lib-1", "book-1", at(75.0))
            assertFalse(store.remove(read))
            assertEquals(at(75.0), store.get("book-1")!!.progress)
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

    private fun at(seconds: Double) = PlaybackProgress(seconds, durationSeconds = 300.0, isFinished = false)
}
