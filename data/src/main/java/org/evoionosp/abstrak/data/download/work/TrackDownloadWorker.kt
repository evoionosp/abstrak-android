package org.evoionosp.abstrak.data.download.work
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.data.R
import org.evoionosp.abstrak.data.connectivity.OfflineModeState
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadWorkerEntryPoint {
    fun batchDownloader(): TrackBatchDownloader

    fun trackDownloadDao(): TrackDownloadDao

    fun downloadRepository(): DownloadRepository

    fun offlineModeState(): OfflineModeState

    fun getValidSession(): GetValidSessionUseCase
}

class TrackDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private var foregroundDenied = false

    override suspend fun doWork(): Result {
        val audiobookId = inputData.getString(KEY_BOOK) ?: return Result.failure()
        val trackIndexes = inputData.getIntArray(KEY_TRACKS)?.toList().orEmpty()
        if (trackIndexes.isEmpty()) return Result.failure()
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                DownloadWorkerEntryPoint::class.java,
            )
        // Offline mode holds downloads back, the tracks staying queued until it resumes them.
        // Trying anyway would spend the retries every track gets on requests that cannot go out.
        if (entryPoint.offlineModeState().isEnabled.first()) return Result.success()
        val getValidSession = entryPoint.getValidSession()
        trySetForeground(foregroundInfo(audiobookId, doneCount = 0, totalCount = trackIndexes.size))
        val result =
            entryPoint.batchDownloader().downloadBatch(
                audiobookId = audiobookId,
                trackIndexes = trackIndexes,
                tokenProvider = { getValidSession()?.accessToken },
                runAttemptCount = runAttemptCount,
            ) { done, total ->
                trySetForeground(foregroundInfo(audiobookId, done, total))
            }
        return if (result.retryNeeded && !result.aborted) Result.retry() else Result.success()
    }

    private suspend fun trySetForeground(info: ForegroundInfo) {
        if (foregroundDenied) return
        try {
            setForeground(info)
        } catch (denied: IllegalStateException) {
            foregroundDenied = true
        }
    }

    private fun foregroundInfo(
        audiobookId: String,
        doneCount: Int,
        totalCount: Int,
    ): ForegroundInfo {
        ensureChannel()
        val context = applicationContext
        val contentIntent =
            appLaunchIntent(context)?.let { launch ->
                PendingIntent.getActivity(
                    context,
                    audiobookId.hashCode(),
                    launch,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        val cancelIntent =
            PendingIntent.getBroadcast(
                context,
                CANCEL_REQUEST_CODE_BASE + audiobookId.hashCode(),
                Intent(context, DownloadCancelReceiver::class.java).apply {
                    action = DownloadCancelReceiver.ACTION_CANCEL_DOWNLOAD
                    putExtra(DownloadCancelReceiver.EXTRA_BOOK, audiobookId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.download_notification_title))
                .setContentText(
                    context.getString(R.string.download_notification_progress, doneCount, totalCount),
                ).setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(totalCount, doneCount, false)
                .setContentIntent(contentIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.download_notification_cancel),
                    cancelIntent,
                ).build()
        val notificationId = NOTIFICATION_ID_BASE + audiobookId.hashCode()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.download_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    /**
     * Opens the app at the activity behind its launcher entry. The entry itself, which
     * getLaunchIntentForPackage names, changes with the app icon, and a notification pointing
     * at one that has since been switched off would open nothing.
     */
    private fun appLaunchIntent(context: Context): Intent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        val activity =
            context.packageManager
                .resolveActivity(launch, 0)
                ?.activityInfo
                ?.targetActivity ?: return launch
        return launch.setClassName(context, activity)
    }

    companion object {
        const val KEY_BOOK = "audiobookId"
        const val KEY_TRACKS = "trackIndexes"
        const val KEY_WIFI_ONLY = "wifiOnly"
        private const val CHANNEL_ID = "abstrak-downloads"
        private const val NOTIFICATION_ID_BASE = 41_000
        private const val CANCEL_REQUEST_CODE_BASE = 43_000
    }
}
