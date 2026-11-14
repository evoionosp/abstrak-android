package org.evoionosp.abstrak.data.download.local.dao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.data.test.testTrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackDownloadDaoTest {
    private lateinit var database: AbstrakDatabase
    private lateinit var dao: TrackDownloadDao

    @Before
    fun setUp() {
        database = TestDatabase.create()
        dao = database.trackDownloadDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert and observe round-trips rows in track order`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 2),
                    testTrackDownloadEntity(trackIndex = 0),
                    testTrackDownloadEntity(trackIndex = 1),
                ),
            )
            assertEquals(
                listOf(0, 1, 2),
                dao.observeForBook("book-1").first().map { it.trackIndex },
            )
        }

    @Test
    fun `upsert replaces the row`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.QUEUED))
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.FAILED))
            assertEquals(
                DownloadStatus.FAILED.name,
                dao.getTrack("book-1", 0)?.status,
            )
        }

    @Test
    fun `updateProgress writes bytes and total`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.DOWNLOADING))
            assertEquals(1, dao.updateProgress("book-1", 0, 512L, 1024L, 7L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(512L, row.bytesDownloaded)
            assertEquals(1024L, row.totalBytes)
            assertEquals(7L, row.updatedAtMillis)
        }

    @Test
    fun `markFailed only fails downloading rows`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                ),
            )
            assertEquals(1, dao.markFailed("book-1", 0, 9L))
            assertEquals(0, dao.markFailed("book-1", 1, 9L))
            assertEquals(0, dao.markFailed("book-1", 9, 9L))
            assertEquals(
                listOf("FAILED", "QUEUED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `markFailedAndClearBytes fails downloading rows and drops their bytes`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        trackIndex = 0,
                        status = DownloadStatus.DOWNLOADING,
                        bytesDownloaded = 50L,
                    ),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                ),
            )
            assertEquals(1, dao.markFailedAndClearBytes("book-1", 0, 9L))
            assertEquals(0, dao.markFailedAndClearBytes("book-1", 1, 9L))
            assertEquals(0, dao.markFailedAndClearBytes("book-1", 9, 9L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(DownloadStatus.FAILED.name, row.status)
            assertEquals(0L, row.bytesDownloaded)
            assertEquals(9L, row.updatedAtMillis)
        }

    @Test
    fun `clearBytesDownloaded drops bytes without changing status`() =
        runTest {
            dao.upsert(
                testTrackDownloadEntity(
                    status = DownloadStatus.FAILED,
                    bytesDownloaded = 50L,
                ),
            )
            assertEquals(1, dao.clearBytesDownloaded("book-1", 0, 9L))
            assertEquals(0, dao.clearBytesDownloaded("book-1", 9, 9L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(DownloadStatus.FAILED.name, row.status)
            assertEquals(0L, row.bytesDownloaded)
            assertEquals(9L, row.updatedAtMillis)
        }

    @Test
    fun `markComplete sets the finished fields`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.DOWNLOADING))
            assertEquals(1, dao.markComplete("book-1", 0, 100L, 100L, "/root/track_0.mp3", 11L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(DownloadStatus.COMPLETED.name, row.status)
            assertEquals(100L, row.bytesDownloaded)
            assertEquals(100L, row.totalBytes)
            assertEquals("/root/track_0.mp3", row.localPath)
        }

    @Test
    fun `markComplete accepts a requeued row but not a failed one`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.FAILED),
                ),
            )
            assertEquals(1, dao.markComplete("book-1", 0, 100L, 100L, "/root/track_0.mp3", 11L))
            assertEquals(0, dao.markComplete("book-1", 1, 100L, 100L, "/root/track_1.mp3", 11L))
            assertEquals(
                listOf("COMPLETED", "FAILED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `failActive fails only active rows`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.COMPLETED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.FAILED),
                ),
            )
            dao.failActive("book-1", 13L)
            assertEquals(
                listOf("FAILED", "FAILED", "COMPLETED", "FAILED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `deleteUnfinishedTrack deletes any row but a completed one`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.FAILED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.COMPLETED),
                ),
            )
            assertEquals(1, dao.deleteUnfinishedTrack("book-1", 0))
            assertEquals(1, dao.deleteUnfinishedTrack("book-1", 2))
            assertEquals(0, dao.deleteUnfinishedTrack("book-1", 3))
            assertEquals(0, dao.deleteUnfinishedTrack("book-1", 9))
            assertEquals(
                listOf(1 to "QUEUED", 3 to "COMPLETED"),
                dao.getForBook("book-1").map { it.trackIndex to it.status },
            )
        }

    @Test
    fun `requeueDownloading returns downloading rows to queued`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.COMPLETED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.FAILED),
                ),
            )
            dao.requeueDownloading("book-1", 13L)
            assertEquals(
                listOf("QUEUED", "COMPLETED", "QUEUED", "FAILED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `requeueStaleDownloading heals only stale downloading rows`() =
        runTest {
            val now = System.currentTimeMillis()
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        trackIndex = 0,
                        status = DownloadStatus.DOWNLOADING,
                        updatedAtMillis = now - 3_600_000L,
                    ),
                    testTrackDownloadEntity(
                        trackIndex = 1,
                        status = DownloadStatus.DOWNLOADING,
                        updatedAtMillis = now,
                    ),
                    testTrackDownloadEntity(
                        trackIndex = 2,
                        status = DownloadStatus.FAILED,
                        updatedAtMillis = now - 3_600_000L,
                    ),
                    testTrackDownloadEntity(
                        audiobookId = "book-2",
                        status = DownloadStatus.DOWNLOADING,
                        updatedAtMillis = now - 3_600_000L,
                    ),
                ),
            )
            assertEquals(2, dao.requeueStaleDownloading(now - 600_000L, now))
            assertEquals(
                listOf("QUEUED", "DOWNLOADING", "FAILED"),
                dao.getForBook("book-1").map { it.status },
            )
            assertEquals("QUEUED", dao.getTrack("book-2", 0)?.status)
        }

    @Test
    fun `insertIgnore keeps existing rows`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.FAILED))
            dao.insertIgnore(listOf(testTrackDownloadEntity(status = DownloadStatus.QUEUED)))
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `requeueTracks revives failed rows and leaves running and finished ones`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.FAILED),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.COMPLETED),
                ),
            )
            dao.requeueTracks("book-1", listOf(0, 1, 2, 3), wifiOnly = false, updatedAtMillis = 5L)
            assertEquals(
                listOf("QUEUED", "QUEUED", "DOWNLOADING", "COMPLETED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `requeueCompleted clears a finished row for a fresh fetch`() =
        runTest {
            dao.upsert(
                testTrackDownloadEntity(
                    status = DownloadStatus.COMPLETED,
                    bytesDownloaded = 100L,
                    totalBytes = 100L,
                    localPath = "/root/track_0.mp3",
                    etag = "\"v1\"",
                    trackIno = "old-ino",
                ),
            )
            assertEquals(1, dao.requeueCompleted("book-1", 0, 5L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(DownloadStatus.QUEUED.name, row.status)
            assertEquals(0L, row.bytesDownloaded)
            assertNull(row.totalBytes)
            assertNull(row.localPath)
            assertNull(row.etag)
        }

    @Test
    fun `requeueCompleted ignores rows that are not completed`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.FAILED))
            assertEquals(0, dao.requeueCompleted("book-1", 0, 5L))
            assertEquals(0, dao.requeueCompleted("book-1", 9, 5L))
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `releaseClaim returns a downloading row to queued`() =
        runTest {
            dao.upsert(
                testTrackDownloadEntity(
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 40L,
                    totalBytes = 100L,
                ),
            )
            assertEquals(1, dao.releaseClaim("book-1", 0, 5L))
            val row = dao.getTrack("book-1", 0) as TrackDownloadEntity
            assertEquals(DownloadStatus.QUEUED.name, row.status)
            assertEquals(40L, row.bytesDownloaded)
            assertEquals(100L, row.totalBytes)
        }

    @Test
    fun `releaseClaim ignores rows that are not downloading`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.FAILED))
            assertEquals(0, dao.releaseClaim("book-1", 0, 5L))
            assertEquals(0, dao.releaseClaim("book-1", 9, 5L))
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `claimForDownload only claims runnable rows`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.FAILED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.COMPLETED),
                ),
            )
            assertEquals(1, dao.claimForDownload("book-1", 0, "ino-9", 5L))
            assertEquals(1, dao.claimForDownload("book-1", 1, "ino-9", 5L))
            assertEquals(0, dao.claimForDownload("book-1", 2, "ino-9", 5L))
            assertEquals(0, dao.claimForDownload("book-1", 3, "ino-9", 5L))
            assertEquals(0, dao.claimForDownload("book-1", 9, null, 5L))
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.DOWNLOADING.name, row?.status)
            assertEquals("ino-9", row?.trackIno)
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 2)?.status)
        }

    @Test
    fun `progress only touches downloading rows`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.FAILED),
                ),
            )
            assertEquals(1, dao.updateProgress("book-1", 0, 10L, 100L, 5L))
            assertEquals(0, dao.updateProgress("book-1", 1, 10L, 100L, 5L))
            assertEquals(0, dao.updateProgress("book-1", 2, 10L, 100L, 5L))
            assertEquals(0, dao.updateProgress("book-1", 9, 10L, 100L, 5L))
        }

    @Test
    fun `updateValidators stores server validators for downloading rows`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                ),
            )
            assertEquals(1, dao.updateValidators("book-1", 0, "\"v1\"", "Tue, 01 Jan 2030 00:00:00 GMT"))
            assertEquals(0, dao.updateValidators("book-1", 1, "\"v1\"", "Tue, 01 Jan 2030 00:00:00 GMT"))
            val row = dao.getTrack("book-1", 0)
            assertEquals("\"v1\"", row?.etag)
            assertEquals("Tue, 01 Jan 2030 00:00:00 GMT", row?.lastModified)
        }

    @Test
    fun `getTrack returns null when missing`() =
        runTest {
            assertNull(dao.getTrack("book-1", 0))
        }

    @Test
    fun `deleteTrack removes one row`() =
        runTest {
            dao.upsertAll(listOf(testTrackDownloadEntity(trackIndex = 0), testTrackDownloadEntity(trackIndex = 1)))
            dao.deleteTrack("book-1", 0)
            assertEquals(listOf(1), dao.getForBook("book-1").map { it.trackIndex })
        }

    @Test
    fun `deleteBook removes the book and keeps others`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(audiobookId = "book-1"),
                    testTrackDownloadEntity(audiobookId = "book-2"),
                ),
            )
            dao.deleteBook("book-1")
            assertEquals(emptyList<TrackDownloadEntity>(), dao.getForBook("book-1"))
            assertEquals(1, dao.getForBook("book-2").size)
        }

    @Test
    fun `deleteAll empties the table`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(audiobookId = "book-1"),
                    testTrackDownloadEntity(audiobookId = "book-2"),
                ),
            )
            dao.deleteAll()
            assertEquals(emptyList<TrackDownloadEntity>(), dao.observeAll().first())
        }
}
