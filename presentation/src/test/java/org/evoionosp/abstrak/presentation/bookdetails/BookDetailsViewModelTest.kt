package org.evoionosp.abstrak.presentation.bookdetails
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.usecase.CancelBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.CancelChapterDownloadResult
import org.evoionosp.abstrak.domain.download.usecase.CancelChapterDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DeleteBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DeleteChapterDownloadResult
import org.evoionosp.abstrak.domain.download.usecase.DeleteChapterDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DownloadBookResult
import org.evoionosp.abstrak.domain.download.usecase.DownloadBookUseCase
import org.evoionosp.abstrak.domain.download.usecase.DownloadChapterResult
import org.evoionosp.abstrak.domain.download.usecase.DownloadChapterUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveBookDownloadsUseCase
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.playback.PlaybackState
import org.evoionosp.abstrak.presentation.navigation.BOOK_DETAILS_ARG_AUDIOBOOK_ID
import org.evoionosp.abstrak.presentation.navigation.BOOK_DETAILS_ARG_LIBRARY_ID
import org.evoionosp.abstrak.presentation.ui.download.DownloadButtonStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val book =
        Audiobook(
            id = "book-1",
            libraryId = "lib-1",
            title = "Dune",
            author = "Frank Herbert",
            coverUrl = "https://example.com/cover.jpg",
            series = null,
            durationInSeconds = 300L,
        )
    private val chapters =
        listOf(
            AudiobookChapter(title = "Ch 1", startInSeconds = 0, endInSeconds = 100),
            AudiobookChapter(title = "Ch 2", startInSeconds = 100, endInSeconds = 200),
            AudiobookChapter(title = "Ch 3", startInSeconds = 200, endInSeconds = 300),
        )
    private val tracks =
        listOf(
            AudiobookTrack(0, 0, 60, "Track 0", "https://example.com/0.mp3", "audio/mpeg"),
            AudiobookTrack(1, 60, 60, "Track 1", "https://example.com/1.mp3", "audio/mpeg"),
            AudiobookTrack(2, 120, 60, "Track 2", "https://example.com/2.mp3", "audio/mpeg"),
            AudiobookTrack(3, 180, 60, "Track 3", "https://example.com/3.mp3", "audio/mpeg"),
            AudiobookTrack(4, 240, 60, "Track 4", "https://example.com/4.mp3", "audio/mpeg"),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Harness(
        val viewModel: BookDetailsViewModel,
        val playbackConnection: PlaybackConnection,
        val downloadBook: DownloadBookUseCase,
        val downloadChapter: DownloadChapterUseCase,
        val cancelBookDownload: CancelBookDownloadUseCase,
        val cancelChapterDownload: CancelChapterDownloadUseCase,
        val deleteBookDownload: DeleteBookDownloadUseCase,
        val deleteChapterDownload: DeleteChapterDownloadUseCase,
    )

    private fun download(
        trackIndex: Int,
        status: DownloadStatus,
        bytesDownloaded: Long = 0L,
        totalBytes: Long? = null,
    ) = TrackDownload(
        audiobookId = "book-1",
        trackIndex = trackIndex,
        status = status,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        localPath = null,
    )

    private fun createHarness(
        downloads: List<TrackDownload> = emptyList(),
        playback: PlaybackState = PlaybackState(),
        session: LoginSession? = null,
        refreshAudiobookDetail: RefreshAudiobookDetailUseCase = mockk(),
        cachedShelf: List<Audiobook> = emptyList(),
        serverProgress: PlaybackProgress? = null,
    ): Harness {
        val playbackConnection = mockk<PlaybackConnection>(relaxUnitFun = true)
        every { playbackConnection.playbackState } returns MutableStateFlow(playback)
        val observeAudiobookDetail = mockk<ObserveAudiobookDetailUseCase>()
        every { observeAudiobookDetail(any(), any()) } returns
            flowOf(
                AudiobookDetail(
                    audiobook = book,
                    description = null,
                    chapters = chapters,
                    tracks = tracks,
                    refreshedAtMillis = 0L,
                ),
            )
        val observeContinueListening = mockk<ObserveContinueListeningUseCase>()
        every { observeContinueListening("lib-1") } returns flowOf(cachedShelf)
        val fetchPlaybackProgress = mockk<FetchPlaybackProgressUseCase>()
        coEvery { fetchPlaybackProgress(any(), any(), any()) } returns serverProgress
        val getValidSession = mockk<GetValidSessionUseCase>()
        coEvery { getValidSession() } returns session
        val observeBookDownloads = mockk<ObserveBookDownloadsUseCase>()
        every { observeBookDownloads(any()) } returns flowOf(downloads)
        val downloadBook = mockk<DownloadBookUseCase>()
        coEvery { downloadBook(any(), any(), any(), any(), any()) } returns DownloadBookResult.Enqueued(1)
        val downloadChapter = mockk<DownloadChapterUseCase>()
        coEvery { downloadChapter(any(), any(), any(), any()) } returns DownloadChapterResult.Enqueued(1)
        val cancelBookDownload = mockk<CancelBookDownloadUseCase>()
        coEvery { cancelBookDownload(any()) } returns Unit
        val cancelChapterDownload = mockk<CancelChapterDownloadUseCase>()
        coEvery { cancelChapterDownload(any(), any(), any()) } returns CancelChapterDownloadResult.Cancelled(1)
        val deleteBookDownload = mockk<DeleteBookDownloadUseCase>()
        coEvery { deleteBookDownload(any()) } returns Unit
        val deleteChapterDownload = mockk<DeleteChapterDownloadUseCase>()
        coEvery { deleteChapterDownload(any(), any(), any()) } returns DeleteChapterDownloadResult.Deleted(1)
        val viewModel =
            BookDetailsViewModel(
                savedStateHandle =
                    SavedStateHandle(
                        mapOf(
                            BOOK_DETAILS_ARG_LIBRARY_ID to "lib-1",
                            BOOK_DETAILS_ARG_AUDIOBOOK_ID to "book-1",
                        ),
                    ),
                playbackConnection = playbackConnection,
                observeAudiobookDetail = observeAudiobookDetail,
                refreshAudiobookDetail = refreshAudiobookDetail,
                fetchPlaybackProgress = fetchPlaybackProgress,
                observeContinueListening = observeContinueListening,
                getValidSessionUseCase = getValidSession,
                observeBookDownloads = observeBookDownloads,
                downloadBook = downloadBook,
                downloadChapter = downloadChapter,
                cancelBookDownload = cancelBookDownload,
                cancelChapterDownload = cancelChapterDownload,
                deleteBookDownload = deleteBookDownload,
                deleteChapterDownload = deleteChapterDownload,
                scopeCalculator = DownloadScopeCalculator(),
            )
        return Harness(
            viewModel = viewModel,
            playbackConnection = playbackConnection,
            downloadBook = downloadBook,
            downloadChapter = downloadChapter,
            cancelBookDownload = cancelBookDownload,
            cancelChapterDownload = cancelChapterDownload,
            deleteBookDownload = deleteBookDownload,
            deleteChapterDownload = deleteChapterDownload,
        )
    }

    private suspend fun ReceiveTurbine<BookDetailsUiState>.awaitSatisfying(
        predicate: (BookDetailsUiState) -> Boolean,
    ): BookDetailsUiState {
        var item = awaitItem()
        while (!predicate(item)) item = awaitItem()
        return item
    }

    @Test
    fun `shows the cached book while the refresh is still running`() =
        runTest {
            val refresh = mockk<RefreshAudiobookDetailUseCase>()
            coEvery { refresh(any(), any(), any(), any()) } coAnswers { awaitCancellation() }
            val harness =
                createHarness(
                    session =
                        LoginSession(
                            accessToken = "token",
                            refreshToken = null,
                            userId = null,
                            username = "reader",
                            baseUrl = "https://abs.example.com",
                        ),
                    refreshAudiobookDetail = refresh,
                )
            harness.viewModel.uiState.test {
                val state = awaitSatisfying { it.audiobook != null }
                assertEquals(book, state.audiobook)
                assertEquals(chapters, state.chapters)
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { refresh(any(), any(), "lib-1", "book-1") }
        }

    @Test
    fun `a book never listened to has nothing to continue`() =
        runTest {
            val harness = createHarness()
            harness.viewModel.uiState.test {
                assertFalse(awaitSatisfying { it.audiobook != null }.hasProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the shelf's cached position counts before the server answers or offline`() =
        runTest {
            val harness = createHarness(cachedShelf = listOf(book.copy(progressSeconds = 150.0)))
            harness.viewModel.uiState.test {
                assertTrue(awaitSatisfying { it.progressSeconds == 150.0 }.hasProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the server's position wins over the cached one`() =
        runTest {
            val harness =
                createHarness(
                    session = session,
                    refreshAudiobookDetail = succeedingRefresh(),
                    cachedShelf = listOf(book.copy(progressSeconds = 150.0)),
                    serverProgress = PlaybackProgress(currentTimeSeconds = 120.0, durationSeconds = 300.0, isFinished = false),
                )
            harness.viewModel.uiState.test {
                assertTrue(awaitSatisfying { it.progressSeconds == 120.0 }.hasProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a finished book has nothing to continue even when the shelf cached it`() =
        runTest {
            val harness =
                createHarness(
                    session = session,
                    refreshAudiobookDetail = succeedingRefresh(),
                    cachedShelf = listOf(book.copy(progressSeconds = 150.0)),
                    serverProgress = PlaybackProgress(currentTimeSeconds = 300.0, durationSeconds = 300.0, isFinished = true),
                )
            harness.viewModel.uiState.test {
                val state = awaitSatisfying { it.audiobook != null }
                assertEquals(0.0, state.progressSeconds, 0.0)
                assertFalse(state.hasProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the playing book continues from its live position`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            harness.viewModel.uiState.test {
                assertTrue(awaitSatisfying { it.progressSeconds == 150.0 }.hasProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private val session =
        LoginSession(
            accessToken = "token",
            refreshToken = null,
            userId = null,
            username = "reader",
            baseUrl = "https://abs.example.com",
        )

    private fun succeedingRefresh(): RefreshAudiobookDetailUseCase {
        val refresh = mockk<RefreshAudiobookDetailUseCase>()
        coEvery { refresh(any(), any(), any(), any()) } returns DomainResult.Success(Unit)
        return refresh
    }

    @Test
    fun `continue listening resumes the playing book instead of pausing it`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, isPlaying = true))
            harness.viewModel.playBook()
            verify { harness.playbackConnection.play() }
            verify(exactly = 0) { harness.playbackConnection.pause() }
        }

    @Test
    fun `continue listening starts a book that is not active`() =
        runTest {
            val harness = createHarness()
            harness.viewModel.playBook()
            verify { harness.playbackConnection.playAudiobook(book) }
        }

    @Test
    fun `starting a book hands the player its known position to show while it loads`() =
        runTest {
            val harness = createHarness(cachedShelf = listOf(book.copy(progressSeconds = 150.0)))
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null && it.progressSeconds == 150.0 }
                harness.viewModel.playBook()
                verify { harness.playbackConnection.playAudiobook(book.copy(progressSeconds = 150.0)) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the chapter in progress continues where the book left off`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() && it.progressSeconds == 150.0 }
                harness.viewModel.playChapter(chapters[1])
                verify { harness.playbackConnection.play() }
                verify(exactly = 0) { harness.playbackConnection.seekToBookSeconds(any()) }
                assertNull(harness.viewModel.uiState.value.pendingChapterSwitch)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `another chapter asks before replacing progress and plays from its start once confirmed`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() && it.progressSeconds == 150.0 }
                harness.viewModel.playChapter(chapters[2])
                awaitSatisfying { it.pendingChapterSwitch == chapters[2] }
                verify(exactly = 0) { harness.playbackConnection.seekToBookSeconds(any()) }
                verify(exactly = 0) { harness.playbackConnection.play() }
                harness.viewModel.confirmChapterSwitch()
                awaitSatisfying { it.pendingChapterSwitch == null }
                verifyOrder {
                    harness.playbackConnection.seekToBookSeconds(200.0)
                    harness.playbackConnection.play()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dismissing the chapter confirmation keeps the progress`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() && it.progressSeconds == 150.0 }
                harness.viewModel.playChapter(chapters[0])
                awaitSatisfying { it.pendingChapterSwitch == chapters[0] }
                harness.viewModel.dismissChapterSwitch()
                awaitSatisfying { it.pendingChapterSwitch == null }
                verify(exactly = 0) { harness.playbackConnection.seekToBookSeconds(any()) }
                verify(exactly = 0) { harness.playbackConnection.play() }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a chapter of a book without progress plays from its start right away`() =
        runTest {
            val harness = createHarness()
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() }
                harness.viewModel.playChapter(chapters[1])
                verify { harness.playbackConnection.playAudiobook(book, 100.0) }
                assertNull(harness.viewModel.uiState.value.pendingChapterSwitch)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun TestScope.eventsOf(viewModel: BookDetailsViewModel): List<BookDetailsUiEvent> {
        val events = mutableListOf<BookDetailsUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    @Test
    fun `starting the book opens the full player`() =
        runTest {
            val harness = createHarness()
            val events = eventsOf(harness.viewModel)
            harness.viewModel.playBook()
            verify { harness.playbackConnection.playAudiobook(book) }
            assertEquals(listOf(BookDetailsUiEvent.OpenPlayer), events)
        }

    @Test
    fun `resuming the loaded book opens the full player too`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book))
            val events = eventsOf(harness.viewModel)
            harness.viewModel.playBook()
            verify { harness.playbackConnection.play() }
            assertEquals(listOf(BookDetailsUiEvent.OpenPlayer), events)
        }

    @Test
    fun `a chapter that plays right away opens the full player`() =
        runTest {
            val harness = createHarness()
            val events = eventsOf(harness.viewModel)
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() }
                harness.viewModel.playChapter(chapters[1])
                assertEquals(listOf(BookDetailsUiEvent.OpenPlayer), events)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a chapter that asks first opens the full player only once confirmed`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            val events = eventsOf(harness.viewModel)
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() && it.progressSeconds == 150.0 }
                harness.viewModel.playChapter(chapters[2])
                awaitSatisfying { it.pendingChapterSwitch == chapters[2] }
                assertEquals(emptyList<BookDetailsUiEvent>(), events)
                harness.viewModel.confirmChapterSwitch()
                assertEquals(listOf(BookDetailsUiEvent.OpenPlayer), events)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a dismissed chapter switch leaves the player closed`() =
        runTest {
            val harness = createHarness(playback = PlaybackState(audiobook = book, currentBookPositionSeconds = 150.0))
            val events = eventsOf(harness.viewModel)
            harness.viewModel.uiState.test {
                awaitSatisfying { it.chapters.isNotEmpty() && it.progressSeconds == 150.0 }
                harness.viewModel.playChapter(chapters[0])
                awaitSatisfying { it.pendingChapterSwitch == chapters[0] }
                harness.viewModel.dismissChapterSwitch()
                awaitSatisfying { it.pendingChapterSwitch == null }
                assertEquals(emptyList<BookDetailsUiEvent>(), events)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `surfaces chapter and book download states`() =
        runTest {
            val harness =
                createHarness(
                    downloads =
                        listOf(
                            download(0, DownloadStatus.COMPLETED, 60, 60),
                            download(1, DownloadStatus.DOWNLOADING, 30, 60),
                        ),
                )
            harness.viewModel.uiState.test {
                val loaded =
                    awaitSatisfying { state ->
                        state.audiobook != null &&
                            state.chapterDownloads.any { it.status == DownloadButtonStatus.DOWNLOADING }
                    }
                assertEquals(
                    listOf(
                        DownloadButtonStatus.DOWNLOADING,
                        DownloadButtonStatus.DOWNLOADING,
                        DownloadButtonStatus.NOT_DOWNLOADED,
                    ),
                    loaded.chapterDownloads.map { it.status },
                )
                assertEquals(0.75f, loaded.chapterDownloads[0].progressFraction)
                assertEquals(DownloadButtonStatus.DOWNLOADING, loaded.bookDownload.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `downloadBook passes the live position`() =
        runTest {
            val harness =
                createHarness(
                    playback =
                        PlaybackState(
                            audiobook = book,
                            isPlaying = true,
                            currentBookPositionSeconds = 150.0,
                        ),
                )
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null && it.progressSeconds == 150.0 }
                harness.viewModel.downloadBook()
                coVerify {
                    harness.downloadBook(
                        "lib-1",
                        "book-1",
                        150.0,
                        false,
                        false,
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `download and cancel actions delegate with the book and index`() =
        runTest {
            val harness = createHarness()
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null }
                harness.viewModel.downloadChapter(1)
                harness.viewModel.cancelChapterDownload(2)
                harness.viewModel.cancelBookDownload()
                coVerify { harness.downloadChapter("lib-1", "book-1", 1, false) }
                coVerify { harness.cancelChapterDownload("lib-1", "book-1", 2) }
                coVerify { harness.cancelBookDownload("book-1") }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `request and confirm chapter delete`() =
        runTest {
            val harness =
                createHarness(
                    downloads =
                        listOf(
                            download(0, DownloadStatus.COMPLETED, 60, 60),
                            download(1, DownloadStatus.COMPLETED, 60, 60),
                        ),
                )
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null && it.chapterDownloads.any { s -> s.downloadedBytes > 0 } }
                harness.viewModel.requestDeleteChapter(0)
                assertEquals(
                    DeleteDownloadTarget.Chapter(0, "Ch 1", 120L),
                    awaitItem().pendingDelete,
                )
                harness.viewModel.confirmDelete()
                assertNull(awaitItem().pendingDelete)
                coVerify { harness.deleteChapterDownload("lib-1", "book-1", 0) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dismiss delete closes without deleting`() =
        runTest {
            val harness = createHarness()
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null }
                harness.viewModel.requestDeleteBook()
                assertEquals(
                    DeleteDownloadTarget.Book("Dune", 0L),
                    awaitItem().pendingDelete,
                )
                harness.viewModel.dismissDelete()
                assertNull(awaitItem().pendingDelete)
                coVerify(exactly = 0) { harness.deleteBookDownload(any()) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `confirm book delete deletes the book download`() =
        runTest {
            val harness =
                createHarness(
                    downloads = listOf(download(0, DownloadStatus.COMPLETED, 60, 60)),
                )
            harness.viewModel.uiState.test {
                awaitSatisfying { it.audiobook != null && it.bookDownload.downloadedBytes == 60L }
                harness.viewModel.requestDeleteBook()
                assertEquals(
                    DeleteDownloadTarget.Book("Dune", 60L),
                    awaitItem().pendingDelete,
                )
                harness.viewModel.confirmDelete()
                coVerify { harness.deleteBookDownload("book-1") }
                cancelAndIgnoreRemainingEvents()
            }
        }
}
