package org.evoionosp.abstrak.domain.audiobook.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.SyncStatus

class ObserveLibrarySyncStatusUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) {
        operator fun invoke(libraryId: String): Flow<SyncStatus> =
            audiobookRepository.observeLibrarySyncStatus(libraryId)
    }
