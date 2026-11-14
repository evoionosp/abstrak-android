package org.evoionosp.abstrak.domain.library.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository

class SelectLibraryUseCase
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
    ) {
        suspend operator fun invoke(libraryId: String): DomainResult<Unit> = libraryRepository.selectLibrary(libraryId)
    }
