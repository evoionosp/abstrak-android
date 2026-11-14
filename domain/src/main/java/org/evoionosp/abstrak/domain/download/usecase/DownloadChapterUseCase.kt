package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

sealed interface DownloadChapterResult {
    data class Enqueued(
        val trackCount: Int,
    ) : DownloadChapterResult

    data object AlreadyComplete : DownloadChapterResult

    data object UnknownChapter : DownloadChapterResult

    data object NoPlayableTracks : DownloadChapterResult
}

class DownloadChapterUseCase
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
            chapterIndex: Int,
            wifiOnly: Boolean = false,
        ): DownloadChapterResult {
            val detail =
                audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
                    ?: return DownloadChapterResult.NoPlayableTracks
            if (detail.tracks.isEmpty()) return DownloadChapterResult.NoPlayableTracks
            if (chapterIndex !in detail.chapters.indices) return DownloadChapterResult.UnknownChapter
            val completed =
                downloadRepository
                    .observeTrackDownloads(audiobookId)
                    .first()
                    .filter { it.status == DownloadStatus.COMPLETED }
                    .map { it.trackIndex }
                    .toSet()
            val totalSeconds =
                scopeCalculator.totalDurationSeconds(
                    tracks = detail.tracks,
                    declaredDurationSeconds = detail.audiobook.durationInSeconds,
                )
            val indexes =
                scopeCalculator.chapterTrackIndexes(
                    chapterIndex = chapterIndex,
                    chapters = detail.chapters,
                    tracks = detail.tracks,
                    totalDurationSeconds = totalSeconds,
                    completedTrackIndexes = completed,
                )
            if (indexes.isEmpty()) return DownloadChapterResult.AlreadyComplete
            val effectiveWifiOnly = wifiOnly || appSettingsStore.settings.first().downloadWifiOnly
            downloadRepository.enqueueTracks(
                audiobookId = audiobookId,
                trackIndexes = indexes,
                wifiOnly = effectiveWifiOnly,
            )
            return DownloadChapterResult.Enqueued(indexes.size)
        }
    }
