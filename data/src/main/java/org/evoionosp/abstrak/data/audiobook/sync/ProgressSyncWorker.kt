package org.evoionosp.abstrak.data.audiobook.sync
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.domain.audiobook.usecase.ProgressSyncResult
import org.evoionosp.abstrak.domain.audiobook.usecase.SyncPendingProgressUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProgressSyncWorkerEntryPoint {
    fun syncPendingProgress(): SyncPendingProgressUseCase
}

class ProgressSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sync =
            EntryPointAccessors
                .fromApplication(applicationContext, ProgressSyncWorkerEntryPoint::class.java)
                .syncPendingProgress()
        return when (sync()) {
            ProgressSyncResult.DONE, ProgressSyncResult.SIGNED_OUT -> Result.success()
            ProgressSyncResult.RETRY -> Result.retry()
        }
    }
}
