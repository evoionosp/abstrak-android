package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class ObserveBookDownloadSummaryUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        operator fun invoke(audiobookId: String): Flow<BookDownloadSummary?> =
            downloadRepository.observeBookSummary(audiobookId)
    }
