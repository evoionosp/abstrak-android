package org.evoionosp.abstrak.domain.download.repository
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.model.TrackDownload

interface DownloadRepository {
    fun observeTrackDownloads(audiobookId: String): Flow<List<TrackDownload>>

    fun observeBookSummary(audiobookId: String): Flow<BookDownloadSummary?>

    fun observeDownloadedBooks(): Flow<List<DownloadedBook>>

    fun observeStorageUsage(): Flow<StorageUsage>

    suspend fun enqueueTracks(
        audiobookId: String,
        trackIndexes: List<Int>,
        wifiOnly: Boolean = false,
    )

    suspend fun cancelBook(audiobookId: String)

    suspend fun cancelTracks(
        audiobookId: String,
        trackIndexes: List<Int>,
    )

    suspend fun retryBook(audiobookId: String)

    suspend fun reconcileStorage()

    /** Stops downloads in progress and holds back queued ones, all staying queued, until [resumeDownloads]. */
    suspend fun pauseDownloads()

    /** Starts every queued download again, including any [pauseDownloads] held back. */
    suspend fun resumeDownloads()

    suspend fun deleteBook(audiobookId: String)

    suspend fun deleteTrack(
        audiobookId: String,
        trackIndex: Int,
    )

    suspend fun deleteAll()

    suspend fun localFileForTrack(
        audiobookId: String,
        trackIndex: Int,
    ): String?
}
