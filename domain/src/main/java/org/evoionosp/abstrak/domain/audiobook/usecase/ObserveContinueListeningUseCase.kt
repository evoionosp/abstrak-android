package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository

class ObserveContinueListeningUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) {
        operator fun invoke(libraryId: String): Flow<List<Audiobook>> =
            audiobookRepository.observeContinueListening(libraryId)
    }
