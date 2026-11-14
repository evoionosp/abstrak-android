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
        suspend operator fun invoke() {
            if (pendingProgressStore.getAll().isNotEmpty()) progressSyncScheduler.schedule()
        }
    }
