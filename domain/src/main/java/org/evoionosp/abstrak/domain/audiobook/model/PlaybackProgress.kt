package org.evoionosp.abstrak.domain.audiobook.model

data class PlaybackProgress(
    val currentTimeSeconds: Double,
    val durationSeconds: Double?,
    val isFinished: Boolean,
    /**
     * When this position was recorded, in Unix epoch milliseconds, matching Audiobookshelf media
     * progress lastUpdate: the device's clock for a save made here (which is sent as lastUpdate),
     * the server's lastUpdate for progress fetched from it. Zero means unknown.
     */
    val lastUpdateMillis: Long = 0L,
) {
    val resumeSeconds: Double?
        get() = currentTimeSeconds.takeIf { !isFinished && it > 0 }
}
