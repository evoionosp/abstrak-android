package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressMerge
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.library.model.DomainResult

class FetchPlaybackProgressUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val pendingProgressStore: PendingProgressStore,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            audiobookId: String,
        ): PlaybackProgress? {
            val queued = pendingProgressStore.get(audiobookId)
            return when (val result = audiobookRepository.fetchProgress(baseUrl, accessToken, audiobookId)) {
                is DomainResult.Success -> {
                    ProgressMerge.newest(queued?.progress, result.data)
                }

                is DomainResult.Failure -> {
                    queued?.progress
                }
            }
        }
    }
