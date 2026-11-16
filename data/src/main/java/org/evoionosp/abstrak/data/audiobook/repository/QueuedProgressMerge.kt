package org.evoionosp.abstrak.data.audiobook.repository
import org.evoionosp.abstrak.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity

internal fun mergeQueuedProgress(
    shelf: List<ContinueListeningEntity>,
    queued: List<PendingProgressEntity>,
): List<ContinueListeningEntity> {
    val merged = shelf.associateByTo(LinkedHashMap()) { it.audiobookId }
    for (pending in queued) {
        val item = merged[pending.audiobookId]
        // The most recently updated record wins, as in ProgressMerge (a tie keeps the server's).
        // Compare whenever the shelf row has a time, even without a position: a book on the
        // server's shelf has progress there, and a missing position only means fetching it failed.
        if (item != null && pending.savedAtMillis <= item.progressLastUpdateMillis) continue
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
