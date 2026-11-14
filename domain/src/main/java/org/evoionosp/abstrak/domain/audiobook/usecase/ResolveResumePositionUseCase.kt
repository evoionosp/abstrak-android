package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressMerge
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.library.model.DomainResult

class ResolveResumePositionUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val pendingProgressStore: PendingProgressStore,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            audiobook: Audiobook,
        ): Double {
            val queued = pendingProgressStore.get(audiobook.id)?.progress
            val progress =
                when (val result = audiobookRepository.fetchProgress(baseUrl, accessToken, audiobook.id)) {
                    is DomainResult.Success -> ProgressMerge.furthest(queued, result.data) ?: return 0.0
                    is DomainResult.Failure -> queued ?: return recordedPosition(audiobook)
                }
            return progress.resumeSeconds ?: 0.0
        }

        private suspend fun recordedPosition(audiobook: Audiobook): Double =
            audiobookRepository
                .observeContinueListening(audiobook.libraryId)
                .first()
                .firstOrNull { it.id == audiobook.id }
                ?.progressSeconds
                ?: audiobook.progressSeconds
                ?: 0.0
    }
