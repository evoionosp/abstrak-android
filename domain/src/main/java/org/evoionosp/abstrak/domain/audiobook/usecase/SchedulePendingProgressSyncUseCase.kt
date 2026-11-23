package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.audiobook.sync.ProgressSyncScheduler

class SchedulePendingProgressSyncUseCase
    @Inject
    constructor(
        private val pendingProgressStore: PendingProgressStore,
        private val progressSyncScheduler: ProgressSyncScheduler,
    ) {
        /** Schedules delivery of queued progress, if any; [immediately] skips a pending retry backoff. */
        suspend operator fun invoke(immediately: Boolean = false) {
            if (pendingProgressStore.getAll().isEmpty()) return
            if (immediately) progressSyncScheduler.scheduleNow() else progressSyncScheduler.schedule()
        }
    }
