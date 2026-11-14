package org.evoionosp.abstrak.data.download.work
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.remote.RemoteFileDownloader
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.test.MockWebServerRule
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.data.test.testTrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackBatchDownloaderTest {
    @get:Rule
    val server = MockWebServerRule()

    @get:Rule
    val tempDir = TemporaryFolder()
    private lateinit var database: AbstrakDatabase
    private lateinit var dao: TrackDownloadDao
    private lateinit var fileStore: DownloadFileStore
    private lateinit var batcher: TrackBatchDownloader

    @Before
    fun setUp() {
        database = TestDatabase.create()
        dao = database.trackDownloadDao()
        fileStore = DownloadFileStore(tempDir.root)
        batcher =
            TrackBatchDownloader(
                trackDownloadDao = dao,
                audiobookDetailDao = database.audiobookDetailDao(),
                fileStore = fileStore,
                downloader = RemoteFileDownloader(OkHttpClient(), Dispatchers.Unconfined),
                bookLocks = DownloadBookLocks(),
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun ioBatcher(bookLocks: DownloadBookLocks = DownloadBookLocks()) =
        TrackBatchDownloader(
            trackDownloadDao = dao,
            audiobookDetailDao = database.audiobookDetailDao(),
            fileStore = fileStore,
            downloader = RemoteFileDownloader(OkHttpClient(), Dispatchers.IO),
            bookLocks = bookLocks,
            ioDispatcher = Dispatchers.IO,
        )

    private suspend fun seedCatalog() {
        database.audiobookDetailDao().upsertDetail(
            AudiobookDetailEntity(
                audiobookId = "book-1",
                libraryId = "lib-1",
                title = "Dune",
                author = "Frank Herbert",
                coverUrl = "https://example.com/cover.jpg",
                series = null,
                durationInSeconds = 3600L,
                description = null,
                refreshedAtMillis = 0L,
            ),
        )
        database.audiobookDetailDao().upsertTracks(
            listOf(
                trackEntity(0, server.baseUrl() + "file0"),
                trackEntity(1, server.baseUrl() + "file1"),
            ),
        )
    }

    private fun trackEntity(
        index: Int,
        remoteUrl: String,
    ) = AudiobookTrackEntity(
        audiobookId = "book-1",
        trackIndex = index,
        startOffsetInSeconds = index * 100L,
        durationInSeconds = 100L,
        title = "Track $index",
        remoteUrl = remoteUrl,
        mimeType = "audio/mpeg",
    )

    @Test
    fun `downloads each track and marks complete`() =
        runTest {
            seedCatalog()
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0),
                    testTrackDownloadEntity(trackIndex = 1),
                ),
            )
            server.enqueuePlainText(200, "first-track-bytes")
            server.enqueuePlainText(200, "second-track-bytes")
            val started = mutableListOf<Pair<Int, Int>>()
            val result =
                batcher.downloadBatch("book-1", listOf(0, 1), tokenProvider = { "tok" }) { done, total ->
                    started += done to total
                }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(
                listOf(DownloadStatus.COMPLETED, DownloadStatus.COMPLETED),
                dao.getForBook("book-1").map { DownloadStatus.valueOf(it.status) },
            )
            assertEquals("first-track-bytes", fileStore.trackFile("book-1", 0, "mp3").readText())
            assertEquals("second-track-bytes", fileStore.trackFile("book-1", 1, "mp3").readText())
            assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), started)
            assertEquals("Bearer tok", server.takeRequest().getHeader("Authorization"))
        }

    @Test
    fun `requests the download route`() =
        runTest {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            server.enqueuePlainText(200, "bytes")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertTrue(server.takeRequest().path!!.endsWith("/download"))
        }

    @Test
    fun `skips completed tracks with files without network calls`() =
        runTest {
            seedCatalog()
            val finished = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            finished.writeText("already-here")
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.COMPLETED,
                    localPath = finished.absolutePath,
                ),
            )
            dao.upsert(testTrackDownloadEntity(trackIndex = 1))
            server.enqueuePlainText(200, "second-track-bytes")
            batcher.downloadBatch("book-1", listOf(0, 1), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(1, server.server.requestCount)
            assertEquals("second-track-bytes", fileStore.trackFile("book-1", 1, "mp3").readText())
        }

    @Test
    fun `skips completed rows whose files are gone without network calls`() =
        runTest {
            seedCatalog()
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.COMPLETED,
                    localPath = "/nope/track_0.mp3",
                ),
            )
            val result =
                batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(0, server.server.requestCount)
            assertEquals(DownloadStatus.COMPLETED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `skips unrunnable rows and fetches the rest`() =
        runTest {
            seedCatalog()
            val failedTemp = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            failedTemp.writeText("partial")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        trackIndex = 0,
                        status = DownloadStatus.FAILED,
                        bytesDownloaded = 7L,
                    ),
                    testTrackDownloadEntity(trackIndex = 1),
                ),
            )
            server.enqueuePlainText(200, "second-track-bytes")
            val result =
                batcher.downloadBatch("book-1", listOf(0, 1), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(1, server.server.requestCount)
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
            assertEquals(7L, dao.getTrack("book-1", 0)?.bytesDownloaded)
            assertEquals("partial", failedTemp.readText())
            assertEquals("second-track-bytes", fileStore.trackFile("book-1", 1, "mp3").readText())
        }

    @Test
    fun `records failure and continues with the rest`() =
        runTest {
            seedCatalog()
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0),
                    testTrackDownloadEntity(trackIndex = 1),
                ),
            )
            server.enqueuePlainText(404, "gone")
            server.enqueuePlainText(200, "second-track-bytes")
            val result =
                batcher.downloadBatch("book-1", listOf(0, 1), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(
                listOf(DownloadStatus.FAILED, DownloadStatus.COMPLETED),
                dao.getForBook("book-1").map { DownloadStatus.valueOf(it.status) },
            )
            assertEquals("second-track-bytes", fileStore.trackFile("book-1", 1, "mp3").readText())
        }

    @Test
    fun `releases network failures back to queued for retry`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, "http://127.0.0.1:9/file0")),
            )
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            val result =
                batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = true, aborted = false), result)
            assertEquals(DownloadStatus.QUEUED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `fails network failures loudly once retries are exhausted`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, "http://127.0.0.1:9/file0")),
            )
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            val result =
                batcher.downloadBatch(
                    audiobookId = "book-1",
                    trackIndexes = listOf(0),
                    tokenProvider = { "tok" },
                    runAttemptCount = 5,
                ) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `releases unexpected failures back to queued and continues`() =
        runTest {
            seedCatalog()
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0),
                    testTrackDownloadEntity(trackIndex = 1),
                ),
            )
            server.enqueuePlainText(200, "second-track-bytes")
            var tokenCalls = 0
            val result =
                batcher.downloadBatch(
                    audiobookId = "book-1",
                    trackIndexes = listOf(0, 1),
                    tokenProvider = {
                        tokenCalls++
                        if (tokenCalls == 1) throw IllegalStateException("boom") else "tok"
                    },
                ) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = true, aborted = false), result)
            assertEquals(
                listOf(DownloadStatus.QUEUED, DownloadStatus.COMPLETED),
                dao.getForBook("book-1").map { DownloadStatus.valueOf(it.status) },
            )
        }

    @Test
    fun `fails unexpected failures loudly once retries are exhausted`() =
        runTest {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            val result =
                batcher.downloadBatch(
                    audiobookId = "book-1",
                    trackIndexes = listOf(0),
                    tokenProvider = { throw IllegalStateException("boom") },
                    runAttemptCount = 5,
                ) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `a batch waits for the previous batch of the same book`() =
        runBlocking {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            server.enqueuePlainText(200, "first-track-bytes")
            val locks = DownloadBookLocks()
            val lock = locks.lockFor("book-1")
            lock.lock()
            val batch =
                async(Dispatchers.IO) {
                    ioBatcher(locks).downloadBatch("book-1", listOf(0), tokenProvider = { "tok" })
                }
            delay(300)
            assertEquals(0, server.server.requestCount)
            lock.unlock()
            assertEquals(
                BatchResult(retryNeeded = false, aborted = false),
                withTimeout(5_000) { batch.await() },
            )
            assertEquals(DownloadStatus.COMPLETED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `prunes rows for tracks missing from the catalog`() =
        runTest {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 9))
            batcher.downloadBatch("book-1", listOf(9), tokenProvider = { "tok" }) { _, _ -> }
            assertTrue(dao.getForBook("book-1").isEmpty())
            assertEquals(0, server.server.requestCount)
        }

    @Test
    fun `failed rows keep their wifiOnly flag`() =
        runTest {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, wifiOnly = true))
            server.enqueuePlainText(404, "gone")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(true, row?.wifiOnly)
        }

    @Test
    fun `skips tracks whose rows are gone without touching the network`() =
        runTest {
            seedCatalog()
            val result =
                batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(0, server.server.requestCount)
            assertNull(dao.getTrack("book-1", 0))
        }

    @Test
    fun `fails active rows and aborts without a token`() =
        runTest {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED))
            dao.upsert(testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING))
            val result =
                batcher.downloadBatch("book-1", listOf(0, 1), tokenProvider = { null }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = false, aborted = true), result)
            assertEquals(0, server.server.requestCount)
            assertEquals(
                listOf(DownloadStatus.FAILED, DownloadStatus.FAILED),
                dao.getForBook("book-1").map { DownloadStatus.valueOf(it.status) },
            )
        }

    @Test
    fun `persists validators and revalidates resumes`() =
        runTest {
            seedCatalog()
            val resumeTemp = fileStore.tempFile("book-1", 0)
            resumeTemp.parentFile?.mkdirs()
            resumeTemp.writeText("HEAD-")
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING, etag = "\"v1\""))
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10")
                    .addHeader("ETag", "\"v2\""),
            )
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals("\"v1\"", server.takeRequest().getHeader("If-Range"))
            assertEquals("\"v2\"", dao.getTrack("book-1", 0)?.etag)
            assertEquals("HEAD-ABCDE", fileStore.trackFile("book-1", 0, "mp3").readText())
        }

    @Test
    fun `rejects resumed totals that disagree with the stored total`() =
        runTest {
            seedCatalog()
            val resumeTemp = fileStore.tempFile("book-1", 0)
            resumeTemp.parentFile?.mkdirs()
            resumeTemp.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("T".repeat(150))
                    .addHeader("Content-Range", "bytes 50-199/200"),
            )
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(DownloadStatus.FAILED.name, dao.getTrack("book-1", 0)?.status)
            assertFalse(fileStore.tempFile("book-1", 0).exists())
            assertFalse(fileStore.trackFile("book-1", 0, "mp3").exists())
        }

    @Test
    fun `corrupt resume reports zero bytes after discarding the part`() =
        runTest {
            seedCatalog()
            val resumeTemp = fileStore.tempFile("book-1", 0)
            resumeTemp.parentFile?.mkdirs()
            resumeTemp.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("T".repeat(150))
                    .addHeader("Content-Range", "bytes 50-199/200"),
            )
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(0L, row?.bytesDownloaded)
            assertFalse(fileStore.tempFile("book-1", 0).exists())
            assertFalse(fileStore.trackFile("book-1", 0, "mp3").exists())
        }

    @Test
    fun `stale failed row reports zero bytes after its files are dropped`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, server.baseUrl() + "file/new-ino")),
            )
            val staleTemp = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            staleTemp.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.FAILED,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                    trackIno = "old-ino",
                ),
            )
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(0L, row?.bytesDownloaded)
            assertFalse(fileStore.tempFile("book-1", 0).exists())
            assertEquals(0, server.server.requestCount)
        }

    @Test
    fun `stale queued row reports zero bytes when its refetch fails`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, server.baseUrl() + "file/new-ino")),
            )
            val staleTemp = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            staleTemp.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                    trackIno = "old-ino",
                ),
            )
            server.enqueuePlainText(404, "gone")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(0L, row?.bytesDownloaded)
            assertFalse(fileStore.tempFile("book-1", 0).exists())
        }

    @Test
    fun `failed fetch with no part file reports zero bytes`() =
        runTest {
            seedCatalog()
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            server.enqueuePlainText(500, "boom")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(0L, row?.bytesDownloaded)
            assertFalse(fileStore.tempFile("book-1", 0).exists())
        }

    @Test
    fun `failed fetch with a kept part keeps its bytes`() =
        runTest {
            seedCatalog()
            val part = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            part.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            server.enqueuePlainText(500, "boom")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.FAILED.name, row?.status)
            assertEquals(50L, row?.bytesDownloaded)
            assertEquals("H".repeat(50), part.readText())
        }

    @Test
    fun `retryable failure with no part file releases the row with zero bytes`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, "http://127.0.0.1:9/file0")),
            )
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            val result =
                batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = true, aborted = false), result)
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.QUEUED.name, row?.status)
            assertEquals(0L, row?.bytesDownloaded)
        }

    @Test
    fun `retryable failure with a kept part releases the row with its bytes`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, "http://127.0.0.1:9/file0")),
            )
            val part = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            part.writeText("H".repeat(50))
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 50L,
                    totalBytes = 100L,
                ),
            )
            val result =
                batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals(BatchResult(retryNeeded = true, aborted = false), result)
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.QUEUED.name, row?.status)
            assertEquals(50L, row?.bytesDownloaded)
            assertEquals("H".repeat(50), part.readText())
        }

    @Test
    fun `refetches tracks whose server file changed`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, server.baseUrl() + "file/new-ino")),
            )
            val stale = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            stale.writeText("old-file-bytes")
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.COMPLETED,
                    localPath = stale.absolutePath,
                    trackIno = "old-ino",
                ),
            )
            server.enqueuePlainText(200, "new-file-bytes")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals("new-file-bytes", fileStore.trackFile("book-1", 0, "mp3").readText())
            assertEquals("new-ino", dao.getTrack("book-1", 0)?.trackIno)
            assertEquals(DownloadStatus.COMPLETED.name, dao.getTrack("book-1", 0)?.status)
        }

    @Test
    fun `stores the file identity on claim`() =
        runTest {
            seedCatalog()
            database.audiobookDetailDao().upsertTracks(
                listOf(trackEntity(0, server.baseUrl() + "file/ino-7")),
            )
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            server.enqueuePlainText(200, "bytes")
            batcher.downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> }
            assertEquals("ino-7", dao.getTrack("book-1", 0)?.trackIno)
        }

    @Test
    fun `a user cancel mid-stream drops the part and skips the track`() =
        runBlocking(Dispatchers.IO) {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("B".repeat(50_000))
                    .throttleBody(1024, 100, TimeUnit.MILLISECONDS),
            )
            val run = async { ioBatcher().downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> } }
            val temp = fileStore.tempFile("book-1", 0)
            withTimeout(15_000) {
                while (temp.length() < 1024) delay(25)
            }
            assertEquals(1, dao.deleteUnfinishedTrack("book-1", 0))
            val result = withTimeout(30_000) { run.await() }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertNull(dao.getTrack("book-1", 0))
            assertFalse(temp.exists())
            assertFalse(fileStore.trackFile("book-1", 0, "mp3").exists())
        }

    @Test
    fun `a system stop mid-stream returns the row to queued and keeps the part`() =
        runBlocking(Dispatchers.IO) {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            val body = "B".repeat(50_000)
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(body)
                    .throttleBody(1024, 100, TimeUnit.MILLISECONDS),
            )
            val run = async { ioBatcher().downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> } }
            val temp = fileStore.tempFile("book-1", 0)
            withTimeout(15_000) {
                while (temp.length() < 1024) delay(25)
            }
            withTimeout(15_000) { run.cancelAndJoin() }
            assertEquals(DownloadStatus.QUEUED.name, dao.getTrack("book-1", 0)?.status)
            val partial = temp.length()
            assertTrue("expected a partial file, got $partial bytes", partial in 1 until 50_000)
        }

    @Test
    fun `reclaims a row returned to queued mid-stream and completes`() =
        runBlocking(Dispatchers.IO) {
            seedCatalog()
            dao.upsert(testTrackDownloadEntity(trackIndex = 0))
            val body = "B".repeat(50_000)
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(body)
                    .throttleBody(1024, 100, TimeUnit.MILLISECONDS),
            )
            val run = async { ioBatcher().downloadBatch("book-1", listOf(0), tokenProvider = { "tok" }) { _, _ -> } }
            val temp = fileStore.tempFile("book-1", 0)
            withTimeout(15_000) {
                while (temp.length() < 1024) delay(25)
            }
            assertEquals(1, dao.releaseClaim("book-1", 0, System.currentTimeMillis()))
            val result = withTimeout(30_000) { run.await() }
            assertEquals(BatchResult(retryNeeded = false, aborted = false), result)
            assertEquals(DownloadStatus.COMPLETED.name, dao.getTrack("book-1", 0)?.status)
            assertEquals(body, fileStore.trackFile("book-1", 0, "mp3").readText())
        }

    @Test
    fun `downloadUrl appends the permission-checked route`() {
        assertEquals(
            "https://example.com/api/items/x/file/1/download",
            downloadUrl("https://example.com/api/items/x/file/1"),
        )
        assertEquals(
            "https://example.com/api/items/x/file/1/download?t=1",
            downloadUrl("https://example.com/api/items/x/file/1?t=1"),
        )
        assertEquals(
            "https://example.com/api/items/x/file/1/download",
            downloadUrl("https://example.com/api/items/x/file/1/download"),
        )
    }

    @Test
    fun `inoFromUrl parses the file identity`() {
        assertEquals("123", inoFromUrl("https://example.com/api/items/x/file/123"))
        assertEquals("123", inoFromUrl("https://example.com/api/items/x/file/123/download?t=1"))
        assertNull(inoFromUrl("https://example.com/audio/1.mp3"))
        assertNull(inoFromUrl("not a url"))
    }
}
