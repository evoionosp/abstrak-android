package org.evoionosp.abstrak.domain.library.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository

class RefreshLibrariesUseCase
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
        ): DomainResult<Unit> = libraryRepository.refreshLibraries(baseUrl, accessToken)
    }
