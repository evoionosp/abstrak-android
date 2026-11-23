package org.evoionosp.abstrak.data.audiobook.sync
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressSyncPolicy
import org.evoionosp.abstrak.domain.audiobook.sync.ProgressSyncScheduler

@Singleton
class WorkManagerProgressSyncScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProgressSyncScheduler {
        private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

        override fun schedule() {
            workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, syncRequest())
        }

        override fun scheduleNow() {
            workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, syncRequest())
        }

        companion object {
            internal const val UNIQUE_NAME = "progress-sync"

            internal fun syncRequest(): OneTimeWorkRequest =
                OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        ProgressSyncPolicy.INTERVAL_SECONDS.toLong(),
                        TimeUnit.SECONDS,
                    ).build()
        }
    }
