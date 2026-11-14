package org.evoionosp.abstrak.data.audiobook.sync
import android.util.Log
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.data.audiobook.local.dao.PendingProgressDao
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.evoionosp.abstrak.domain.audiobook.model.PendingProgress
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore

@Singleton
class PendingProgressStoreImpl
    @Inject
    constructor(
        private val dao: PendingProgressDao,
        @param:Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : PendingProgressStore {
        override suspend fun put(
            libraryId: String,
            audiobookId: String,
            progress: PlaybackProgress,
        ) {
            bestEffort("Queueing progress of $audiobookId", Unit) {
                dao.upsert(
                    PendingProgressEntity(
                        audiobookId = audiobookId,
                        libraryId = libraryId,
                        currentTimeSeconds = progress.currentTimeSeconds,
                        durationSeconds = progress.durationSeconds,
                        isFinished = progress.isFinished,
                        savedAtMillis = System.currentTimeMillis(),
                    ),
                )
            }
        }

        override suspend fun get(audiobookId: String): PendingProgress? =
            bestEffort("Reading queued progress of $audiobookId", null) { dao.get(audiobookId)?.toDomain() }

        override suspend fun getAll(): List<PendingProgress> =
            bestEffort("Reading queued progress", emptyList()) { dao.getAll().map { it.toDomain() } }

        override suspend fun remove(pending: PendingProgress): Boolean =
            bestEffort("Removing queued progress of ${pending.audiobookId}", false) {
                dao.deleteIfUnchanged(
                    audiobookId = pending.audiobookId,
                    savedAtMillis = pending.savedAtMillis,
                    currentTimeSeconds = pending.progress.currentTimeSeconds,
                    isFinished = pending.progress.isFinished,
                ) > 0
            }

        private suspend fun <T> bestEffort(
            action: String,
            fallback: T,
            block: suspend () -> T,
        ): T =
            withContext(ioDispatcher) {
                try {
                    block()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(TAG, "$action failed", exception)
                    fallback
                }
            }

        private companion object {
            const val TAG = "PendingProgress"
        }
    }

internal fun PendingProgressEntity.toDomain(): PendingProgress =
    PendingProgress(
        libraryId = libraryId,
        audiobookId = audiobookId,
        progress = toPlaybackProgress(),
        savedAtMillis = savedAtMillis,
    )

internal fun PendingProgressEntity.toPlaybackProgress(): PlaybackProgress =
    PlaybackProgress(
        currentTimeSeconds = currentTimeSeconds,
        durationSeconds = durationSeconds,
        isFinished = isFinished,
    )
