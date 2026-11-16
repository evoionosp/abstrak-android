package org.evoionosp.abstrak.domain.audiobook.model

/**
 * A save the server couldn't take yet, one per book. [PlaybackProgress.lastUpdateMillis] of
 * [progress] is when the position was saved on this device; it identifies the row and is what
 * merges compare against the server's own update time.
 */
data class PendingProgress(
    val libraryId: String,
    val audiobookId: String,
    val progress: PlaybackProgress,
)
