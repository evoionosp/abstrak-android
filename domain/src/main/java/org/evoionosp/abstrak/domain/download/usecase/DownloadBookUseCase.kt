package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

sealed interface DownloadBookResult {
    data class Enqueued(
        val trackCount: Int,
    ) : DownloadBookResult

    data object AlreadyComplete : DownloadBookResult

    data object NoPlayableTracks : DownloadBookResult
}

class DownloadBookUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val downloadRepository: DownloadRepository,
        private val scopeCalculator: DownloadScopeCalculator,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend operator fun invoke(
            libraryId: String,
            audiobookId: String,
            positionSeconds: Double?,
            isFinished: Boolean = false,
            wifiOnly: Boolean = false,
        ): DownloadBookResult {
            val detail =
                audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
                    ?: return DownloadBookResult.NoPlayableTracks
            if (detail.tracks.isEmpty()) return DownloadBookResult.NoPlayableTracks
            val completed =
                downloadRepository
                    .observeTrackDownloads(audiobookId)
                    .first()
                    .filter { it.status == DownloadStatus.COMPLETED }
                    .map { it.trackIndex }
                    .toSet()
            val indexes =
                scopeCalculator.bookTrackIndexes(
                    chapters = detail.chapters,
                    tracks = detail.tracks,
                    positionSeconds = positionSeconds,
                    isFinished = isFinished,
                    completedTrackIndexes = completed,
                )
            if (indexes.isEmpty()) return DownloadBookResult.AlreadyComplete
            val effectiveWifiOnly = wifiOnly || appSettingsStore.settings.first().downloadWifiOnly
            downloadRepository.enqueueTracks(
                audiobookId = audiobookId,
                trackIndexes = indexes,
                wifiOnly = effectiveWifiOnly,
            )
            return DownloadBookResult.Enqueued(indexes.size)
        }
    }
