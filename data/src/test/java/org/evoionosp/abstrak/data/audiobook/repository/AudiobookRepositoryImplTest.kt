package org.evoionosp.abstrak.data.audiobook.repository
import android.database.sqlite.SQLiteFullException
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.evoionosp.abstrak.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.abstrak.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.abstrak.data.test.FakeConnectivityObserver
import org.evoionosp.abstrak.data.test.MockWebServerRule
import org.evoionosp.abstrak.data.test.TestDatabase
import org.evoionosp.abstrak.data.test.testManagedClients
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudiobookRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()
    private val testDispatcher = StandardTestDispatcher()
    private val connectivity = FakeConnectivityObserver()
    private lateinit var database: AbstrakDatabase
    private lateinit var repository: AudiobookRepositoryImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        repository =
            AudiobookRepositoryImpl(
                database = database,
                audiobookDao = database.audiobookDao(),
                audiobookDetailDao = database.audiobookDetailDao(),
                continueListeningDao = database.continueListeningDao(),
                pendingProgressDao = database.pendingProgressDao(),
                syncStateDao = database.librarySyncStateDao(),
                serviceFactory = AudiobookshelfLibraryServiceFactory(testManagedClients()),
                connectivityObserver = connectivity,
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `refreshAudiobooks offline fails and marks sync failed without cache`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE), result)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("FAILED", sync.status)
            assertEquals("CONNECTIVITY_UNAVAILABLE", sync.error)
        }

    @Test
    fun `refreshAudiobooks offline marks sync stale when cache exists`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE), result)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("STALE", sync.status)
        }

    @Test
    fun `refreshAudiobooks success replaces library items and marks sync success`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1")}, ${bookJson(id = "item2")}]}""",
            )
            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            val cached = database.audiobookDao().getAudiobooks("lib1")
            assertEquals(listOf("item1", "item2"), cached.map { it.id })
            assertEquals("Frank Herbert", cached.first().author)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("SUCCESS", sync.status)
            assertNotNull(sync.lastSyncedAtMillis)
        }

    @Test
    fun `refreshAudiobooks propagates auth errors without cache fallback`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(401, "{}")
            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale-token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Failure(CatalogError.AUTH), result)
        }

    @Test
    fun `refreshAudiobookDetail caches detail chapters and tracks`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, fullBookJson())
            val result =
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            repository.observeAudiobookDetail("lib1", "item1").filterNotNull().test {
                val detail = awaitItem()
                assertEquals("Dune", detail.audiobook.title)
                assertEquals("Desert epic.", detail.description)
                assertEquals(listOf("One", "Two"), detail.chapters.map { it.title })
                assertEquals(listOf(0, 1), detail.tracks.map { it.index })
                assertTrue(
                    detail.tracks
                        .single { it.index == 0 }
                        .remoteUrl
                        .endsWith("/audio/1.mp3"),
                )
            }
        }

    @Test
    fun `refreshAudiobookDetail maps not found and rejects non-books`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "missing",
                ),
            )
            serverRule.enqueueJson(200, bookJson(id = "pod1", mediaType = "podcast"))
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "pod1",
                ),
            )
        }

    @Test
    fun `refreshContinueListening caches only the requested library`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [
                    ${bookJson(id = "item1", libraryId = "lib1")},
                    ${bookJson(id = "item9", libraryId = "lib2")}
                ]}""",
            )
            val result =
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            repository.observeContinueListening("lib1").test {
                assertEquals(listOf("item1"), awaitItem().map { it.id })
            }
            repository.observeContinueListening("lib2").test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `refreshContinueListening persists the reported playback position`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1", currentTime = 120.5)}]}""",
            )
            val result =
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            repository.observeContinueListening("lib1").test {
                val item = awaitItem().single()
                assertEquals("item1", item.id)
                assertEquals(120.5, item.progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshContinueListening hydrates positions missing from the list response`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1")}]}""",
            )
            serverRule.enqueueJson(
                200,
                """{"currentTime": 120.5, "duration": 600.0, "isFinished": false}""",
            )
            val result =
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            repository.observeContinueListening("lib1").test {
                assertEquals(120.5, awaitItem().single().progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshContinueListening tolerates failed per-item progress`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1")}]}""",
            )
            serverRule.enqueueJson(404, "{}")
            val result =
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )
            assertEquals(DomainResult.Success(Unit), result)
            repository.observeContinueListening("lib1").test {
                assertNull(awaitItem().single().progressSeconds)
            }
        }

    @Test
    fun `refreshContinueListening keeps newer queued progress than the server's`() =
        runTest(testDispatcher) {
            database.pendingProgressDao().upsert(queued("item1", seconds = 600.0, savedAtMillis = 5_000L))
            serverRule.enqueueJson(200, """{"results": [${bookJson(id = "item1", currentTime = 120.5)}]}""")
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertEquals(600.0, awaitItem().single().progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshContinueListening takes the server's more recent progress than the queued`() =
        runTest(testDispatcher) {
            database.pendingProgressDao().upsert(queued("item1", seconds = 60.0))
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1", currentTime = 120.5, progressLastUpdate = 2_000L)}]}""",
            )
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertEquals(120.5, awaitItem().single().progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshContinueListening keeps a book started offline the server has not heard of`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            database.pendingProgressDao().upsert(queued("cached", seconds = 42.0, savedAtMillis = Long.MAX_VALUE))
            serverRule.enqueueJson(200, """{"results": [${bookJson(id = "item1", currentTime = 120.5)}]}""")
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                val shelf = awaitItem()
                assertEquals(listOf("cached", "item1"), shelf.map { it.id })
                assertEquals(42.0, shelf.first().progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshContinueListening keeps a book whose progress failed to load over an older queued finish`() =
        runTest(testDispatcher) {
            database.pendingProgressDao().upsert(
                queued("item1", seconds = 7_260.0, isFinished = true, savedAtMillis = 1_000L),
            )
            serverRule.enqueueJson(200, """{"results": [${bookJson(id = "item1", progressLastUpdate = 9_000L)}]}""")
            serverRule.enqueueJson(500, "{}")
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertEquals(listOf("item1"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `refreshContinueListening leaves a book finished on this device off the shelf`() =
        runTest(testDispatcher) {
            database.pendingProgressDao().upsert(queued("item1", seconds = 7_260.0, isFinished = true))
            serverRule.enqueueJson(200, """{"results": [${bookJson(id = "item1", currentTime = 120.5)}]}""")
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `refreshContinueListening ignores progress queued in another library`() =
        runTest(testDispatcher) {
            database.pendingProgressDao().upsert(queued("item1", seconds = 600.0, libraryId = "lib2"))
            serverRule.enqueueJson(200, """{"results": [${bookJson(id = "item1", currentTime = 120.5)}]}""")
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertEquals(120.5, awaitItem().single().progressSeconds!!, 0.001)
            }
        }

    @Test
    fun `refreshAudiobooks keeps ebooks apart from audiobooks`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [
                    ${minifiedJson(id = "audio", numTracks = 2, duration = 600.0)},
                    ${minifiedJson(id = "ebook", numTracks = 0, duration = 0.0)}
                ]}""",
            )
            repository.refreshAudiobooks(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeAudiobooks("lib1").test {
                val books = awaitItem().associateBy { it.id }
                assertTrue(books.getValue("audio").hasAudio)
                assertFalse(books.getValue("ebook").hasAudio)
            }
        }

    @Test
    fun `refreshContinueListening leaves ebooks in progress off the shelf`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [
                    ${minifiedJson(id = "audio", numTracks = 2, duration = 600.0, currentTime = 30.0)},
                    ${minifiedJson(id = "ebook", numTracks = 0, duration = 0.0, currentTime = 0.0)}
                ]}""",
            )
            repository.refreshContinueListening(baseUrl = serverRule.baseUrl(), accessToken = "token", libraryId = "lib1")
            repository.observeContinueListening("lib1").test {
                assertEquals(listOf("audio"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `recordListeningProgress moves the played book to the front of the shelf while offline`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            database.audiobookDao().upsertAudiobooks(
                listOf(cachedBook().copy(id = "older"), cachedBook().copy(id = "played")),
            )
            database.continueListeningDao().upsert(
                listOf(ContinueListeningEntity("older", "lib1", progressLastUpdateMillis = 1_000L, currentTimeSeconds = 5.0)),
            )
            repository.observeContinueListening("lib1").test {
                assertEquals(listOf("older"), awaitItem().map { it.id })
                repository.recordListeningProgress("lib1", "played", positionSeconds = 42.0, isFinished = false)
                val shelf = awaitItem()
                assertEquals(listOf("played", "older"), shelf.map { it.id })
                assertEquals(42.0, shelf.first().progressSeconds!!, 0.001)
                repository.recordListeningProgress("lib1", "older", positionSeconds = 6.0, isFinished = false)
                assertEquals(listOf("older", "played"), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `recordListeningProgress takes a finished book off the shelf`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            database.continueListeningDao().upsert(
                listOf(ContinueListeningEntity("cached", "lib1", progressLastUpdateMillis = 1_000L, currentTimeSeconds = 5.0)),
            )
            repository.recordListeningProgress("lib1", "cached", positionSeconds = 9.0, isFinished = true)
            repository.observeContinueListening("lib1").test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `recordListeningProgress survives a failed cache write`() =
        runTest(testDispatcher) {
            val fullDiskDao = mockk<ContinueListeningDao>()
            coEvery { fullDiskDao.upsert(any()) } throws SQLiteFullException("disk full")
            val repository =
                AudiobookRepositoryImpl(
                    database = database,
                    audiobookDao = database.audiobookDao(),
                    audiobookDetailDao = database.audiobookDetailDao(),
                    continueListeningDao = fullDiskDao,
                    pendingProgressDao = database.pendingProgressDao(),
                    syncStateDao = database.librarySyncStateDao(),
                    serviceFactory = AudiobookshelfLibraryServiceFactory(testManagedClients()),
                    connectivityObserver = connectivity,
                    ioDispatcher = testDispatcher,
                )
            repository.recordListeningProgress("lib1", "cached", positionSeconds = 9.0, isFinished = false)
            coVerify { fullDiskDao.upsert(any()) }
        }

    @Test
    fun `fetchProgress maps server progress`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"currentTime": 120.5, "duration": 600.0, "isFinished": false, "lastUpdate": 1700000000000}""",
            )
            val result =
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                )
            assertEquals(
                DomainResult.Success(PlaybackProgress(120.5, 600.0, false, lastUpdateMillis = 1_700_000_000_000L)),
                result,
            )
        }

    @Test
    fun `fetchProgress returns null progress on 404 and auth on 401`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Success(null),
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                ),
            )
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale",
                    audiobookId = "item1",
                ),
            )
        }

    @Test
    fun `saveProgress sends when the position was recorded as lastUpdate`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, "{}")
            serverRule.enqueueJson(200, "{}")
            repository.saveProgress(
                baseUrl = serverRule.baseUrl(),
                accessToken = "token",
                audiobookId = "item1",
                progress = PlaybackProgress(150.0, 300.0, false, lastUpdateMillis = 1_700_000_000_000L),
            )
            assertEquals(1_700_000_000_000L, JSONObject(serverRule.takeRequest().body.readUtf8()).getLong("lastUpdate"))
            repository.saveProgress(
                baseUrl = serverRule.baseUrl(),
                accessToken = "token",
                audiobookId = "item1",
                progress = PlaybackProgress(150.0, 300.0, false),
            )
            assertFalse(JSONObject(serverRule.takeRequest().body.readUtf8()).has("lastUpdate"))
        }

    @Test
    fun `saveProgress posts derived ratio and honors offline`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, "{}")
            val result =
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(150.0, 300.0, false),
                )
            assertEquals(DomainResult.Success(Unit), result)
            val body = JSONObject(serverRule.takeRequest().body.readUtf8())
            assertEquals(150.0, body.getDouble("currentTime"), 0.0)
            assertEquals(300.0, body.getDouble("duration"), 0.0)
            assertEquals(0.5, body.getDouble("progress"), 0.0)
            assertEquals(false, body.getBoolean("isFinished"))
            connectivity.setConnected(false)
            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
        }

    @Test
    fun `observeAudiobooks emits cached books`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            repository.observeAudiobooks("lib1").test {
                assertEquals(listOf("cached"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `observeAllAudiobooks emits every library's books by title`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(
                listOf(
                    cachedBook().copy(id = "dune", libraryId = "lib1", title = "dune"),
                    cachedBook().copy(id = "children", libraryId = "lib2", title = "Children of Dune"),
                ),
            )
            repository.observeAllAudiobooks().test {
                val books = awaitItem()
                assertEquals(listOf("children", "dune"), books.map { it.id })
                assertEquals(listOf("lib2", "lib1"), books.map { it.libraryId })
            }
        }

    @Test
    fun `observeAudiobook emits the requested book or null`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            repository.observeAudiobook("lib1", "cached").filterNotNull().test {
                assertEquals("Cached", awaitItem().title)
            }
            repository.observeAudiobook("lib1", "missing").test {
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun `observeLibrarySyncStatus maps stored state`() =
        runTest(testDispatcher) {
            repository.observeLibrarySyncStatus("lib1").test {
                assertEquals(SyncStatus.Idle, awaitItem())
            }
            database.librarySyncStateDao().upsert(
                LibrarySyncStateEntity("lib1", "SUCCESS", 1L, null),
            )
            repository.observeLibrarySyncStatus("lib1").test {
                assertEquals(SyncStatus.Success(1L), awaitItem())
            }
        }

    @Test
    fun `refreshAudiobookDetail maps transport failures`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
            connectivity.setConnected(true)
            serverRule.enqueueJson(200, bookJson(id = ""))
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshAudiobookDetail(
                    baseUrl = "",
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.refreshAudiobookDetail(
                    baseUrl = deadUrl,
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
        }

    @Test
    fun `refreshAudiobookDetail maps http errors`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
            serverRule.enqueueJson(500, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                ),
            )
        }

    @Test
    fun `refreshAudiobooks maps unexpected errors and marks sync failed`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(500, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            val failed = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("FAILED", failed.status)
            assertEquals("UNKNOWN", failed.error)
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshAudiobooks(
                    baseUrl = "",
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.refreshAudiobooks(
                    baseUrl = deadUrl,
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
        }

    @Test
    fun `refreshAudiobooks marks sync stale on unexpected failures with cache`() =
        runTest(testDispatcher) {
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))
            val failingFactory = mockk<AudiobookshelfLibraryServiceFactory>()
            every { failingFactory.create(any()) } throws IllegalStateException("boom")
            val failingRepository =
                AudiobookRepositoryImpl(
                    database = database,
                    audiobookDao = database.audiobookDao(),
                    audiobookDetailDao = database.audiobookDetailDao(),
                    continueListeningDao = database.continueListeningDao(),
                    pendingProgressDao = database.pendingProgressDao(),
                    syncStateDao = database.librarySyncStateDao(),
                    serviceFactory = failingFactory,
                    connectivityObserver = connectivity,
                    ioDispatcher = testDispatcher,
                )
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                failingRepository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            val stale = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("STALE", stale.status)
            assertEquals("UNKNOWN", stale.error)
        }

    @Test
    fun `refreshContinueListening maps failures`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            connectivity.setConnected(true)
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale",
                    libraryId = "lib1",
                ),
            )
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            serverRule.enqueueJson(500, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.refreshContinueListening(
                    baseUrl = "",
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.refreshContinueListening(
                    baseUrl = deadUrl,
                    accessToken = "token",
                    libraryId = "lib1",
                ),
            )
        }

    @Test
    fun `fetchProgress maps transport failures`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                ),
            )
            connectivity.setConnected(true)
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.fetchProgress(
                    baseUrl = "",
                    accessToken = "token",
                    audiobookId = "item1",
                ),
            )
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.fetchProgress(
                    baseUrl = deadUrl,
                    accessToken = "token",
                    audiobookId = "item1",
                ),
            )
        }

    @Test
    fun `saveProgress maps failures and zero duration`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, "{}")
            assertEquals(
                DomainResult.Success(Unit),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(10.0, 0.0, false),
                ),
            )
            val zeroDurationBody = JSONObject(serverRule.takeRequest().body.readUtf8())
            assertEquals(0.0, zeroDurationBody.getDouble("progress"), 0.0)
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
            serverRule.enqueueJson(500, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
            assertEquals(
                DomainResult.Failure(CatalogError.UNKNOWN),
                repository.saveProgress(
                    baseUrl = "",
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.saveProgress(
                    baseUrl = deadUrl,
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
        }

    @Test
    fun `saveProgress maps http failures from the service`() =
        runTest(testDispatcher) {
            fun repositoryThrowing(code: Int): AudiobookRepositoryImpl {
                val failingFactory = mockk<AudiobookshelfLibraryServiceFactory>()
                every { failingFactory.create(any()) } throws
                    HttpException(
                        Response.error<String>(code, "".toResponseBody("text/plain".toMediaType())),
                    )
                return AudiobookRepositoryImpl(
                    database = database,
                    audiobookDao = database.audiobookDao(),
                    audiobookDetailDao = database.audiobookDetailDao(),
                    continueListeningDao = database.continueListeningDao(),
                    pendingProgressDao = database.pendingProgressDao(),
                    syncStateDao = database.librarySyncStateDao(),
                    serviceFactory = failingFactory,
                    connectivityObserver = connectivity,
                    ioDispatcher = testDispatcher,
                )
            }

            suspend fun saveProgressWith(code: Int): DomainResult<Unit> =
                repositoryThrowing(code).saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                )
            assertEquals(DomainResult.Failure(CatalogError.AUTH), saveProgressWith(401))
            assertEquals(DomainResult.Failure(CatalogError.NOT_FOUND), saveProgressWith(404))
            assertEquals(DomainResult.Failure(CatalogError.UNKNOWN), saveProgressWith(500))
        }

    private fun queued(
        audiobookId: String,
        seconds: Double,
        libraryId: String = "lib1",
        isFinished: Boolean = false,
        savedAtMillis: Long = 1_000L,
    ) = PendingProgressEntity(
        audiobookId = audiobookId,
        libraryId = libraryId,
        currentTimeSeconds = seconds,
        durationSeconds = 7_265.0,
        isFinished = isFinished,
        savedAtMillis = savedAtMillis,
    )

    private fun cachedBook(): AudiobookEntity =
        AudiobookEntity(
            id = "cached",
            libraryId = "lib1",
            title = "Cached",
            author = "Author",
            coverUrl = "https://host/cover",
            series = null,
            durationInSeconds = 10L,
        )

    private fun bookJson(
        id: String,
        libraryId: String = "lib1",
        mediaType: String = "book",
        currentTime: Double? = null,
        progressLastUpdate: Long? = null,
    ): String {
        val progress =
            currentTime?.let { """, "userMediaProgress": {"currentTime": $it, "isFinished": false}""" }.orEmpty()
        val updated = progressLastUpdate?.let { """, "progressLastUpdate": $it""" }.orEmpty()
        return """{"id": "$id", "libraryId": "$libraryId", "mediaType": "$mediaType",
            "media": {"duration": 7265.0,
              "metadata": {"title": "Dune", "authorName": "Herbert, Frank"}}$progress$updated}"""
    }

    private fun minifiedJson(
        id: String,
        numTracks: Int,
        duration: Double,
        currentTime: Double? = null,
    ): String {
        val progress =
            currentTime?.let { """, "userMediaProgress": {"currentTime": $it, "isFinished": false}""" }.orEmpty()
        return """{"id": "$id", "libraryId": "lib1", "mediaType": "book",
            "media": {"duration": $duration, "numTracks": $numTracks,
              "metadata": {"title": "Title $id", "authorName": "Author"}}$progress}"""
    }

    private fun fullBookJson(): String =
        """{"id": "item1", "libraryId": "lib1", "mediaType": "book",
            "media": {"duration": 200.0,
              "metadata": {"title": "Dune", "authorName": "Herbert, Frank",
                "description": "Desert epic."},
              "chapters": [
                {"title": "One", "start": 0.0, "end": 100.0},
                {"title": "Two", "start": 100.0, "end": 200.0}
              ],
              "tracks": [
                {"index": 0, "startOffset": 0.0, "duration": 100.0,
                 "title": "Part 1", "contentUrl": "/audio/1.mp3", "mimeType": "audio/mpeg"},
                {"index": 1, "startOffset": 100.0, "duration": 100.0,
                 "title": "Part 2", "contentUrl": "/audio/2.mp3", "mimeType": "audio/mpeg"}
              ]}}"""
}
