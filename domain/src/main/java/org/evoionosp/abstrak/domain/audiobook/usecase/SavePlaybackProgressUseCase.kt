package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.audiobook.sync.ProgressSyncScheduler
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult

class SavePlaybackProgressUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val pendingProgressStore: PendingProgressStore,
        private val syncPendingProgress: SyncPendingProgressUseCase,
        private val progressSyncScheduler: ProgressSyncScheduler,
        private val calculator: PlaybackPositionCalculator,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            libraryId: String,
            audiobookId: String,
            absoluteSeconds: Double,
            totalSeconds: Double?,
        ): DomainResult<Unit> {
            val isFinished = calculator.isFinished(absoluteSeconds, totalSeconds)
            audiobookRepository.recordListeningProgress(
                libraryId = libraryId,
                audiobookId = audiobookId,
                positionSeconds = absoluteSeconds,
                isFinished = isFinished,
            )
            val progress =
                PlaybackProgress(
                    currentTimeSeconds = absoluteSeconds,
                    durationSeconds = totalSeconds,
                    isFinished = isFinished,
                )
            if (pendingProgressStore.get(audiobookId) != null) {
                queue(libraryId, audiobookId, progress)
                return when (syncPendingProgress(audiobookId)) {
                    ProgressSyncResult.DONE -> DomainResult.Success(Unit)
                    ProgressSyncResult.RETRY -> DomainResult.Failure(CatalogError.NETWORK)
                    ProgressSyncResult.SIGNED_OUT -> DomainResult.Failure(CatalogError.AUTH)
                }
            }
            val result =
                audiobookRepository.saveProgress(
                    baseUrl = baseUrl,
                    accessToken = accessToken,
                    audiobookId = audiobookId,
                    progress = progress,
                )
            if (result is DomainResult.Failure && result.error.isWorthRetrying()) {
                queue(libraryId, audiobookId, progress)
            }
            return result
        }

        private suspend fun queue(
            libraryId: String,
            audiobookId: String,
            progress: PlaybackProgress,
        ) {
            pendingProgressStore.put(libraryId, audiobookId, progress)
            progressSyncScheduler.schedule()
        }

        private fun CatalogError.isWorthRetrying(): Boolean =
            this == CatalogError.CONNECTIVITY_UNAVAILABLE ||
                this == CatalogError.NETWORK ||
                this == CatalogError.UNKNOWN
    }
