package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.evoionosp.abstrak.domain.audiobook.model.PendingProgress
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressMerge
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgress
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgressEvents
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase

enum class ProgressSyncResult {
    DONE,
    RETRY,
    SIGNED_OUT,
}

@Singleton
class SyncPendingProgressUseCase
    @Inject
    constructor(
        private val pendingProgressStore: PendingProgressStore,
        private val audiobookRepository: AudiobookRepository,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val pulledProgressEvents: PulledProgressEvents,
    ) {
        private val mutex = Mutex()

        suspend operator fun invoke(audiobookId: String? = null): ProgressSyncResult =
            mutex.withLock {
                val queued = queued(audiobookId)
                if (queued.isEmpty()) return@withLock ProgressSyncResult.DONE
                val session = getValidSessionUseCase() ?: return@withLock ProgressSyncResult.SIGNED_OUT
                for (pending in queued) {
                    val result = sync(session, pending)
                    if (result != ProgressSyncResult.DONE) return@withLock result
                }
                if (queued(audiobookId).isEmpty()) ProgressSyncResult.DONE else ProgressSyncResult.RETRY
            }

        private suspend fun queued(audiobookId: String?): List<PendingProgress> =
            if (audiobookId == null) {
                pendingProgressStore.getAll()
            } else {
                listOfNotNull(pendingProgressStore.get(audiobookId))
            }

        private suspend fun sync(
            session: LoginSession,
            pending: PendingProgress,
        ): ProgressSyncResult {
            val server =
                when (
                    val result =
                        audiobookRepository.fetchProgress(session.baseUrl, session.accessToken, pending.audiobookId)
                ) {
                    is DomainResult.Success -> result.data
                    is DomainResult.Failure -> return result.error.toSyncResult()
                }
            if (server != null && !ProgressMerge.isDeviceNewer(pending.progress, server)) {
                if (pendingProgressStore.remove(pending)) {
                    audiobookRepository.recordListeningProgress(
                        libraryId = pending.libraryId,
                        audiobookId = pending.audiobookId,
                        positionSeconds = server.currentTimeSeconds,
                        isFinished = server.isFinished,
                    )
                    pulledProgressEvents.publish(PulledProgress(pending.audiobookId, server))
                }
                return ProgressSyncResult.DONE
            }
            return when (
                val result =
                    audiobookRepository.saveProgress(
                        session.baseUrl,
                        session.accessToken,
                        pending.audiobookId,
                        pending.progress,
                    )
            ) {
                is DomainResult.Success -> {
                    pendingProgressStore.remove(pending)
                    ProgressSyncResult.DONE
                }

                is DomainResult.Failure -> {
                    if (result.error == CatalogError.NOT_FOUND) {
                        pendingProgressStore.remove(pending)
                        ProgressSyncResult.DONE
                    } else {
                        result.error.toSyncResult()
                    }
                }
            }
        }

        private fun CatalogError.toSyncResult(): ProgressSyncResult =
            if (this == CatalogError.AUTH) ProgressSyncResult.SIGNED_OUT else ProgressSyncResult.RETRY
    }
