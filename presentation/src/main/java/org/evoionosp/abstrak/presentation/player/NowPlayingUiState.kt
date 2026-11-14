package org.evoionosp.abstrak.presentation.player
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.playback.PlaybackState
import org.evoionosp.abstrak.playback.SleepTimerState

data class NowPlayingUiState(
    val playback: PlaybackState = PlaybackState(),
    val totalSeconds: Double = 0.0,
    val chapters: List<AudiobookChapter> = emptyList(),
    val currentChapterIndex: Int = -1,
    val bookProgress: Float = 0f,
    val chapterTitle: String? = null,
    val sleepFooter: SleepFooter = SleepFooter.Off,
    val endOfChapterAvailable: Boolean = false,
    val chapterTimeLeftMs: Long? = null,
    val isLoading: Boolean = false,
)

sealed interface SleepFooter {
    data object Off : SleepFooter

    data class Minutes(
        val minutes: Int,
    ) : SleepFooter

    data object EndOfChapter : SleepFooter
}

internal fun nowPlayingUiState(
    playback: PlaybackState,
    totalSeconds: Double,
    chapters: List<AudiobookChapter>,
    positionCalculator: PlaybackPositionCalculator,
): NowPlayingUiState {
    val positionSeconds = playback.currentBookPositionSeconds
    val chapterIndex = positionCalculator.chapterIndexAt(positionSeconds, chapters)
    val chapterEndSeconds = sleepChapterTargetSeconds(chapters, positionSeconds, totalSeconds)
    val speed = playback.playbackSpeed.takeIf { it > 0f } ?: 1f
    return NowPlayingUiState(
        playback = playback,
        totalSeconds = totalSeconds,
        chapters = chapters,
        currentChapterIndex = chapterIndex,
        bookProgress =
            if (totalSeconds > 0) (positionSeconds / totalSeconds).toFloat().coerceIn(0f, 1f) else 0f,
        chapterTitle = chapters.getOrNull(chapterIndex)?.title,
        sleepFooter = sleepFooter(playback.sleepTimer, playback.sleepRemainingMs),
        endOfChapterAvailable = chapterEndSeconds != null,
        chapterTimeLeftMs = chapterEndSeconds?.let { ((it - positionSeconds) * 1000 / speed).toLong() },
        isLoading = playback.isPreparing || playback.isBuffering,
    )
}

internal fun sleepFooter(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
): SleepFooter =
    when (sleepTimer) {
        SleepTimerState.Off -> {
            SleepFooter.Off
        }

        is SleepTimerState.Timer -> {
            SleepFooter.Minutes((((remainingMs ?: 0L) + 59_999) / 60_000).coerceAtLeast(1).toInt())
        }

        is SleepTimerState.EndOfChapter -> {
            SleepFooter.EndOfChapter
        }
    }

internal fun sleepChapterTargetSeconds(
    chapters: List<AudiobookChapter>,
    positionSeconds: Double,
    totalSeconds: Double,
): Double? {
    val index = chapters.indexOfLast { it.startInSeconds <= positionSeconds }
    chapters
        .getOrNull(index)
        ?.endInSeconds
        ?.toDouble()
        ?.takeIf { it > positionSeconds }
        ?.let { return it }
    chapters
        .getOrNull(index + 1)
        ?.startInSeconds
        ?.toDouble()
        ?.takeIf { it > positionSeconds }
        ?.let { return it }
    return totalSeconds.takeIf { it > positionSeconds }
}
