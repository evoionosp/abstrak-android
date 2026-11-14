package org.evoionosp.abstrak.domain.download
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack

class DownloadScopeCalculator
    @Inject
    constructor() {
        fun totalDurationSeconds(
            tracks: List<AudiobookTrack>,
            declaredDurationSeconds: Long?,
        ): Double? {
            declaredDurationSeconds?.toDouble()?.takeIf { it > 0 }?.let { return it }
            return tracks
                .lastOrNull()
                ?.let { (it.startOffsetInSeconds + it.durationInSeconds).toDouble() }
                ?.takeIf { it > 0 }
        }

        fun bookTrackIndexes(
            chapters: List<AudiobookChapter>,
            tracks: List<AudiobookTrack>,
            positionSeconds: Double?,
            isFinished: Boolean,
            completedTrackIndexes: Set<Int> = emptySet(),
        ): List<Int> {
            if (tracks.isEmpty()) return emptyList()
            if (chapters.isEmpty()) return tracks.map { it.index } - completedTrackIndexes
            val fromChapter =
                if (positionSeconds == null || isFinished) {
                    0
                } else {
                    chapters.indexOfLast { positionSeconds >= it.startInSeconds }.coerceAtLeast(0)
                }
            if (fromChapter == 0) return tracks.map { it.index } - completedTrackIndexes
            val scopeStartSeconds = chapters[fromChapter].startInSeconds
            val wanted =
                tracks
                    .filter { track ->
                        if (track.durationInSeconds <= 0) {
                            track.startOffsetInSeconds >= scopeStartSeconds
                        } else {
                            track.startOffsetInSeconds + track.durationInSeconds > scopeStartSeconds
                        }
                    }.map { it.index }
            return wanted - completedTrackIndexes
        }

        fun chapterTrackIndexes(
            chapterIndex: Int,
            chapters: List<AudiobookChapter>,
            tracks: List<AudiobookTrack>,
            totalDurationSeconds: Double?,
            completedTrackIndexes: Set<Int> = emptySet(),
        ): List<Int> {
            if (tracks.isEmpty()) return emptyList()
            if (chapterIndex !in chapters.indices) return emptyList()
            val endSeconds =
                chapterEndSeconds(
                    chapters = chapters,
                    chapterPosition = chapterIndex,
                    totalDurationSeconds = totalDurationSeconds,
                )
            val wanted =
                tracks
                    .filter { track ->
                        overlaps(
                            track = track,
                            chapterStartSeconds = chapters[chapterIndex].startInSeconds,
                            chapterEndSeconds = endSeconds,
                        )
                    }.map { it.index }
            return wanted - completedTrackIndexes
        }

        fun finishedChapterTrackIndexes(
            chapters: List<AudiobookChapter>,
            tracks: List<AudiobookTrack>,
            totalDurationSeconds: Double?,
            finishedChapterIndexes: Set<Int>,
            completedTrackIndexes: Set<Int> = emptySet(),
        ): List<Int> {
            if (tracks.isEmpty() || finishedChapterIndexes.isEmpty()) return emptyList()
            return tracks
                .filter { track -> track.index in completedTrackIndexes }
                .filter { track ->
                    val overlapping = overlappingChapterIndexes(track, chapters, totalDurationSeconds)
                    overlapping.isNotEmpty() && finishedChapterIndexes.containsAll(overlapping)
                }.map { it.index }
        }

        private fun overlappingChapterIndexes(
            track: AudiobookTrack,
            chapters: List<AudiobookChapter>,
            totalDurationSeconds: Double?,
        ): List<Int> =
            chapters.indices.filter { chapterIndex ->
                overlaps(
                    track = track,
                    chapterStartSeconds = chapters[chapterIndex].startInSeconds,
                    chapterEndSeconds =
                        chapterEndSeconds(
                            chapters = chapters,
                            chapterPosition = chapterIndex,
                            totalDurationSeconds = totalDurationSeconds,
                        ),
                )
            }

        private fun chapterEndSeconds(
            chapters: List<AudiobookChapter>,
            chapterPosition: Int,
            totalDurationSeconds: Double?,
        ): Long {
            chapters[chapterPosition].endInSeconds?.let { return it }
            if (chapterPosition + 1 < chapters.size) return chapters[chapterPosition + 1].startInSeconds
            return totalDurationSeconds?.toLong() ?: Long.MAX_VALUE
        }

        private fun overlaps(
            track: AudiobookTrack,
            chapterStartSeconds: Long,
            chapterEndSeconds: Long,
        ): Boolean {
            val trackStart = track.startOffsetInSeconds
            if (track.durationInSeconds <= 0) {
                return trackStart in chapterStartSeconds..<chapterEndSeconds
            }
            val trackEnd = trackStart + track.durationInSeconds
            return trackStart < chapterEndSeconds && trackEnd > chapterStartSeconds
        }
    }
