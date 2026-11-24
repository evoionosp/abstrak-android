package org.evoionosp.abstrak.domain.audiobook.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.testing.FakePendingProgressStore
import org.evoionosp.abstrak.domain.testing.FakeProgressSyncScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulePendingProgressSyncUseCaseTest {
    private val store = FakePendingProgressStore()
    private val scheduler = FakeProgressSyncScheduler()

    @Test
    fun `asks for a sync when progress is queued`() =
        runTest {
            store.put("lib-1", "book-1", PlaybackProgress(60.0, durationSeconds = 300.0, isFinished = false))
            SchedulePendingProgressSyncUseCase(store, scheduler)()
            assertEquals(1, scheduler.scheduleCount)
        }

    @Test
    fun `asks for nothing when nothing is queued`() =
        runTest {
            SchedulePendingProgressSyncUseCase(store, scheduler)()
            SchedulePendingProgressSyncUseCase(store, scheduler)(immediately = true)
            assertEquals(0, scheduler.scheduleCount)
            assertEquals(0, scheduler.scheduleNowCount)
        }

    @Test
    fun `an immediate sync replaces the pending one instead of keeping it`() =
        runTest {
            store.put("lib-1", "book-1", PlaybackProgress(60.0, durationSeconds = 300.0, isFinished = false))
            SchedulePendingProgressSyncUseCase(store, scheduler)(immediately = true)
            assertEquals(1, scheduler.scheduleNowCount)
            assertEquals(0, scheduler.scheduleCount)
        }
}
