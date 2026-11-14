package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository

class ObserveAudiobookDetailUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) {
        operator fun invoke(
            libraryId: String,
            audiobookId: String,
        ): Flow<AudiobookDetail?> = audiobookRepository.observeAudiobookDetail(libraryId, audiobookId)
    }
