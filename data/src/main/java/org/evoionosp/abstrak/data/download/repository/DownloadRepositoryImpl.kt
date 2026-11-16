package org.evoionosp.abstrak.data.download.repository
import android.database.sqlite.SQLiteFullException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.abstrak.data.audiobook.local.mapper.toAudiobook
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.file.isPlayableDownload
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.data.download.local.mapper.queuedRow
import org.evoionosp.abstrak.data.download.local.mapper.toDomain
import org.evoionosp.abstrak.data.download.work.DownloadWorkScheduler
import org.evoionosp.abstrak.data.download.work.inoFromUrl
import org.evoionosp.abstrak.data.storage.StorageMeter
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

@Singleton
class DownloadRepositoryImpl
    @Inject
    constructor(
        private val trackDownloadDao: TrackDownloadDao,
        private val audiobookDetailDao: AudiobookDetailDao,
        private val fileStore: DownloadFileStore,
        private val storageMeter: StorageMeter,
        private val scheduler: DownloadWorkScheduler,
        @Named("io") private val ioDispatcher: CoroutineDispatcher,
        @Named("application_scope") private val applicationScope: CoroutineScope,
    ) : DownloadRepository {
        private val filesDeleted = MutableStateFlow(0)

        override fun observeTrackDownloads(audiobookId: String): Flow<List<TrackDownload>> =
            trackDownloadDao.observeForBook(audiobookId).map { rows ->
                rows.map { it.toDomain() }
            }

        override fun observeBookSummary(audiobookId: String): Flow<BookDownloadSummary?> =
            trackDownloadDao.observeForBook(audiobookId).map { rows ->
                rows.toSummaryOrNull(audiobookId)
            }

        override fun observeDownloadedBooks(): Flow<List<DownloadedBook>> =
            trackDownloadDao.observeDownloadedBooks().map { books ->
                books.mapNotNull { book ->
                    book.downloads.toSummaryOrNull(book.detail.audiobookId)?.let { summary ->
                        DownloadedBook(audiobook = book.detail.toAudiobook(), summary = summary)
                    }
                }
            }

        override fun observeStorageUsage(): Flow<StorageUsage> =
            flow {
                val cacheBytes = storageMeter.cacheBytes()
                emitAll(
                    combine(trackDownloadDao.observeAll(), filesDeleted) { rows, _ ->
                        StorageUsage(
                            downloadsBytes = rows.sumOf { it.bytesDownloaded },
                            cacheBytes = cacheBytes,
                            deviceFreeBytes = storageMeter.deviceFreeBytes(),
                        )
                    },
                )
            }.distinctUntilChanged()
                .flowOn(ioDispatcher)

        override suspend fun enqueueTracks(
            audiobookId: String,
            trackIndexes: List<Int>,
            wifiOnly: Boolean,
        ) {
            if (trackIndexes.isEmpty()) return
            withContext(ioDispatcher) {
                val rows = trackDownloadDao.getForBook(audiobookId)
                val completed =
                    rows
                        .filter { it.status == DownloadStatus.COMPLETED.name }
                        .map { it.trackIndex }
                        .toSet()
                val fresh = trackIndexes.filter { it !in completed }
                if (fresh.isEmpty()) return@withContext
                trackDownloadDao.insertIgnore(
                    fresh.map { queuedRow(audiobookId, it, wifiOnly, now()) },
                )
                trackDownloadDao.requeueTracks(audiobookId, fresh, wifiOnly, now())
                if (!wifiOnly && rows.any { it.wifiOnly && it.isActive }) {
                    trackDownloadDao.allowAnyNetwork(audiobookId, now())
                    val active = trackDownloadDao.getForBook(audiobookId).filter { it.isActive }
                    scheduler.allowAnyNetwork(audiobookId, active.map { it.trackIndex })
                }
                scheduler.scheduleBook(audiobookId, fresh, wifiOnly)
            }
        }

        override suspend fun cancelBook(audiobookId: String) {
            withContext(ioDispatcher) {
                val unfinished =
                    trackDownloadDao
                        .getForBook(audiobookId)
                        .filter { it.status != DownloadStatus.COMPLETED.name }
                        .map { it.trackIndex }
                cancelUnfinished(audiobookId, unfinished)
                scheduler.cancelBook(audiobookId)
            }
        }

        override suspend fun cancelTracks(
            audiobookId: String,
            trackIndexes: List<Int>,
        ) {
            if (trackIndexes.isEmpty()) return
            withContext(ioDispatcher) {
                cancelUnfinished(audiobookId, trackIndexes)
            }
        }

        override suspend fun retryBook(audiobookId: String) {
            withContext(ioDispatcher) {
                val failed =
                    trackDownloadDao
                        .getForBook(audiobookId)
                        .filter { it.status == DownloadStatus.FAILED.name }
                if (failed.isEmpty()) return@withContext
                enqueueTracks(
                    audiobookId = audiobookId,
                    trackIndexes = failed.map { it.trackIndex },
                    wifiOnly = failed.any { it.wifiOnly },
                )
            }
        }

        override suspend fun deleteBook(audiobookId: String) {
            applicationScope
                .async(ioDispatcher) {
                    deleteRowsFirst(
                        deleteRows = { trackDownloadDao.deleteBook(audiobookId) },
                        freeSpace = {
                            scheduler.cancelBook(audiobookId)
                            fileStore.deleteBookFiles(audiobookId)
                        },
                    )
                    filesDeleted.update { it + 1 }
                }.await()
        }

        override suspend fun deleteTrack(
            audiobookId: String,
            trackIndex: Int,
        ) {
            withContext(ioDispatcher) {
                fileStore.deleteTrackFiles(audiobookId, trackIndex)
                trackDownloadDao.deleteTrack(audiobookId, trackIndex)
                rescheduleActive(audiobookId)
            }
        }

        override suspend fun deleteAll() {
            applicationScope
                .async(ioDispatcher) {
                    deleteRowsFirst(
                        deleteRows = { trackDownloadDao.deleteAll() },
                        freeSpace = {
                            scheduler.cancelAll()
                            fileStore.deleteAll()
                        },
                    )
                    filesDeleted.update { it + 1 }
                }.await()
        }

        override suspend fun localFileForTrack(
            audiobookId: String,
            trackIndex: Int,
        ): String? =
            withContext(ioDispatcher) {
                val row = trackDownloadDao.getTrack(audiobookId, trackIndex) ?: return@withContext null
                if (row.status != DownloadStatus.COMPLETED.name) return@withContext null
                val path = row.localPath?.takeIf(::isPlayableDownload) ?: return@withContext null
                if (isStaleIdentity(row)) return@withContext null
                path
            }

        override suspend fun reconcileStorage() {
            withContext(ioDispatcher) {
                trackDownloadDao.requeueStaleDownloading(now() - STALE_DOWNLOADING_MILLIS, now())
                val rows = trackDownloadDao.getAll()
                rows
                    .filter { it.status == DownloadStatus.COMPLETED.name && !isPlayableDownload(it.localPath) }
                    .forEach { trackDownloadDao.deleteTrack(it.audiobookId, it.trackIndex) }
                val referenced =
                    buildSet {
                        rows.forEach { row ->
                            row.localPath?.let { add(it) }
                            add(fileStore.tempFile(row.audiobookId, row.trackIndex).absolutePath)
                        }
                    }
                fileStore.deleteOrphanedFiles(referenced, now() - ORPHAN_AGE_MILLIS)
            }
        }

        private suspend fun deleteRowsFirst(
            deleteRows: suspend () -> Unit,
            freeSpace: suspend () -> Unit,
        ) {
            try {
                deleteRows()
            } catch (_: SQLiteFullException) {
                freeSpace()
                deleteRows()
                return
            }
            freeSpace()
        }

        private suspend fun cancelUnfinished(
            audiobookId: String,
            trackIndexes: List<Int>,
        ) {
            trackIndexes.forEach { trackIndex ->
                if (trackDownloadDao.deleteUnfinishedTrack(audiobookId, trackIndex) > 0) {
                    fileStore.deleteTrackFiles(audiobookId, trackIndex)
                }
            }
        }

        private suspend fun rescheduleActive(audiobookId: String) {
            val active = trackDownloadDao.getForBook(audiobookId).filter { it.isActive }
            if (active.isEmpty()) return
            scheduler.scheduleBook(
                audiobookId = audiobookId,
                trackIndexes = active.map { it.trackIndex },
                wifiOnly = active.any { it.wifiOnly },
            )
        }

        private suspend fun isStaleIdentity(row: TrackDownloadEntity): Boolean {
            val stored = row.trackIno ?: return false
            val catalogUrl =
                audiobookDetailDao.getTrackRemoteUrl(row.audiobookId, row.trackIndex)
                    ?: return false
            val catalogIno = inoFromUrl(catalogUrl) ?: return false
            return stored != catalogIno
        }

        private val TrackDownloadEntity.isActive: Boolean
            get() = status == DownloadStatus.QUEUED.name || status == DownloadStatus.DOWNLOADING.name

        private fun List<TrackDownloadEntity>.toSummaryOrNull(audiobookId: String): BookDownloadSummary? {
            if (isEmpty()) return null
            val statuses = map { it.toDomain().status }
            return BookDownloadSummary(
                audiobookId = audiobookId,
                totalTracks = size,
                completedTracks = statuses.count { it == DownloadStatus.COMPLETED },
                activeTracks = statuses.count { it == DownloadStatus.QUEUED || it == DownloadStatus.DOWNLOADING },
                failedTracks = statuses.count { it == DownloadStatus.FAILED },
                bytesDownloaded = sumOf { it.bytesDownloaded },
                totalBytes = takeIf { all -> all.all { it.totalBytes != null } }?.sumOf { it.totalBytes!! },
                status = BookDownloadSummary.aggregateStatus(statuses),
            )
        }

        private fun now(): Long = System.currentTimeMillis()

        private companion object {
            const val ORPHAN_AGE_MILLIS = 3_600_000L
            const val STALE_DOWNLOADING_MILLIS = 600_000L
        }
    }
