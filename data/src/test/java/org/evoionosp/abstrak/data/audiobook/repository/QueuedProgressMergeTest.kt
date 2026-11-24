package org.evoionosp.abstrak.data.audiobook.repository
import org.evoionosp.abstrak.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuedProgressMergeTest {
    @Test
    fun `the server's shelf stands when nothing is queued`() {
        val shelf = listOf(onShelf("a", 10.0), onShelf("b", 20.0))
        assertEquals(shelf, mergeQueuedProgress(shelf, queued = emptyList()))
    }

    @Test
    fun `newer queued progress takes over the book's place and time`() {
        val merged =
            mergeQueuedProgress(
                shelf = listOf(onShelf("a", 10.0, updatedAt = 100L)),
                queued = listOf(queued("a", 50.0, savedAt = 300L)),
            )
        assertEquals(listOf(onShelf("a", 50.0, updatedAt = 300L)), merged)
    }

    @Test
    fun `older queued progress keeps the server's update time`() {
        val merged =
            mergeQueuedProgress(
                shelf = listOf(onShelf("a", 10.0, updatedAt = 900L)),
                queued = listOf(queued("a", 50.0, savedAt = 300L)),
            )
        assertEquals(900L, merged.single().progressLastUpdateMillis)
    }

    @Test
    fun `older queued progress leaves the server's`() {
        val shelf = listOf(onShelf("a", 90.0))
        assertEquals(shelf, mergeQueuedProgress(shelf, listOf(queued("a", 50.0, savedAt = 50L))))
    }

    @Test
    fun `newer queued progress takes over even when it is behind the server's`() {
        val merged =
            mergeQueuedProgress(
                shelf = listOf(onShelf("a", 90.0, updatedAt = 100L)),
                queued = listOf(queued("a", 50.0, savedAt = 300L)),
            )
        assertEquals(listOf(onShelf("a", 50.0, updatedAt = 300L)), merged)
    }

    @Test
    fun `newer queued progress fills in a book whose position failed to load`() {
        val merged =
            mergeQueuedProgress(
                shelf = listOf(onShelf("a", null, updatedAt = 100L)),
                queued = listOf(queued("a", 50.0, savedAt = 300L)),
            )
        assertEquals(listOf(onShelf("a", 50.0, updatedAt = 300L)), merged)
    }

    @Test
    fun `older queued progress leaves a book whose position failed to load`() {
        val shelf = listOf(onShelf("a", null, updatedAt = 900L))
        assertEquals(shelf, mergeQueuedProgress(shelf, listOf(queued("a", 50.0, savedAt = 300L))))
    }

    @Test
    fun `an older finish keeps a book whose position failed to load on the shelf`() {
        val shelf = listOf(onShelf("a", null, updatedAt = 900L))
        assertEquals(shelf, mergeQueuedProgress(shelf, listOf(queued("a", 300.0, isFinished = true, savedAt = 300L))))
    }

    @Test
    fun `queued progress fills in a book the server gave no time or position for`() {
        val merged = mergeQueuedProgress(listOf(onShelf("a", null, updatedAt = 0L)), listOf(queued("a", 50.0)))
        assertEquals(50.0, merged.single().currentTimeSeconds!!, 0.001)
    }

    @Test
    fun `a book finished on this device leaves the shelf`() {
        val merged =
            mergeQueuedProgress(
                shelf = listOf(onShelf("a", 10.0), onShelf("b", 20.0)),
                queued = listOf(queued("a", 300.0, isFinished = true)),
            )
        assertEquals(listOf("b"), merged.map { it.audiobookId })
    }

    @Test
    fun `a book only this device has listened to joins the shelf`() {
        val merged = mergeQueuedProgress(listOf(onShelf("a", 10.0)), listOf(queued("new", 5.0)))
        assertEquals(listOf("a", "new"), merged.map { it.audiobookId })
    }

    private fun onShelf(
        id: String,
        seconds: Double?,
        updatedAt: Long = 100L,
    ) = ContinueListeningEntity(
        audiobookId = id,
        libraryId = "lib",
        progressLastUpdateMillis = updatedAt,
        currentTimeSeconds = seconds,
    )

    private fun queued(
        id: String,
        seconds: Double,
        isFinished: Boolean = false,
        savedAt: Long = 200L,
    ) = PendingProgressEntity(
        audiobookId = id,
        libraryId = "lib",
        currentTimeSeconds = seconds,
        durationSeconds = 300.0,
        isFinished = isFinished,
        savedAtMillis = savedAt,
    )
}
