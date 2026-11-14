package org.evoionosp.abstrak.data.download.local.entity
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "track_downloads",
    primaryKeys = ["audiobookId", "trackIndex"],
    indices = [Index(value = ["audiobookId"])],
)
data class TrackDownloadEntity(
    val audiobookId: String,
    val trackIndex: Int,
    val status: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long? = null,
    val localPath: String? = null,
    val wifiOnly: Boolean = false,
    val etag: String? = null,
    val lastModified: String? = null,
    val trackIno: String? = null,
    val updatedAtMillis: Long = 0L,
)
