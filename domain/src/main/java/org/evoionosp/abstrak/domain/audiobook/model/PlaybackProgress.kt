package org.evoionosp.abstrak.domain.audiobook.model

data class PlaybackProgress(
    val currentTimeSeconds: Double,
    val durationSeconds: Double?,
    val isFinished: Boolean,
) {
    val resumeSeconds: Double?
        get() = currentTimeSeconds.takeIf { !isFinished && it > 0 }
}
