package org.evoionosp.abstrak.data.download.local.dao
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity

data class DownloadedBookRows(
    @Embedded val detail: AudiobookDetailEntity,
    @Relation(parentColumn = "audiobookId", entityColumn = "audiobookId")
    val downloads: List<TrackDownloadEntity>,
)

@Dao
interface TrackDownloadDao {
    @Query(
        "SELECT * FROM track_downloads WHERE audiobookId = :audiobookId ORDER BY trackIndex ASC",
    )
    fun observeForBook(audiobookId: String): Flow<List<TrackDownloadEntity>>

    @Query("SELECT * FROM track_downloads ORDER BY audiobookId ASC, trackIndex ASC")
    fun observeAll(): Flow<List<TrackDownloadEntity>>

    @Transaction
    @Query(
        "SELECT * FROM audiobook_details WHERE audiobookId IN (SELECT audiobookId FROM track_downloads) " +
            "ORDER BY title COLLATE NOCASE ASC, audiobookId ASC",
    )
    fun observeDownloadedBooks(): Flow<List<DownloadedBookRows>>

    @Query(
        "SELECT * FROM track_downloads WHERE audiobookId = :audiobookId ORDER BY trackIndex ASC",
    )
    suspend fun getForBook(audiobookId: String): List<TrackDownloadEntity>

    @Query("SELECT * FROM track_downloads")
    suspend fun getAll(): List<TrackDownloadEntity>

    @Query(
        "SELECT * FROM track_downloads WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex LIMIT 1",
    )
    suspend fun getTrack(
        audiobookId: String,
        trackIndex: Int,
    ): TrackDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<TrackDownloadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: TrackDownloadEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rows: List<TrackDownloadEntity>)

    @Query(
        "UPDATE track_downloads SET status = 'QUEUED', wifiOnly = :wifiOnly, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex IN (:trackIndexes) " +
            "AND status IN ('FAILED', 'QUEUED')",
    )
    suspend fun requeueTracks(
        audiobookId: String,
        trackIndexes: List<Int>,
        wifiOnly: Boolean,
        updatedAtMillis: Long,
    )

    @Query(
        "UPDATE track_downloads SET wifiOnly = 0, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND wifiOnly = 1 AND status IN ('QUEUED', 'DOWNLOADING')",
    )
    suspend fun allowAnyNetwork(
        audiobookId: String,
        updatedAtMillis: Long,
    )

    @Query(
        "UPDATE track_downloads SET status = 'DOWNLOADING', trackIno = :trackIno, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex " +
            "AND status IN ('QUEUED', 'DOWNLOADING')",
    )
    suspend fun claimForDownload(
        audiobookId: String,
        trackIndex: Int,
        trackIno: String?,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'FAILED', updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex AND status = 'DOWNLOADING'",
    )
    suspend fun markFailed(
        audiobookId: String,
        trackIndex: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'FAILED', bytesDownloaded = 0, " +
            "updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex AND status = 'DOWNLOADING'",
    )
    suspend fun markFailedAndClearBytes(
        audiobookId: String,
        trackIndex: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET bytesDownloaded = 0, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex",
    )
    suspend fun clearBytesDownloaded(
        audiobookId: String,
        trackIndex: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'QUEUED', bytesDownloaded = 0, totalBytes = NULL, localPath = NULL, " +
            "etag = NULL, lastModified = NULL, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex AND status = 'COMPLETED'",
    )
    suspend fun requeueCompleted(
        audiobookId: String,
        trackIndex: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'QUEUED', updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex AND status = 'DOWNLOADING'",
    )
    suspend fun releaseClaim(
        audiobookId: String,
        trackIndex: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, " +
            "updatedAtMillis = :updatedAtMillis WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex " +
            "AND status = 'DOWNLOADING'",
    )
    suspend fun updateProgress(
        audiobookId: String,
        trackIndex: Int,
        bytesDownloaded: Long,
        totalBytes: Long?,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET etag = :etag, lastModified = :lastModified " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex AND status = 'DOWNLOADING'",
    )
    suspend fun updateValidators(
        audiobookId: String,
        trackIndex: Int,
        etag: String?,
        lastModified: String?,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'COMPLETED', bytesDownloaded = :bytesDownloaded, " +
            "totalBytes = :totalBytes, localPath = :localPath, updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex " +
            "AND status IN ('DOWNLOADING', 'QUEUED')",
    )
    suspend fun markComplete(
        audiobookId: String,
        trackIndex: Int,
        bytesDownloaded: Long,
        totalBytes: Long?,
        localPath: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'FAILED', updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND status IN ('QUEUED', 'DOWNLOADING')",
    )
    suspend fun failActive(
        audiobookId: String,
        updatedAtMillis: Long,
    )

    @Query(
        "DELETE FROM track_downloads WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex " +
            "AND status != 'COMPLETED'",
    )
    suspend fun deleteUnfinishedTrack(
        audiobookId: String,
        trackIndex: Int,
    ): Int

    @Query(
        "UPDATE track_downloads SET status = 'QUEUED', updatedAtMillis = :updatedAtMillis " +
            "WHERE audiobookId = :audiobookId AND status = 'DOWNLOADING'",
    )
    suspend fun requeueDownloading(
        audiobookId: String,
        updatedAtMillis: Long,
    )

    @Query(
        "UPDATE track_downloads SET status = 'QUEUED', updatedAtMillis = :updatedAtMillis " +
            "WHERE status = 'DOWNLOADING' AND updatedAtMillis < :olderThanMillis",
    )
    suspend fun requeueStaleDownloading(
        olderThanMillis: Long,
        updatedAtMillis: Long,
    ): Int

    @Query("DELETE FROM track_downloads WHERE audiobookId = :audiobookId")
    suspend fun deleteBook(audiobookId: String)

    @Query(
        "DELETE FROM track_downloads WHERE audiobookId = :audiobookId AND trackIndex = :trackIndex",
    )
    suspend fun deleteTrack(
        audiobookId: String,
        trackIndex: Int,
    )

    @Query("DELETE FROM track_downloads")
    suspend fun deleteAll()
}
