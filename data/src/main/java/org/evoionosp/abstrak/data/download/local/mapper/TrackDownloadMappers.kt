package org.evoionosp.abstrak.data.download.local.mapper
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload

internal fun TrackDownloadEntity.toDomain(): TrackDownload =
    TrackDownload(
        audiobookId = audiobookId,
        trackIndex = trackIndex,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED),
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        localPath = localPath,
    )

internal fun queuedRow(
    audiobookId: String,
    trackIndex: Int,
    wifiOnly: Boolean,
    updatedAtMillis: Long,
): TrackDownloadEntity =
    TrackDownloadEntity(
        audiobookId = audiobookId,
        trackIndex = trackIndex,
        status = DownloadStatus.QUEUED.name,
        wifiOnly = wifiOnly,
        updatedAtMillis = updatedAtMillis,
    )
