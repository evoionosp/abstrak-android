package org.evoionosp.abstrak.data.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.test.FakeDownloadWorkScheduler
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.data.test.testTrackDownloadEntity
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.junit.After
import org.junit.Assert.assertEquals
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
class DownloadStoreImplTest {
    @get:Rule
    val tempDir = TemporaryFolder()
    private lateinit var database: AbstrakDatabase
    private lateinit var scheduler: FakeDownloadWorkScheduler
    private lateinit var store: DownloadStoreImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        scheduler = FakeDownloadWorkScheduler()
        store =
            DownloadStoreImpl(
                trackDownloadDao = database.trackDownloadDao(),
                fileStore = DownloadFileStore(tempDir.root),
                scheduler = scheduler,
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `deleteAll stops work, wipes files and rows`() =
        runTest {
            database.trackDownloadDao().upsertAll(
                listOf(
                    testTrackDownloadEntity(audiobookId = "book-1", status = DownloadStatus.COMPLETED),
                    testTrackDownloadEntity(audiobookId = "book-2", status = DownloadStatus.QUEUED),
                ),
            )
            val fileStore = DownloadFileStore(tempDir.root)
            fileStore.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }.writeText("audio")
            store.deleteAll()
            assertEquals(1, scheduler.cancelAllCount)
            assertEquals(
                emptyList<String>(),
                database
                    .trackDownloadDao()
                    .observeAll()
                    .first()
                    .map { it.audiobookId },
            )
            assertTrue(tempDir.root.listFiles().isNullOrEmpty())
        }
}
