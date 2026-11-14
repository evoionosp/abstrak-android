package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

sealed interface DeleteFinishedChapterDownloadsResult {
    data class Deleted(
        val trackCount: Int,
    ) : DeleteFinishedChapterDownloadsResult

    data object UnknownChapter : DeleteFinishedChapterDownloadsResult

    data object NoPlayableTracks : DeleteFinishedChapterDownloadsResult
}

class DeleteFinishedChapterDownloadsUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val downloadRepository: DownloadRepository,
        private val scopeCalculator: DownloadScopeCalculator,
    ) {
        suspend operator fun invoke(
            libraryId: String,
            audiobookId: String,
            finishedChapterIndexes: Set<Int>,
        ): DeleteFinishedChapterDownloadsResult {
            val detail =
                audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
                    ?: return DeleteFinishedChapterDownloadsResult.NoPlayableTracks
            if (detail.tracks.isEmpty()) return DeleteFinishedChapterDownloadsResult.NoPlayableTracks
            if (finishedChapterIndexes.none { it in detail.chapters.indices }) {
                return DeleteFinishedChapterDownloadsResult.UnknownChapter
            }
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
                scopeCalculator.finishedChapterTrackIndexes(
                    chapters = detail.chapters,
                    tracks = detail.tracks,
                    totalDurationSeconds = totalSeconds,
                    finishedChapterIndexes = finishedChapterIndexes,
                    completedTrackIndexes = completed,
                )
            indexes.forEach { trackIndex ->
                downloadRepository.deleteTrack(audiobookId, trackIndex)
            }
            return DeleteFinishedChapterDownloadsResult.Deleted(indexes.size)
        }
    }
