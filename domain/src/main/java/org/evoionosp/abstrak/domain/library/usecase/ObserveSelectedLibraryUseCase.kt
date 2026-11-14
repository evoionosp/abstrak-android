package org.evoionosp.abstrak.domain.library.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository

class ObserveSelectedLibraryUseCase
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
    ) {
        operator fun invoke(): Flow<AudiobookLibrary?> = libraryRepository.observeSelectedLibrary()
    }
