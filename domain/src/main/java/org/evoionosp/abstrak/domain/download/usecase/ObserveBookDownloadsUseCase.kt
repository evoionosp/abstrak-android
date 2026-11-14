package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class ObserveBookDownloadsUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        operator fun invoke(audiobookId: String): Flow<List<TrackDownload>> =
            downloadRepository.observeTrackDownloads(audiobookId)
    }
