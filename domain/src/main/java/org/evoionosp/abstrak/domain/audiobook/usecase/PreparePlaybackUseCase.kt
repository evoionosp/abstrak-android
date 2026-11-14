package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult

sealed interface PlaybackPreparation {
    data class Ready(
        val detail: AudiobookDetail,
    ) : PlaybackPreparation

    data object NoAudio : PlaybackPreparation

    data class Failed(
        val error: CatalogError,
    ) : PlaybackPreparation
}

class PreparePlaybackUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            libraryId: String,
            audiobookId: String,
        ): PlaybackPreparation {
            val cached = audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
            if (cached != null && cached.tracks.isNotEmpty()) return PlaybackPreparation.Ready(cached)
            val refreshed =
                audiobookRepository.refreshAudiobookDetail(
                    baseUrl = baseUrl,
                    accessToken = accessToken,
                    libraryId = libraryId,
                    audiobookId = audiobookId,
                )
            val detail = audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
            return when {
                detail != null && detail.tracks.isNotEmpty() -> PlaybackPreparation.Ready(detail)
                refreshed is DomainResult.Failure -> PlaybackPreparation.Failed(refreshed.error)
                else -> PlaybackPreparation.NoAudio
            }
        }
    }
