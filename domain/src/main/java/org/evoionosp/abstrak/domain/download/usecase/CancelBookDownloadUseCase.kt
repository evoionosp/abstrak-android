package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class CancelBookDownloadUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        suspend operator fun invoke(audiobookId: String) {
            downloadRepository.cancelBook(audiobookId)
        }
    }
