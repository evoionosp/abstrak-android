package org.evoionosp.abstrak.data.audiobook.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity

@Dao
interface PendingProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingProgressEntity)

    @Query("SELECT * FROM pending_progress WHERE audiobookId = :audiobookId")
    suspend fun get(audiobookId: String): PendingProgressEntity?

    @Query("SELECT * FROM pending_progress ORDER BY savedAtMillis")
    suspend fun getAll(): List<PendingProgressEntity>

    @Query("SELECT * FROM pending_progress WHERE libraryId = :libraryId")
    suspend fun getForLibrary(libraryId: String): List<PendingProgressEntity>

    @Query(
        """
        DELETE FROM pending_progress
        WHERE audiobookId = :audiobookId
            AND savedAtMillis = :savedAtMillis
            AND currentTimeSeconds = :currentTimeSeconds
            AND isFinished = :isFinished
        """,
    )
    suspend fun deleteIfUnchanged(
        audiobookId: String,
        savedAtMillis: Long,
        currentTimeSeconds: Double,
        isFinished: Boolean,
    ): Int
}
