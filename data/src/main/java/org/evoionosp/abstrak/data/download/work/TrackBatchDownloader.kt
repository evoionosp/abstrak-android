package org.evoionosp.abstrak.data.download.work
import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.data.download.remote.DownloadError
import org.evoionosp.abstrak.data.download.remote.DownloadOutcome
import org.evoionosp.abstrak.data.download.remote.RemoteFileDownloader
import org.evoionosp.abstrak.data.download.remote.ResumeValidators
import org.evoionosp.abstrak.domain.download.model.DownloadStatus

data class BatchResult(
    val retryNeeded: Boolean,
    val aborted: Boolean,
)

class TrackBatchDownloader
    @Inject
    constructor(
        private val trackDownloadDao: TrackDownloadDao,
        private val audiobookDetailDao: AudiobookDetailDao,
        private val fileStore: DownloadFileStore,
        private val downloader: RemoteFileDownloader,
        private val bookLocks: DownloadBookLocks,
        @Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun downloadBatch(
            audiobookId: String,
            trackIndexes: List<Int>,
            tokenProvider: suspend () -> String?,
            runAttemptCount: Int = 0,
            onTrackStarted: suspend (doneCount: Int, totalCount: Int) -> Unit = { _, _ -> },
        ): BatchResult =
            withContext(ioDispatcher) {
                bookLocks.lockFor(audiobookId).withLock {
                    runBatch(audiobookId, trackIndexes, tokenProvider, runAttemptCount, onTrackStarted)
                }
            }

        private suspend fun runBatch(
            audiobookId: String,
            trackIndexes: List<Int>,
            tokenProvider: suspend () -> String?,
            runAttemptCount: Int,
            onTrackStarted: suspend (doneCount: Int, totalCount: Int) -> Unit,
        ): BatchResult =
            coroutineScope {
                val catalogTracks =
                    audiobookDetailDao.getTracks(audiobookId).associateBy { it.trackIndex }
                var retryNeeded = false
                try {
                    trackIndexes.forEachIndexed { position, trackIndex ->
                        ensureActive()
                        onTrackStarted(position, trackIndexes.size)
                        val catalog = catalogTracks[trackIndex]
                        if (catalog == null) {
                            trackDownloadDao.deleteTrack(audiobookId, trackIndex)
                            return@forEachIndexed
                        }
                        val ino = inoFromUrl(catalog.remoteUrl)
                        val existing = trackDownloadDao.getTrack(audiobookId, trackIndex)
                        if (isStaleFile(existing, ino)) {
                            fileStore.deleteTrackFiles(audiobookId, trackIndex)
                            trackDownloadDao.clearBytesDownloaded(audiobookId, trackIndex, now())
                            when (existing?.status) {
                                DownloadStatus.COMPLETED.name -> {
                                    if (trackDownloadDao.requeueCompleted(audiobookId, trackIndex, now()) == 0) {
                                        return@forEachIndexed
                                    }
                                }

                                DownloadStatus.QUEUED.name, DownloadStatus.DOWNLOADING.name -> {
                                    Unit
                                }

                                else -> {
                                    return@forEachIndexed
                                }
                            }
                        } else if (isAlreadyComplete(existing)) {
                            return@forEachIndexed
                        }
                        val claimed =
                            trackDownloadDao.claimForDownload(
                                audiobookId = audiobookId,
                                trackIndex = trackIndex,
                                trackIno = ino,
                                updatedAtMillis = now(),
                            )
                        if (claimed == 0) return@forEachIndexed
                        val tempFile = fileStore.tempFile(audiobookId, trackIndex)
                        try {
                            val accessToken =
                                tokenProvider() ?: run {
                                    trackDownloadDao.failActive(audiobookId, now())
                                    return@coroutineScope BatchResult(retryNeeded, aborted = true)
                                }
                            val extension = fileStore.extensionFor(catalog.mimeType, catalog.remoteUrl)
                            val storedTotal = existing?.totalBytes
                            val outcome =
                                downloader.download(
                                    url = downloadUrl(catalog.remoteUrl),
                                    destFile = tempFile,
                                    accessToken = accessToken,
                                    resumeValidators =
                                        ResumeValidators(
                                            existing?.etag,
                                            existing?.lastModified,
                                        ),
                                    onProgress = { bytes, total ->
                                        val landed =
                                            recordWhileMine(audiobookId, trackIndex) {
                                                trackDownloadDao.updateProgress(
                                                    audiobookId = audiobookId,
                                                    trackIndex = trackIndex,
                                                    bytesDownloaded = bytes,
                                                    totalBytes = total,
                                                    updatedAtMillis = now(),
                                                )
                                            }
                                        if (!landed) throw StaleTrackException()
                                    },
                                    onHeaders = { etag, lastModified ->
                                        val landed =
                                            recordWhileMine(audiobookId, trackIndex) {
                                                trackDownloadDao.updateValidators(
                                                    audiobookId = audiobookId,
                                                    trackIndex = trackIndex,
                                                    etag = etag,
                                                    lastModified = lastModified,
                                                )
                                            }
                                        if (!landed) throw StaleTrackException()
                                    },
                                )
                            when (outcome) {
                                is DownloadOutcome.Success -> {
                                    if (isCorruptResume(outcome, storedTotal)) {
                                        fileStore.deleteTempFile(audiobookId, trackIndex)
                                        trackDownloadDao.markFailedAndClearBytes(audiobookId, trackIndex, now())
                                    } else {
                                        publish(
                                            audiobookId = audiobookId,
                                            trackIndex = trackIndex,
                                            tempFile = tempFile,
                                            extension = extension,
                                            bytesWritten = outcome.bytesWritten,
                                            totalBytes = outcome.totalBytes,
                                        )
                                    }
                                }

                                is DownloadOutcome.Failed -> {
                                    val retryable =
                                        outcome.error == DownloadError.Network &&
                                            runAttemptCount < MAX_RUN_ATTEMPTS
                                    if (settleFailure(audiobookId, trackIndex, tempFile, retryable)) {
                                        retryNeeded = true
                                    }
                                }
                            }
                        } catch (stale: StaleTrackException) {
                            dropIfOrphaned(audiobookId, trackIndex, tempFile)
                            return@forEachIndexed
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Exception) {
                            Log.w(TAG, "Track $trackIndex of $audiobookId failed unexpectedly", error)
                            val retryable = runAttemptCount < MAX_RUN_ATTEMPTS
                            if (settleFailure(audiobookId, trackIndex, tempFile, retryable)) {
                                retryNeeded = true
                            }
                        }
                    }
                    onTrackStarted(trackIndexes.size, trackIndexes.size)
                    BatchResult(retryNeeded, aborted = false)
                } catch (cancellation: CancellationException) {
                    withContext(NonCancellable) {
                        trackDownloadDao.requeueDownloading(audiobookId, now())
                    }
                    throw cancellation
                }
            }

        private fun isAlreadyComplete(row: TrackDownloadEntity?): Boolean {
            if (row?.status != DownloadStatus.COMPLETED.name) return false
            return row.localPath?.let { File(it).exists() } ?: false
        }

        private fun isStaleFile(
            row: TrackDownloadEntity?,
            ino: String?,
        ): Boolean {
            val stored = row?.trackIno ?: return false
            return ino != null && stored != ino
        }

        private fun isCorruptResume(
            outcome: DownloadOutcome.Success,
            storedTotal: Long?,
        ): Boolean =
            outcome.resumed &&
                outcome.totalBytes != null &&
                storedTotal != null &&
                outcome.totalBytes != storedTotal

        private suspend fun publish(
            audiobookId: String,
            trackIndex: Int,
            tempFile: File,
            extension: String,
            bytesWritten: Long,
            totalBytes: Long?,
        ) {
            val finalFile = fileStore.trackFile(audiobookId, trackIndex, extension)
            fileStore.publishTempFile(tempFile, finalFile)
            val published =
                trackDownloadDao.markComplete(
                    audiobookId = audiobookId,
                    trackIndex = trackIndex,
                    bytesDownloaded = bytesWritten,
                    totalBytes = totalBytes,
                    localPath = finalFile.absolutePath,
                    updatedAtMillis = now(),
                )
            if (published == 0) dropIfOrphaned(audiobookId, trackIndex, finalFile)
        }

        private suspend fun recordWhileMine(
            audiobookId: String,
            trackIndex: Int,
            write: suspend () -> Int,
        ): Boolean {
            if (write() != 0) return true
            val row = trackDownloadDao.getTrack(audiobookId, trackIndex) ?: return false
            if (row.status != DownloadStatus.QUEUED.name) return false
            if (trackDownloadDao.claimForDownload(audiobookId, trackIndex, row.trackIno, now()) == 0) {
                return false
            }
            return write() != 0
        }

        private suspend fun settleFailure(
            audiobookId: String,
            trackIndex: Int,
            tempFile: File,
            retryable: Boolean,
        ): Boolean {
            val partGone = !tempFile.exists()
            if (!retryable) {
                settleTrack(audiobookId, trackIndex, tempFile) {
                    if (partGone) {
                        trackDownloadDao.markFailedAndClearBytes(audiobookId, trackIndex, now())
                    } else {
                        trackDownloadDao.markFailed(audiobookId, trackIndex, now())
                    }
                }
                return false
            }
            val released =
                settleTrack(audiobookId, trackIndex, tempFile) {
                    trackDownloadDao.releaseClaim(audiobookId, trackIndex, now())
                }
            if (released && partGone) {
                trackDownloadDao.clearBytesDownloaded(audiobookId, trackIndex, now())
            }
            return released
        }

        private suspend fun settleTrack(
            audiobookId: String,
            trackIndex: Int,
            tempFile: File,
            write: suspend () -> Int,
        ): Boolean {
            if (write() != 0) return true
            dropIfOrphaned(audiobookId, trackIndex, tempFile)
            return false
        }

        private suspend fun dropIfOrphaned(
            audiobookId: String,
            trackIndex: Int,
            file: File,
        ) {
            if (trackDownloadDao.getTrack(audiobookId, trackIndex) == null) file.delete()
        }

        private fun now(): Long = System.currentTimeMillis()
    }

private class StaleTrackException : RuntimeException()

internal fun downloadUrl(remoteUrl: String): String {
    val base = remoteUrl.substringBefore("?")
    val query = remoteUrl.substringAfter("?", "")
    if (base.endsWith(DOWNLOAD_SUFFIX)) return remoteUrl
    return base + DOWNLOAD_SUFFIX + ("?$query".takeIf { query.isNotEmpty() }.orEmpty())
}

private const val DOWNLOAD_SUFFIX = "/download"
private const val TAG = "TrackBatchDownloader"
private const val MAX_RUN_ATTEMPTS = 5

internal fun inoFromUrl(remoteUrl: String): String? =
    remoteUrl
        .substringBefore("?")
        .substringAfterLast("/file/", "")
        .substringBefore("/")
        .takeIf { it.isNotEmpty() }
