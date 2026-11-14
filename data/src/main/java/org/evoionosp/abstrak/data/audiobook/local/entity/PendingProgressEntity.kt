package org.evoionosp.abstrak.data.audiobook.local.entity
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_progress",
    indices = [Index(value = ["libraryId"])],
)
data class PendingProgressEntity(
    @PrimaryKey val audiobookId: String,
    val libraryId: String,
    val currentTimeSeconds: Double,
    val durationSeconds: Double?,
    val isFinished: Boolean,
    val savedAtMillis: Long,
)
