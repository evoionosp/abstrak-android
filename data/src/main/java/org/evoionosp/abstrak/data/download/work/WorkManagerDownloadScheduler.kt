package org.evoionosp.abstrak.data.download.work
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WorkManagerDownloadScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DownloadWorkScheduler {
        private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

        override fun scheduleBook(
            audiobookId: String,
            trackIndexes: List<Int>,
            wifiOnly: Boolean,
        ) {
            if (trackIndexes.isEmpty()) return
            workManager.enqueueUniqueWork(
                uniqueName(audiobookId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                batchRequest(audiobookId, trackIndexes, wifiOnly),
            )
        }

        override suspend fun allowAnyNetwork(
            audiobookId: String,
            trackIndexes: List<Int>,
        ) {
            if (trackIndexes.isEmpty()) return
            workManager
                .getWorkInfosForUniqueWorkFlow(uniqueName(audiobookId))
                .first()
                .filter { !it.state.isFinished && it.constraints.requiredNetworkType == NetworkType.UNMETERED }
                .forEach { batch ->
                    workManager.updateWork(batchRequest(audiobookId, trackIndexes, wifiOnly = false, id = batch.id))
                }
        }

        private fun batchRequest(
            audiobookId: String,
            trackIndexes: List<Int>,
            wifiOnly: Boolean,
            id: UUID? = null,
        ): OneTimeWorkRequest {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            return OneTimeWorkRequestBuilder<TrackDownloadWorker>()
                .apply { id?.let(::setId) }
                .setInputData(
                    workDataOf(
                        TrackDownloadWorker.KEY_BOOK to audiobookId,
                        TrackDownloadWorker.KEY_TRACKS to trackIndexes.toIntArray(),
                        TrackDownloadWorker.KEY_WIFI_ONLY to wifiOnly,
                    ),
                ).setConstraints(constraints)
                .addTag(TAG_DOWNLOADS)
                .addTag(tagForBook(audiobookId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
        }

        override fun cancelBook(audiobookId: String) {
            workManager.cancelUniqueWork(uniqueName(audiobookId))
        }

        override fun cancelAll() {
            workManager.cancelAllWorkByTag(TAG_DOWNLOADS)
        }

        companion object {
            internal fun uniqueName(audiobookId: String): String = "download-$audiobookId"

            private const val TAG_DOWNLOADS = "abstrak-downloads"
            private const val BACKOFF_SECONDS = 30L

            private fun tagForBook(audiobookId: String): String = "download-book-$audiobookId"
        }
    }
