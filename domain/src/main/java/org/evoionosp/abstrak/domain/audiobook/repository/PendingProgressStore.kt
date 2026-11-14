package org.evoionosp.abstrak.domain.audiobook.repository
import org.evoionosp.abstrak.domain.audiobook.model.PendingProgress
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress

interface PendingProgressStore {
    suspend fun put(
        libraryId: String,
        audiobookId: String,
        progress: PlaybackProgress,
    )

    suspend fun get(audiobookId: String): PendingProgress?

    suspend fun getAll(): List<PendingProgress>

    suspend fun remove(pending: PendingProgress): Boolean
}
