package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

sealed interface DeleteChapterDownloadResult {
    data class Deleted(
        val trackCount: Int,
    ) : DeleteChapterDownloadResult

    data object UnknownChapter : DeleteChapterDownloadResult

    data object NoPlayableTracks : DeleteChapterDownloadResult
}

class DeleteChapterDownloadUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val downloadRepository: DownloadRepository,
        private val scopeCalculator: DownloadScopeCalculator,
    ) {
        suspend operator fun invoke(
            libraryId: String,
            audiobookId: String,
            chapterIndex: Int,
        ): DeleteChapterDownloadResult {
            val detail =
                audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
                    ?: return DeleteChapterDownloadResult.NoPlayableTracks
            if (detail.tracks.isEmpty()) return DeleteChapterDownloadResult.NoPlayableTracks
            if (chapterIndex !in detail.chapters.indices) return DeleteChapterDownloadResult.UnknownChapter
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
                )
            indexes.forEach { trackIndex ->
                downloadRepository.deleteTrack(audiobookId, trackIndex)
            }
            return DeleteChapterDownloadResult.Deleted(indexes.size)
        }
    }
