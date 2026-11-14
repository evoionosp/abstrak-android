package org.evoionosp.abstrak.domain.audiobook.model

data class PendingProgress(
    val libraryId: String,
    val audiobookId: String,
    val progress: PlaybackProgress,
    val savedAtMillis: Long,
)
