package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class ObserveDownloadedBooksUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        operator fun invoke(): Flow<List<DownloadedBook>> = downloadRepository.observeDownloadedBooks()
    }
