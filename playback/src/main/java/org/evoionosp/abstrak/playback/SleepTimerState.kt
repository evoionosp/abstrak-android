package org.evoionosp.abstrak.playback

sealed interface SleepTimerState {
    data object Off : SleepTimerState

    data class Timer(
        val endsAtMs: Long,
    ) : SleepTimerState

    data class EndOfChapter(
        val targetBookSeconds: Double,
    ) : SleepTimerState
}

fun SleepTimerState.remainingMs(nowMs: Long): Long? =
    when (this) {
        SleepTimerState.Off -> null
        is SleepTimerState.Timer -> (endsAtMs - nowMs).coerceAtLeast(0)
        is SleepTimerState.EndOfChapter -> null
    }
