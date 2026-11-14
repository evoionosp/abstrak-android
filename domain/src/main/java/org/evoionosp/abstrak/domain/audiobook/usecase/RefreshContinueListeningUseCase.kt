package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.DomainResult

class RefreshContinueListeningUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            libraryId: String,
        ): DomainResult<Unit> = audiobookRepository.refreshContinueListening(baseUrl, accessToken, libraryId)
    }
