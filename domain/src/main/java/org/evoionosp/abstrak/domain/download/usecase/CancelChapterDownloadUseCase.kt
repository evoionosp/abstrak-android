package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

sealed interface CancelChapterDownloadResult {
    data class Cancelled(
        val trackCount: Int,
    ) : CancelChapterDownloadResult

    data object UnknownChapter : CancelChapterDownloadResult

    data object NoPlayableTracks : CancelChapterDownloadResult
}

class CancelChapterDownloadUseCase
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
        ): CancelChapterDownloadResult {
            val detail =
                audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
                    ?: return CancelChapterDownloadResult.NoPlayableTracks
            if (detail.tracks.isEmpty()) return CancelChapterDownloadResult.NoPlayableTracks
            if (chapterIndex !in detail.chapters.indices) return CancelChapterDownloadResult.UnknownChapter
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
            if (indexes.isEmpty()) return CancelChapterDownloadResult.Cancelled(0)
            downloadRepository.cancelTracks(
                audiobookId = audiobookId,
                trackIndexes = indexes,
            )
            return CancelChapterDownloadResult.Cancelled(indexes.size)
        }
    }
