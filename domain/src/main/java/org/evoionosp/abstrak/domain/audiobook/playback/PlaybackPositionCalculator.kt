package org.evoionosp.abstrak.domain.audiobook.playback
import javax.inject.Inject
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack

class PlaybackPositionCalculator
    @Inject
    constructor() {
        fun resolveSeekPosition(
            absoluteSeconds: Double,
            tracks: List<AudiobookTrack>,
        ): TrackPosition {
            if (tracks.isEmpty()) return TrackPosition(trackIndex = 0, offsetMs = 0L)
            val index =
                tracks.indexOfLast { absoluteSeconds >= it.startOffsetInSeconds }.coerceAtLeast(
                    0,
                )
            val offsetSeconds = absoluteSeconds - tracks[index].startOffsetInSeconds
            val offsetMs = (offsetSeconds * 1000).toLong().coerceAtLeast(0L)
            return TrackPosition(trackIndex = index, offsetMs = offsetMs)
        }

        fun absolutePosition(
            trackIndex: Int,
            positionMs: Long,
            tracks: List<AudiobookTrack>,
        ): Double {
            if (trackIndex < 0) return 0.0
            val trackStart = tracks.getOrNull(trackIndex)?.startOffsetInSeconds ?: 0L
            return trackStart + positionMs.coerceAtLeast(0L) / 1000.0
        }

        fun totalDurationSeconds(
            audiobook: Audiobook,
            tracks: List<AudiobookTrack>,
        ): Double {
            val fromTracks =
                tracks
                    .lastOrNull()
                    ?.let { it.startOffsetInSeconds + it.durationInSeconds }
                    ?.toDouble()
                    ?: 0.0
            return if (fromTracks >
                0
            ) {
                fromTracks
            } else {
                audiobook.durationInSeconds?.toDouble() ?: 0.0
            }
        }

        fun chapterTitleForTrack(
            track: AudiobookTrack,
            chapters: List<AudiobookChapter>,
        ): String {
            if (chapters.isEmpty()) return track.title
            val startSeconds = track.startOffsetInSeconds
            val chapter =
                chapters.firstOrNull { chapter ->
                    startSeconds >= chapter.startInSeconds &&
                        (chapter.endInSeconds == null || startSeconds < chapter.endInSeconds)
                }
            return chapter?.title ?: track.title
        }

        fun chapterIndexAt(
            positionSeconds: Double,
            chapters: List<AudiobookChapter>,
        ): Int = chapters.indexOfLast { it.startInSeconds <= positionSeconds }

        fun chaptersPlayedThrough(
            fromSeconds: Double,
            toSeconds: Double,
            elapsedMs: Long,
            speed: Float,
            chapters: List<AudiobookChapter>,
        ): IntRange {
            val advance = toSeconds - fromSeconds
            val playable = elapsedMs / 1000.0 * speed + PLAYED_THROUGH_SLACK_SECONDS
            if (advance <= 0 || advance > playable) return IntRange.EMPTY
            val fromChapter = chapterIndexAt(fromSeconds, chapters).coerceAtLeast(0)
            return fromChapter until chapterIndexAt(toSeconds, chapters)
        }

        fun nextChapterStart(
            positionSeconds: Double,
            chapters: List<AudiobookChapter>,
        ): Double? {
            if (chapters.isEmpty()) return null
            return chapters
                .firstOrNull { it.startInSeconds > positionSeconds }
                ?.startInSeconds
                ?.toDouble()
        }

        fun previousChapterTarget(
            positionSeconds: Double,
            chapters: List<AudiobookChapter>,
        ): Double {
            if (chapters.isEmpty()) return 0.0
            val currentIndex = chapterIndexAt(positionSeconds, chapters)
            if (currentIndex < 0) return 0.0
            val currentStart = chapters[currentIndex].startInSeconds.toDouble()
            return if (positionSeconds - currentStart > PREVIOUS_CHAPTER_THRESHOLD_SECONDS ||
                currentIndex == 0
            ) {
                currentStart
            } else {
                chapters[currentIndex - 1].startInSeconds.toDouble()
            }
        }

        fun isFinished(
            absoluteSeconds: Double,
            totalSeconds: Double?,
        ): Boolean {
            val total = totalSeconds?.takeIf { it > 0 } ?: return false
            return absoluteSeconds >= total - FINISH_THRESHOLD_SECONDS
        }

        companion object {
            private const val FINISH_THRESHOLD_SECONDS = 5.0
            private const val PREVIOUS_CHAPTER_THRESHOLD_SECONDS = 20.0
            private const val PLAYED_THROUGH_SLACK_SECONDS = 2.0
        }
    }
