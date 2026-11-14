package org.evoionosp.abstrak.data.audiobook.repository
import org.evoionosp.abstrak.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.evoionosp.abstrak.data.audiobook.sync.toPlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressMerge

internal fun mergeQueuedProgress(
    shelf: List<ContinueListeningEntity>,
    queued: List<PendingProgressEntity>,
): List<ContinueListeningEntity> {
    val merged = shelf.associateByTo(LinkedHashMap()) { it.audiobookId }
    for (pending in queued) {
        val item = merged[pending.audiobookId]
        val server = item?.currentTimeSeconds?.let { PlaybackProgress(it, durationSeconds = null, isFinished = false) }
        if (!ProgressMerge.isDeviceAhead(pending.toPlaybackProgress(), server)) continue
        if (pending.isFinished) {
            merged.remove(pending.audiobookId)
        } else {
            merged[pending.audiobookId] =
                ContinueListeningEntity(
                    audiobookId = pending.audiobookId,
                    libraryId = pending.libraryId,
                    progressLastUpdateMillis = maxOf(item?.progressLastUpdateMillis ?: 0L, pending.savedAtMillis),
                    currentTimeSeconds = pending.currentTimeSeconds,
                )
        }
    }
    return merged.values.toList()
}
