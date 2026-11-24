package org.evoionosp.abstrak.data.download.repository
import android.database.sqlite.SQLiteFullException
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.work.DownloadWorkScheduler
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.storage.StorageMeter
import org.evoionosp.abstrak.data.test.FakeDownloadWorkScheduler
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.data.test.testTrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadRepositoryImplTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @get:Rule
    val cacheDir = TemporaryFolder()
    private lateinit var database: AbstrakDatabase
    private lateinit var dao: TrackDownloadDao
    private lateinit var scheduler: FakeDownloadWorkScheduler
    private lateinit var repository: DownloadRepositoryImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        dao = database.trackDownloadDao()
        scheduler = FakeDownloadWorkScheduler()
        repository =
            DownloadRepositoryImpl(
                trackDownloadDao = dao,
                audiobookDetailDao = database.audiobookDetailDao(),
                fileStore = DownloadFileStore(tempDir.root),
                storageMeter = StorageMeter(cacheDir = cacheDir.root, downloadRoot = tempDir.root),
                scheduler = scheduler,
                ioDispatcher = Dispatchers.Unconfined,
                applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedTrackUrls(vararg urls: Pair<Int, String>) {
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
            urls.map { (index, url) ->
                AudiobookTrackEntity(
                    audiobookId = "book-1",
                    trackIndex = index,
                    startOffsetInSeconds = index * 100L,
                    durationInSeconds = 100L,
                    title = "Track $index",
                    remoteUrl = url,
                    mimeType = "audio/mpeg",
                )
            },
        )
    }

    @Test
    fun `enqueueTracks inserts queued rows and schedules fresh ones`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED))
            repository.enqueueTracks("book-1", listOf(0, 1, 2), wifiOnly = false)
            assertEquals(
                listOf("COMPLETED", "QUEUED", "QUEUED"),
                dao.getForBook("book-1").map { it.status },
            )
            assertEquals(
                listOf(FakeDownloadWorkScheduler.Scheduled("book-1", listOf(1, 2), false)),
                scheduler.scheduled,
            )
        }

    @Test
    fun `pauseDownloads cancels every batch and leaves the tracks queued`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED))
            repository.pauseDownloads()
            assertEquals(1, scheduler.cancelAllCount)
            assertEquals(listOf("QUEUED"), dao.getForBook("book-1").map { it.status })
        }

    @Test
    fun `resumeDownloads reschedules each book's queued and interrupted tracks only`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.QUEUED))
            dao.upsert(testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING))
            dao.upsert(testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.COMPLETED))
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-2", status = DownloadStatus.FAILED))
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-3", status = DownloadStatus.QUEUED, wifiOnly = true))
            repository.resumeDownloads()
            assertEquals(
                listOf(
                    FakeDownloadWorkScheduler.Scheduled("book-1", listOf(0, 1), false),
                    FakeDownloadWorkScheduler.Scheduled("book-3", listOf(0), true),
                ),
                scheduler.scheduled.sortedBy { it.audiobookId },
            )
        }

    @Test
    fun `enqueueTracks ignores empty input`() =
        runTest {
            repository.enqueueTracks("book-1", emptyList(), wifiOnly = false)
            assertEquals(emptyList<String>(), dao.getForBook("book-1").map { it.status })
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `enqueueTracks schedules nothing when all are complete`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED))
            repository.enqueueTracks("book-1", listOf(0), wifiOnly = false)
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `enqueueTracks on any network lets the book's Wi-Fi-only downloads use any network too`() =
        runTest {
            repository.enqueueTracks("book-1", listOf(0, 1), wifiOnly = true)
            repository.enqueueTracks("book-1", listOf(2), wifiOnly = false)
            assertEquals(listOf(false, false, false), dao.getForBook("book-1").map { it.wifiOnly })
            assertEquals(listOf("book-1" to listOf(0, 1, 2)), scheduler.allowedAnyNetwork)
            assertEquals(
                listOf(
                    FakeDownloadWorkScheduler.Scheduled("book-1", listOf(0, 1), true),
                    FakeDownloadWorkScheduler.Scheduled("book-1", listOf(2), false),
                ),
                scheduler.scheduled,
            )
        }

    @Test
    fun `enqueueTracks on any network unblocks a track it asks for again`() =
        runTest {
            repository.enqueueTracks("book-1", listOf(0), wifiOnly = true)
            repository.enqueueTracks("book-1", listOf(0), wifiOnly = false)
            assertEquals(listOf(false), dao.getForBook("book-1").map { it.wifiOnly })
            assertEquals(listOf("book-1" to listOf(0)), scheduler.allowedAnyNetwork)
        }

    @Test
    fun `enqueueTracks over Wi-Fi only leaves the book's other downloads as they are`() =
        runTest {
            repository.enqueueTracks("book-1", listOf(0), wifiOnly = false)
            repository.enqueueTracks("book-1", listOf(1), wifiOnly = true)
            assertEquals(listOf(false, true), dao.getForBook("book-1").map { it.wifiOnly })
            assertEquals(emptyList<Pair<String, List<Int>>>(), scheduler.allowedAnyNetwork)
        }

    @Test
    fun `enqueueTracks leaves running rows and their progress alone`() =
        runTest {
            dao.upsert(
                testTrackDownloadEntity(
                    trackIndex = 0,
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = 512L,
                    totalBytes = 1024L,
                ),
            )
            repository.enqueueTracks("book-1", listOf(0), wifiOnly = false)
            val row = dao.getTrack("book-1", 0)
            assertEquals(DownloadStatus.DOWNLOADING.name, row?.status)
            assertEquals(512L, row?.bytesDownloaded)
            assertEquals(1024L, row?.totalBytes)
        }

    @Test
    fun `cancelBook keeps finished tracks and clears unfinished ones with their partial files`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val streaming = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            streaming.writeText("partial")
            val failedPart = fileStore.tempFile("book-1", 2)
            failedPart.writeText("stale")
            val finished = fileStore.trackFile("book-1", 3, "mp3")
            finished.writeText("audio")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING, bytesDownloaded = 7),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.FAILED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.COMPLETED, localPath = finished.absolutePath),
                ),
            )
            repository.cancelBook("book-1")
            assertEquals(listOf("book-1"), scheduler.cancelledBooks)
            assertEquals(
                listOf(3 to "COMPLETED"),
                dao.getForBook("book-1").map { it.trackIndex to it.status },
            )
            assertFalse(streaming.exists())
            assertFalse(failedPart.exists())
            assertEquals("audio", finished.readText())
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `cancelTracks clears only the named unfinished tracks and leaves the batch running`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val cancelled = fileStore.tempFile("book-1", 1).apply { parentFile?.mkdirs() }
            cancelled.writeText("partial")
            val untouched = fileStore.tempFile("book-1", 2)
            untouched.writeText("other")
            val finished = fileStore.trackFile("book-1", 0, "mp3")
            finished.writeText("audio")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = finished.absolutePath),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING, bytesDownloaded = 7),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.DOWNLOADING),
                ),
            )
            repository.cancelTracks("book-1", listOf(0, 1))
            assertEquals(
                listOf(0 to "COMPLETED", 2 to "DOWNLOADING"),
                dao.getForBook("book-1").map { it.trackIndex to it.status },
            )
            assertFalse(cancelled.exists())
            assertEquals("other", untouched.readText())
            assertEquals("audio", finished.readText())
            assertEquals(emptyList<String>(), scheduler.cancelledBooks)
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `cancelTracks ignores empty input`() =
        runTest {
            repository.cancelTracks("book-1", emptyList())
            assertEquals(emptyList<String>(), scheduler.cancelledBooks)
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `retryBook re-enqueues failures honoring wifiOnly`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.FAILED, wifiOnly = true),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.FAILED),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.COMPLETED),
                ),
            )
            repository.retryBook("book-1")
            assertEquals(
                listOf(FakeDownloadWorkScheduler.Scheduled("book-1", listOf(0, 1), true)),
                scheduler.scheduled,
            )
            assertEquals(
                listOf("QUEUED", "QUEUED", "COMPLETED"),
                dao.getForBook("book-1").map { it.status },
            )
        }

    @Test
    fun `retryBook with no failures schedules nothing`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.COMPLETED))
            repository.retryBook("book-1")
            assertEquals(emptyList<FakeDownloadWorkScheduler.Scheduled>(), scheduler.scheduled)
        }

    @Test
    fun `deleteBook cancels, wipes files and rows`() =
        runTest {
            dao.upsert(testTrackDownloadEntity())
            val file = DownloadFileStore(tempDir.root).trackFile("book-1", 0, "mp3")
            file.parentFile?.mkdirs()
            file.writeText("audio")
            repository.deleteBook("book-1")
            assertEquals(listOf("book-1"), scheduler.cancelledBooks)
            assertEquals(false, File(tempDir.root, "book-1").exists())
            assertEquals(emptyList<String>(), dao.getForBook("book-1").map { it.status })
        }

    @Test
    fun `deleteAll cancels all work, wipes files and rows across books`() =
        runTest {
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-1"))
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-2", trackIndex = 1))
            val fileStore = DownloadFileStore(tempDir.root)
            val file = fileStore.trackFile("book-1", 0, "mp3")
            file.parentFile?.mkdirs()
            file.writeText("audio")
            repository.deleteAll()
            assertEquals(1, scheduler.cancelAllCount)
            assertEquals(false, File(tempDir.root, "book-1").exists())
            assertEquals(emptyList<String>(), dao.getAll().map { it.audiobookId })
        }

    @Test
    fun `deleteBook frees space and retries its row delete on a full disk`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val file = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            file.writeText("audio")
            dao.upsert(testTrackDownloadEntity())
            var filesGoneWhenRetried = false
            var cancelledWhenRetried = false
            val repository =
                repositoryWith(
                    FullDiskDao(dao) {
                        filesGoneWhenRetried = !file.exists()
                        cancelledWhenRetried = scheduler.cancelledBooks.contains("book-1")
                    },
                    fileStore,
                )
            repository.deleteBook("book-1")
            assertTrue(filesGoneWhenRetried)
            assertTrue(cancelledWhenRetried)
            assertEquals(listOf("book-1"), scheduler.cancelledBooks)
            assertFalse(file.exists())
            assertFalse(File(tempDir.root, "book-1").exists())
            assertEquals(emptyList<String>(), dao.getForBook("book-1").map { it.status })
        }

    @Test
    fun `deleteAll frees space and retries its row delete on a full disk`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val file = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            file.writeText("audio")
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-1"))
            dao.upsert(testTrackDownloadEntity(audiobookId = "book-2", trackIndex = 1))
            var filesGoneWhenRetried = false
            var cancelledWhenRetried = false
            val repository =
                repositoryWith(
                    FullDiskDao(dao) {
                        filesGoneWhenRetried = !file.exists()
                        cancelledWhenRetried = scheduler.cancelAllCount == 1
                    },
                    fileStore,
                )
            repository.deleteAll()
            assertTrue(filesGoneWhenRetried)
            assertTrue(cancelledWhenRetried)
            assertEquals(1, scheduler.cancelAllCount)
            assertFalse(file.exists())
            assertEquals(emptyList<String>(), dao.getAll().map { it.audiobookId })
        }

    @Test
    fun `deleteBook propagates when the retry also hits a full disk`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val file = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            file.writeText("audio")
            dao.upsert(testTrackDownloadEntity())
            val repository = repositoryWith(FullDiskDao(dao, failTimes = 2), fileStore)
            try {
                repository.deleteBook("book-1")
                fail("expected the retry to throw SQLiteFullException")
            } catch (expected: SQLiteFullException) {
            }
            assertEquals(listOf("book-1"), scheduler.cancelledBooks)
            assertFalse(file.exists())
            assertEquals(1, dao.getForBook("book-1").size)
        }

    private fun repositoryWith(
        trackDao: TrackDownloadDao,
        fileStore: DownloadFileStore,
    ) = DownloadRepositoryImpl(
        trackDownloadDao = trackDao,
        audiobookDetailDao = database.audiobookDetailDao(),
        fileStore = fileStore,
        storageMeter = StorageMeter(cacheDir = cacheDir.root, downloadRoot = tempDir.root),
        scheduler = scheduler,
        ioDispatcher = Dispatchers.Unconfined,
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

    @Test
    fun `deleteBook of an in-flight book drops its rows before stopping the work and clears partial files`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val finished = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            finished.writeText("audio")
            val partial = fileStore.tempFile("book-1", 1)
            partial.writeText("partial")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = finished.absolutePath),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.DOWNLOADING, bytesDownloaded = 7),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.QUEUED),
                ),
            )
            var rowsWhenStopped: Int? = null
            val orderingScheduler =
                object : DownloadWorkScheduler {
                    override fun scheduleBook(
                        audiobookId: String,
                        trackIndexes: List<Int>,
                        wifiOnly: Boolean,
                    ) = Unit

                    override suspend fun allowAnyNetwork(
                        audiobookId: String,
                        trackIndexes: List<Int>,
                    ) = Unit

                    override fun cancelBook(audiobookId: String) {
                        rowsWhenStopped = runBlocking { dao.getForBook(audiobookId).size }
                    }

                    override fun cancelAll() = Unit
                }
            val repository =
                DownloadRepositoryImpl(
                    trackDownloadDao = dao,
                    audiobookDetailDao = database.audiobookDetailDao(),
                    fileStore = fileStore,
                    storageMeter = StorageMeter(cacheDir = cacheDir.root, downloadRoot = tempDir.root),
                    scheduler = orderingScheduler,
                    ioDispatcher = Dispatchers.Unconfined,
                    applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
                )
            repository.deleteBook("book-1")
            assertEquals(0, rowsWhenStopped)
            assertFalse(partial.exists())
            assertFalse(finished.exists())
            assertEquals(emptyList<Int>(), dao.getForBook("book-1").map { it.trackIndex })
        }

    @Test
    fun `deleteBook finishes even when its caller is cancelled mid-delete`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val finished = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            finished.writeText("audio")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = finished.absolutePath),
                ),
            )
            val dispatcher = StandardTestDispatcher(testScheduler)
            val applicationJob = SupervisorJob()
            val repository =
                DownloadRepositoryImpl(
                    trackDownloadDao = dao,
                    audiobookDetailDao = database.audiobookDetailDao(),
                    fileStore = fileStore,
                    storageMeter = StorageMeter(cacheDir = cacheDir.root, downloadRoot = tempDir.root),
                    scheduler = scheduler,
                    ioDispatcher = dispatcher,
                    applicationScope = CoroutineScope(applicationJob + dispatcher),
                )
            val caller = launch(dispatcher) { repository.deleteBook("book-1") }
            runCurrent()
            caller.cancel()
            applicationJob.children.toList().joinAll()
            assertEquals(emptyList<Int>(), dao.getForBook("book-1").map { it.trackIndex })
            assertEquals(listOf("book-1"), scheduler.cancelledBooks)
            assertFalse(finished.exists())
        }

    @Test
    fun `observeDownloadedBooks pairs each downloaded book's detail with its rollup, by title`() =
        runTest {
            seedDetail(audiobookId = "book-b", libraryId = "lib-1", title = "dune")
            seedDetail(audiobookId = "book-a", libraryId = "lib-2", title = "Children of Dune")
            seedDetail(audiobookId = "book-c", libraryId = "lib-1", title = "Never downloaded")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        audiobookId = "book-b",
                        trackIndex = 0,
                        status = DownloadStatus.COMPLETED,
                        bytesDownloaded = 100,
                        totalBytes = 100,
                    ),
                    testTrackDownloadEntity(
                        audiobookId = "book-b",
                        trackIndex = 1,
                        status = DownloadStatus.DOWNLOADING,
                        bytesDownloaded = 40,
                        totalBytes = 100,
                    ),
                    testTrackDownloadEntity(audiobookId = "book-a", status = DownloadStatus.COMPLETED, bytesDownloaded = 7),
                ),
            )
            val books = repository.observeDownloadedBooks().first()
            assertEquals(listOf("book-a", "book-b"), books.map { it.audiobook.id })
            assertEquals(listOf("lib-2", "lib-1"), books.map { it.audiobook.libraryId })
            assertEquals(listOf("Children of Dune", "dune"), books.map { it.audiobook.title })
            assertEquals(listOf(7L, 140L), books.map { it.summary.bytesDownloaded })
            assertEquals(listOf(DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING), books.map { it.summary.status })
        }

    @Test
    fun `observeDownloadedBooks drops a book once its download is deleted`() =
        runTest {
            seedDetail(audiobookId = "book-1", libraryId = "lib-1", title = "Dune")
            dao.upsert(testTrackDownloadEntity(status = DownloadStatus.COMPLETED, bytesDownloaded = 5))
            repository.observeDownloadedBooks().test {
                assertEquals(listOf("book-1"), awaitItem().map { it.audiobook.id })
                repository.deleteBook("book-1")
                assertEquals(emptyList<String>(), awaitItem().map { it.audiobook.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun seedDetail(
        audiobookId: String,
        libraryId: String,
        title: String,
    ) {
        database.audiobookDetailDao().upsertDetail(
            AudiobookDetailEntity(
                audiobookId = audiobookId,
                libraryId = libraryId,
                title = title,
                author = "Frank Herbert",
                coverUrl = "https://example.com/$audiobookId.jpg",
                series = null,
                durationInSeconds = 3600L,
                description = null,
                refreshedAtMillis = 0L,
            ),
        )
    }

    @Test
    fun `deleteTrack removes one file and row and reschedules the rest`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.DOWNLOADING),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                ),
            )
            val fileStore = DownloadFileStore(tempDir.root)
            val doomed = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            doomed.writeText("a")
            val survivor = fileStore.trackFile("book-1", 1, "mp3")
            survivor.writeText("b")
            repository.deleteTrack("book-1", 0)
            assertEquals(emptyList<String>(), scheduler.cancelledBooks)
            assertEquals(false, doomed.exists())
            assertEquals("b", survivor.readText())
            assertEquals(listOf(1), dao.getForBook("book-1").map { it.trackIndex })
            assertEquals(DownloadStatus.QUEUED.name, dao.getTrack("book-1", 1)?.status)
            assertEquals(
                listOf(FakeDownloadWorkScheduler.Scheduled("book-1", listOf(1), false)),
                scheduler.scheduled,
            )
        }

    @Test
    fun `localFileForTrack returns only finished existing files`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val present = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            present.writeText("audio")
            val empty = fileStore.trackFile("book-1", 3, "mp3").apply { writeBytes(ByteArray(0)) }
            val directory = File(tempDir.root, "book-1/track_4.mp3").apply { mkdirs() }
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = present.absolutePath),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.COMPLETED, localPath = "/nope/track_1.mp3"),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.QUEUED),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.COMPLETED, localPath = empty.absolutePath),
                    testTrackDownloadEntity(trackIndex = 4, status = DownloadStatus.COMPLETED, localPath = directory.absolutePath),
                ),
            )
            assertEquals(present.absolutePath, repository.localFileForTrack("book-1", 0))
            assertNull(repository.localFileForTrack("book-1", 1))
            assertNull(repository.localFileForTrack("book-1", 2))
            assertNull(repository.localFileForTrack("book-1", 3))
            assertNull(repository.localFileForTrack("book-1", 4))
            assertNull(repository.localFileForTrack("book-1", 9))
        }

    @Test
    fun `localFileForTrack hides files whose server identity changed`() =
        runTest {
            seedTrackUrls(
                0 to "https://example.com/api/items/x/file/ino-new",
                1 to "https://example.com/api/items/x/file/ino-same",
            )
            val fileStore = DownloadFileStore(tempDir.root)
            val stale = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            stale.writeText("stale")
            val fresh = fileStore.trackFile("book-1", 1, "mp3")
            fresh.writeText("fresh")
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        trackIndex = 0,
                        status = DownloadStatus.COMPLETED,
                        localPath = stale.absolutePath,
                        trackIno = "ino-old",
                    ),
                    testTrackDownloadEntity(
                        trackIndex = 1,
                        status = DownloadStatus.COMPLETED,
                        localPath = fresh.absolutePath,
                        trackIno = "ino-same",
                    ),
                ),
            )
            assertNull(repository.localFileForTrack("book-1", 0))
            assertEquals(fresh.absolutePath, repository.localFileForTrack("book-1", 1))
        }

    @Test
    fun `reconcileStorage prunes missing-file rows and old orphans`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val present = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            present.writeText("audio")
            val protectedTemp = fileStore.tempFile("book-1", 3)
            protectedTemp.writeText("partial")
            protectedTemp.setLastModified(System.currentTimeMillis() - 2 * 3_600_000L)
            val oldOrphan = File(fileStore.bookDir("book-1"), "track_9.mp3").apply { writeText("orphan") }
            oldOrphan.setLastModified(System.currentTimeMillis() - 2 * 3_600_000L)
            val freshOrphan =
                File(fileStore.bookDir("book-1"), "track_8.mp3").apply { writeText("fresh") }
            val ghostDir = File(tempDir.root, "ghost").apply { mkdirs() }
            File(ghostDir, "track_0.mp3").apply {
                writeText("ghost")
                setLastModified(System.currentTimeMillis() - 2 * 3_600_000L)
            }
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = present.absolutePath),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.COMPLETED, localPath = "/nope/track_1.mp3"),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.COMPLETED, localPath = null),
                    testTrackDownloadEntity(trackIndex = 3, status = DownloadStatus.QUEUED),
                ),
            )
            repository.reconcileStorage()
            assertEquals(listOf(0, 3), dao.getForBook("book-1").map { it.trackIndex })
            assertTrue(present.exists())
            assertTrue(protectedTemp.exists())
            assertFalse(oldOrphan.exists())
            assertTrue(freshOrphan.exists())
            assertFalse(ghostDir.exists())
        }

    @Test
    fun `reconcileStorage prunes completed rows whose files can't be played`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val present = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            present.writeText("audio")
            val empty = fileStore.trackFile("book-1", 1, "mp3").apply { writeBytes(ByteArray(0)) }
            val dir = File(tempDir.root, "book-1/track_2.mp3").apply { mkdirs() }
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, localPath = present.absolutePath),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.COMPLETED, localPath = empty.absolutePath),
                    testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.COMPLETED, localPath = dir.absolutePath),
                ),
            )
            repository.reconcileStorage()
            assertEquals(listOf(0), dao.getForBook("book-1").map { it.trackIndex })
        }

    @Test
    fun `reconcileStorage heals stale downloading rows`() =
        runTest {
            val now = System.currentTimeMillis()
            val fileStore = DownloadFileStore(tempDir.root)
            val temp = fileStore.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
            temp.writeText("partial")
            temp.setLastModified(now - 2 * 3_600_000L)
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(
                        trackIndex = 0,
                        status = DownloadStatus.DOWNLOADING,
                        bytesDownloaded = 7L,
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
                ),
            )
            repository.reconcileStorage()
            assertEquals(
                listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.FAILED),
                dao.getForBook("book-1").map { DownloadStatus.valueOf(it.status) },
            )
            assertEquals(7L, dao.getTrack("book-1", 0)?.bytesDownloaded)
            assertEquals("partial", temp.readText())
        }

    @Test
    fun `observeBookSummary aggregates and nulls when empty`() =
        runTest {
            repository.observeBookSummary("book-1").test {
                assertEquals(null, awaitItem())
                dao.upsertAll(
                    listOf(
                        testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, bytesDownloaded = 100, totalBytes = 100),
                        testTrackDownloadEntity(
                            trackIndex = 1,
                            status = DownloadStatus.DOWNLOADING,
                            bytesDownloaded = 40,
                            totalBytes = 100,
                        ),
                        testTrackDownloadEntity(trackIndex = 2, status = DownloadStatus.FAILED, bytesDownloaded = 10, totalBytes = 100),
                    ),
                )
                val summary = awaitItem()
                assertEquals(3, summary?.totalTracks)
                assertEquals(1, summary?.completedTracks)
                assertEquals(1, summary?.activeTracks)
                assertEquals(1, summary?.failedTracks)
                assertEquals(150L, summary?.bytesDownloaded)
                assertEquals(300L, summary?.totalBytes)
                assertEquals(DownloadStatus.DOWNLOADING, summary?.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeBookSummary reports null total while any size is unknown`() =
        runTest {
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, status = DownloadStatus.COMPLETED, bytesDownloaded = 100, totalBytes = 100),
                    testTrackDownloadEntity(trackIndex = 1, status = DownloadStatus.QUEUED),
                ),
            )
            assertNull(repository.observeBookSummary("book-1").first()?.totalBytes)
        }

    @Test
    fun `observeStorageUsage sums download bytes and measures the cache`() =
        runTest {
            File(cacheDir.root, "image_cache").apply { mkdirs() }.resolve("cover").writeBytes(ByteArray(40))
            dao.upsertAll(
                listOf(
                    testTrackDownloadEntity(trackIndex = 0, bytesDownloaded = 100),
                    testTrackDownloadEntity(trackIndex = 1, bytesDownloaded = 200),
                ),
            )
            val usage = repository.observeStorageUsage().first()
            assertEquals(300L, usage.downloadsBytes)
            assertEquals(40L, usage.cacheBytes)
            assertTrue(usage.deviceFreeBytes >= 0)
        }

    @Test
    fun `observeStorageUsage measures free space again once Clear all has removed the files`() =
        runTest {
            val fileStore = DownloadFileStore(tempDir.root)
            val file = fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }
            file.writeText("audio")
            val trackDao =
                mockk<TrackDownloadDao>(relaxed = true) {
                    every { observeAll() } returns
                        MutableStateFlow(
                            listOf(
                                testTrackDownloadEntity(
                                    status = DownloadStatus.COMPLETED,
                                    bytesDownloaded = 5,
                                    localPath = file.absolutePath,
                                ),
                            ),
                        )
                }
            val storageMeter =
                mockk<StorageMeter> {
                    every { cacheBytes() } returns 0L
                    every { deviceFreeBytes() } answers { if (file.exists()) 100L else 105L }
                }
            val repository =
                DownloadRepositoryImpl(
                    trackDownloadDao = trackDao,
                    audiobookDetailDao = database.audiobookDetailDao(),
                    fileStore = fileStore,
                    storageMeter = storageMeter,
                    scheduler = scheduler,
                    ioDispatcher = Dispatchers.Unconfined,
                    applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
                )
            repository.observeStorageUsage().test {
                assertEquals(StorageUsage(downloadsBytes = 5, cacheBytes = 0, deviceFreeBytes = 100), awaitItem())
                repository.deleteAll()
                assertEquals(StorageUsage(downloadsBytes = 5, cacheBytes = 0, deviceFreeBytes = 105), awaitItem())
            }
        }

    @Test
    fun `observeStorageUsage follows the downloads but measures the cache once per collection`() =
        runTest {
            repository.observeStorageUsage().test {
                assertEquals(0L, awaitItem().cacheBytes)
                File(cacheDir.root, "cover").writeBytes(ByteArray(40))
                dao.upsert(testTrackDownloadEntity(bytesDownloaded = 10))
                val next = awaitItem()
                assertEquals(10L, next.downloadsBytes)
                assertEquals(0L, next.cacheBytes)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(40L, repository.observeStorageUsage().first().cacheBytes)
        }

    @Test
    fun `observeTrackDownloads emits rows in order`() =
        runTest {
            dao.upsertAll(listOf(testTrackDownloadEntity(trackIndex = 1), testTrackDownloadEntity(trackIndex = 0)))
            assertEquals(
                listOf(0, 1),
                repository.observeTrackDownloads("book-1").first().map { it.trackIndex },
            )
        }
}

private class FullDiskDao(
    private val delegate: TrackDownloadDao,
    private val failTimes: Int = 1,
    private val onRetry: () -> Unit = {},
) : TrackDownloadDao by delegate {
    private var deleteBookCalls = 0
    private var deleteAllCalls = 0

    override suspend fun deleteBook(audiobookId: String) {
        if (++deleteBookCalls <= failTimes) throw SQLiteFullException("database or disk is full")
        onRetry()
        delegate.deleteBook(audiobookId)
    }

    override suspend fun deleteAll() {
        if (++deleteAllCalls <= failTimes) throw SQLiteFullException("database or disk is full")
        onRetry()
        delegate.deleteAll()
    }
}
