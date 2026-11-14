package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class RetryBookDownloadUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        suspend operator fun invoke(audiobookId: String) {
            downloadRepository.retryBook(audiobookId)
        }
    }
