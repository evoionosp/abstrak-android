package org.evoionosp.abstrak.data.di
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.data.audiobook.sync.PendingProgressStoreImpl
import org.evoionosp.abstrak.data.audiobook.sync.WorkManagerProgressSyncScheduler
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.audiobook.sync.ProgressSyncScheduler

@Module
@InstallIn(SingletonComponent::class)
abstract class ProgressSyncModule {
    @Binds
    abstract fun bindPendingProgressStore(impl: PendingProgressStoreImpl): PendingProgressStore

    @Binds
    abstract fun bindProgressSyncScheduler(impl: WorkManagerProgressSyncScheduler): ProgressSyncScheduler
}
