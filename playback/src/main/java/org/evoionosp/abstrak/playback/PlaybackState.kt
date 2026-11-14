package org.evoionosp.abstrak.playback
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook

data class PlaybackState(
    val audiobook: Audiobook? = null,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val currentBookPositionSeconds: Double = 0.0,
    val sleepTimer: SleepTimerState = SleepTimerState.Off,
    val sleepRemainingMs: Long? = null,
)
