package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class ResolveLocalTracksUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        suspend operator fun invoke(
            audiobookId: String,
            tracks: List<AudiobookTrack>,
        ): Map<Int, String> =
            tracks
                .mapNotNull { track ->
                    downloadRepository.localFileForTrack(audiobookId, track.index)?.let { track.index to it }
                }.toMap()
    }
