package org.evoionosp.abstrak.playback
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressSyncPolicy
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgress
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgressEvents
import org.evoionosp.abstrak.domain.audiobook.usecase.PlaybackPreparation
import org.evoionosp.abstrak.domain.audiobook.usecase.PreparePlaybackUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ResolveResumePositionUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.SavePlaybackProgressUseCase
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.usecase.DeleteFinishedChapterDownloadsResult
import org.evoionosp.abstrak.domain.download.usecase.DeleteFinishedChapterDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveBookDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.ResolveLocalTracksUseCase
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaybackConnectionTest {
    @Test
    fun `publishes the initial controller state on connect`() =
        runTest {
            val graph = graph()
            runCurrent()
            graph.connection.playbackState.test {
                val state = awaitItem()
                assertFalse(state.isPlaying)
                assertEquals(300_000L, state.durationMs)
                assertEquals(0L, state.currentPositionMs)
                assertEquals(1.0f, state.playbackSpeed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `play delegates to the controller`() =
        runTest {
            val graph = graph()
            graph.connection.play()
            verify { graph.controller.play() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause delegates and saves progress once the book has played long enough`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            graph.connection.pause()
            runCurrent()
            verify { graph.controller.pause() }
            coVerify { graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 0.0, 300.0) }
        }

    @Test
    fun `a pause before the book has played fifteen seconds saves nothing`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(14_000)
            runCurrent()
            every { graph.controller.isPlaying } returns false
            graph.connection.pause()
            runCurrent()
            verify { graph.controller.pause() }
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `playAudiobook does nothing without a session`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns null
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
            assertNull(graph.connection.playbackState.value.audiobook)
            assertFalse(graph.connection.playbackState.value.isPreparing)
        }

    @Test
    fun `playAudiobook does nothing when no playable detail exists`() =
        runTest {
            val graph = graph()
            coEvery { graph.prepare(any(), any(), any(), any()) } returns PlaybackPreparation.NoAudio
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `playAudiobook loads tracks and resumes where the book was left`() =
        runTest {
            val graph = graph()
            coEvery { graph.resolveResume(any(), any(), any()) } returns 120.0
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            val items = slot<List<MediaItem>>()
            val index = slot<Int>()
            val offset = slot<Long>()
            verify { graph.controller.setMediaItems(capture(items), capture(index), capture(offset)) }
            assertEquals(listOf("0", "1", "2"), items.captured.map { it.mediaId })
            assertEquals(1, index.captured)
            assertEquals(20_000L, offset.captured)
            verify { graph.controller.prepare() }
            verify { graph.controller.play() }
            assertEquals(
                "The Book",
                items.captured[1]
                    .mediaMetadata.title
                    ?.toString(),
            )
            assertEquals(
                "Chapter Two",
                items.captured[1]
                    .mediaMetadata.artist
                    ?.toString(),
            )
        }

    @Test
    fun `explicit start position wins over saved progress`() =
        runTest {
            val graph = graph()
            coEvery { graph.resolveResume(any(), any(), any()) } returns 120.0
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 250.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            verify { graph.controller.setMediaItems(any(), 2, 50_000L) }
        }

    @Test
    fun `asks where to resume the book handed over`() =
        runTest {
            val graph = graph()
            val book = testAudiobook().copy(progressSeconds = 42.0)
            graph.connection.playAudiobook(book)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            coVerify { graph.resolveResume("https://example.com", "access-1", book) }
        }

    @Test
    fun `server progress pulled further along moves the loaded book there`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.currentMediaItemIndex } returns 0
            every { graph.controller.currentPosition } returns 10_000L
            graph.pulledProgress.publish(PulledProgress("book-1", progressAt(250.0)))
            runCurrent()
            verify { graph.controller.seekTo(2, 50_000L) }
            assertEquals(250.0, graph.connection.playbackState.value.currentBookPositionSeconds, 0.001)
        }

    @Test
    fun `moving to pulled progress tells the listener where`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            val notices = collectNotices(graph)
            graph.pulledProgress.publish(PulledProgress("book-1", progressAt(250.0)))
            runCurrent()
            assertEquals(listOf(PlaybackNotice.MovedToServerProgress(250.0)), notices)
        }

    @Test
    fun `server progress pulled behind the player leaves it where it is`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.currentMediaItemIndex } returns 2
            every { graph.controller.currentPosition } returns 50_000L
            graph.pulledProgress.publish(PulledProgress("book-1", progressAt(120.0)))
            runCurrent()
            verify(exactly = 0) { graph.controller.seekTo(any<Int>(), any<Long>()) }
        }

    @Test
    fun `server progress pulled for another book leaves the player alone`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            graph.pulledProgress.publish(PulledProgress("book-2", progressAt(250.0)))
            runCurrent()
            verify(exactly = 0) { graph.controller.seekTo(any<Int>(), any<Long>()) }
        }

    @Test
    fun `a book finished on another device keeps playing where it is`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            graph.pulledProgress.publish(
                PulledProgress("book-1", PlaybackProgress(299.0, durationSeconds = 300.0, isFinished = true)),
            )
            runCurrent()
            verify(exactly = 0) { graph.controller.seekTo(any<Int>(), any<Long>()) }
        }

    @Test
    fun `a book that can't start says why`() =
        runTest {
            val failures =
                mapOf(
                    PlaybackPreparation.Failed(CatalogError.CONNECTIVITY_UNAVAILABLE) to PlaybackFailure.OFFLINE,
                    PlaybackPreparation.Failed(CatalogError.NETWORK) to PlaybackFailure.SERVER,
                    PlaybackPreparation.Failed(CatalogError.UNKNOWN) to PlaybackFailure.SERVER,
                    PlaybackPreparation.Failed(CatalogError.NOT_FOUND) to PlaybackFailure.NOT_FOUND,
                    PlaybackPreparation.NoAudio to PlaybackFailure.NO_AUDIO,
                )
            for ((preparation, failure) in failures) {
                val graph = graph()
                coEvery { graph.prepare(any(), any(), any(), any()) } returns preparation
                val notices = collectNotices(graph)
                graph.connection.playAudiobook(testAudiobook())
                runCurrent()
                assertEquals(listOf(PlaybackNotice.CouldNotPlay(failure)), notices)
            }
        }

    @Test
    fun `offline, a book starting at a track not on the device can't play`() =
        runTest {
            val graph = graph()
            every { graph.connectivity.isConnected() } returns false
            coEvery { graph.resolveResume(any(), any(), any()) } returns 120.0
            coEvery { graph.resolveLocalTracks(any(), any()) } returns mapOf(0 to "/files/downloads/book-1/0.mp3")
            val notices = collectNotices(graph)
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            assertEquals(listOf(PlaybackNotice.CouldNotPlay(PlaybackFailure.OFFLINE)), notices)
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
            assertNull(graph.connection.playbackState.value.audiobook)
        }

    @Test
    fun `offline, a book starting at a track on the device plays`() =
        runTest {
            val graph = graph()
            every { graph.connectivity.isConnected() } returns false
            coEvery { graph.resolveResume(any(), any(), any()) } returns 120.0
            coEvery { graph.resolveLocalTracks(any(), any()) } returns mapOf(1 to "/files/downloads/book-1/1.mp3")
            val notices = collectNotices(graph)
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            runCurrent()
            assertTrue(notices.isEmpty())
            verify { graph.controller.setMediaItems(any(), 1, 20_000L) }
        }

    @Test
    fun `signed out, a book that can't start says nothing`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns null
            val notices = collectNotices(graph)
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            assertTrue(notices.isEmpty())
        }

    @Test
    fun `playback stopped by an error says why`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            val listener = graph.connection.PlayerListener()
            val notices = collectNotices(graph)
            every { graph.connectivity.isConnected() } returns false
            listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
            every { graph.connectivity.isConnected() } returns true
            listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
            listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
            listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
            listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_DECODING_FAILED))
            runCurrent()
            assertEquals(
                listOf(
                    PlaybackFailure.OFFLINE,
                    PlaybackFailure.SERVER,
                    PlaybackFailure.SERVER,
                    PlaybackFailure.UNKNOWN,
                    PlaybackFailure.UNKNOWN,
                ).map { PlaybackNotice.Stopped(it) },
                notices,
            )
        }

    @Test
    fun `play after an error prepares the player again`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.playbackState } returns Player.STATE_IDLE
            graph.connection.play()
            verifyOrder {
                graph.controller.prepare()
                graph.controller.play()
            }
        }

    @Test
    fun `play on a ready player only plays`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            graph.connection.play()
            verify(exactly = 0) { graph.controller.prepare() }
            verify { graph.controller.play() }
        }

    @Test
    fun `a book saves nothing until it has played fifteen seconds`() =
        runTest {
            val graph = graph()
            coEvery { graph.resolveResume(any(), any(), any()) } returns 120.0
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 20_000L
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            runCurrent()
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(14_000)
            runCurrent()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
            advanceTimeBy(1_000)
            runCurrent()
            coVerify(exactly = 1) {
                graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 120.0, 300.0)
            }
        }

    @Test
    fun `each book waits fifteen seconds of its own before saving`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(10_000)
            runCurrent()
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            graph.connection.playbackState.first { it.audiobook?.id == "book-2" && !it.isPreparing }
            advanceTimeBy(10_000)
            runCurrent()
            graph.connection.pause()
            runCurrent()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `switching away saves the book played long enough`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            runCurrent()
            coVerify(exactly = 1) { graph.saveProgress(any(), any(), "lib-1", "book-1", any(), any()) }
        }

    @Test
    fun `playing to the end saves however briefly the book played`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            val listener = graph.connection.PlayerListener()
            val stateChanged = mockk<Player.Events>()
            every { stateChanged.containsAny(*anyIntVararg()) } returns true
            every { graph.controller.playbackState } returns Player.STATE_ENDED
            every { graph.controller.currentMediaItemIndex } returns 2
            every { graph.controller.currentPosition } returns 100_000L
            listener.onEvents(graph.controller, stateChanged)
            runCurrent()
            coVerify { graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 300.0, 300.0) }
        }

    @Test
    fun `stopping saves the position it stopped at`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 30_000L
            graph.connection.stopPlayback()
            runCurrent()
            coVerify { graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 130.0, 300.0) }
        }

    @Test
    fun `a requested book shows at once, preparing, until the player has it`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook().copy(progressSeconds = 42.0))
            val requested = graph.connection.playbackState.value
            assertEquals("book-1", requested.audiobook?.id)
            assertTrue(requested.isPreparing)
            assertEquals(42.0, requested.currentBookPositionSeconds, 0.001)
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
            runCurrent()
            assertFalse(graph.connection.playbackState.value.isPreparing)
            verify { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `switching books pauses the book playing before the next one loads`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            coEvery { graph.prepare(any(), any(), any(), "book-2") } coAnswers { awaitCancellation() }
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            runCurrent()
            verify { graph.controller.pause() }
            val state = graph.connection.playbackState.value
            assertEquals("book-2", state.audiobook?.id)
            assertTrue(state.isPreparing)
        }

    @Test
    fun `a book that can't play falls back to the book the player holds`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            coEvery { graph.prepare(any(), any(), any(), "book-2") } returns PlaybackPreparation.NoAudio
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            runCurrent()
            val state = graph.connection.playbackState.value
            assertEquals("book-1", state.audiobook?.id)
            assertFalse(state.isPreparing)
        }

    @Test
    fun `a newer request replaces one still preparing`() =
        runTest {
            val graph = graph()
            coEvery { graph.prepare(any(), any(), any(), "book-1") } coAnswers { awaitCancellation() }
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            runCurrent()
            val state = graph.connection.playbackState.value
            assertEquals("book-2", state.audiobook?.id)
            assertFalse(state.isPreparing)
            verify(exactly = 1) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `play waits while a book prepares`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.play()
            verify(exactly = 0) { graph.controller.play() }
            runCurrent()
            verify(exactly = 1) { graph.controller.play() }
        }

    @Test
    fun `progress ticks leave a preparing book's position alone`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 50_000L
            coEvery { graph.prepare(any(), any(), any(), "book-2") } coAnswers { awaitCancellation() }
            graph.connection.playAudiobook(testAudiobook().copy(id = "book-2"))
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(0.0, graph.connection.playbackState.value.currentBookPositionSeconds, 0.0)
            assertEquals(0L, graph.connection.playbackState.value.currentPositionMs)
        }

    @Test
    fun `stopping cancels a book still preparing`() =
        runTest {
            val graph = graph()
            coEvery { graph.prepare(any(), any(), any(), any()) } coAnswers {
                delay(5_000)
                PlaybackPreparation.Ready(testDetail())
            }
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            graph.connection.stopPlayback()
            advanceTimeBy(10_000)
            runCurrent()
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
            assertNull(graph.connection.playbackState.value.audiobook)
        }

    @Test
    fun `a book that has just loaded shows buffering until it can play`() =
        runTest {
            val graph = graph()
            every { graph.controller.playbackState } returns Player.STATE_BUFFERING
            every { graph.controller.playWhenReady } returns true
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            val state = graph.connection.playbackState.value
            assertFalse(state.isPreparing)
            assertTrue(state.isBuffering)
        }

    @Test
    fun `buffering shows only while the player would be playing`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            val listener = graph.connection.PlayerListener()
            val stateChanged = mockk<Player.Events>()
            every { stateChanged.containsAny(*anyIntVararg()) } returns true
            every { graph.controller.playbackState } returns Player.STATE_BUFFERING
            every { graph.controller.playWhenReady } returns true
            listener.onEvents(graph.controller, stateChanged)
            assertTrue(graph.connection.playbackState.value.isBuffering)
            every { graph.controller.playWhenReady } returns false
            listener.onEvents(graph.controller, stateChanged)
            assertFalse(graph.connection.playbackState.value.isBuffering)
            every { graph.controller.playbackState } returns Player.STATE_READY
            every { graph.controller.playWhenReady } returns true
            listener.onEvents(graph.controller, stateChanged)
            assertFalse(graph.connection.playbackState.value.isBuffering)
        }

    @Test
    fun `downloaded tracks play from the device and the rest stream`() =
        runTest {
            mockkStatic(Uri::class)
            try {
                val parsed = mutableListOf<String>()
                every { Uri.parse(capture(parsed)) } returns mockk(relaxed = true)
                val graph = graph()
                coEvery { graph.resolveLocalTracks("book-1", any()) } returns mapOf(0 to "/files/downloads/book-1/0.mp3")
                graph.connection.playAudiobook(testAudiobook())
                graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
                assertEquals(
                    listOf("file:/files/downloads/book-1/0.mp3", "https://example.com/1.mp3", "https://example.com/2.mp3"),
                    parsed,
                )
            } finally {
                unmockkAll()
            }
        }

    @Test
    fun `a deleted download behind the playhead streams in place`() =
        runTest {
            val graph = graph()
            coEvery { graph.resolveLocalTracks(any(), any()) } returns mapOf(0 to "/d/0.mp3", 1 to "/d/1.mp3")
            graph.downloads.value = listOf(completed(0), completed(1))
            loadBook(graph)
            every { graph.controller.currentMediaItemIndex } returns 1
            graph.downloads.value = listOf(completed(1))
            runCurrent()
            verify(exactly = 1) { graph.controller.replaceMediaItem(0, any()) }
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `deleting the download being played reloads it streaming at the same spot`() =
        runTest {
            val graph = graph()
            coEvery { graph.resolveLocalTracks(any(), any()) } returns mapOf(0 to "/d/0.mp3", 1 to "/d/1.mp3")
            graph.downloads.value = listOf(completed(0), completed(1))
            loadBook(graph)
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 42_000L
            graph.downloads.value = emptyList()
            runCurrent()
            verify(exactly = 1) { graph.controller.setMediaItems(any(), 1, 42_000L) }
            verify(exactly = 1) { graph.controller.prepare() }
            verify(exactly = 0) { graph.controller.replaceMediaItem(any(), any()) }
        }

    @Test
    fun `a book that streams everything ignores download changes`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            graph.downloads.value = listOf(completed(0))
            graph.downloads.value = emptyList()
            runCurrent()
            verify(exactly = 0) { graph.controller.replaceMediaItem(any(), any()) }
            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    private fun completed(trackIndex: Int) =
        TrackDownload(audiobookId = "book-1", trackIndex = trackIndex, status = DownloadStatus.COMPLETED, localPath = "/d/$trackIndex.mp3")

    @Test
    fun `artwork uri is attached when the book has a cover`() =
        runTest {
            mockkStatic(Uri::class)
            val artwork = mockk<Uri>()
            every { Uri.parse(any()) } returns artwork
            try {
                val graph = graph()
                graph.connection.playAudiobook(testAudiobook().copy(coverUrl = "https://example.com/cover.jpg"))
                graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
                val items = slot<List<MediaItem>>()
                verify { graph.controller.setMediaItems(capture(items), any(), any()) }
                assertEquals(artwork, items.captured[0].mediaMetadata.artworkUri)
            } finally {
                unmockkAll()
            }
        }

    @Test
    fun `seekToBookSeconds resolves through tracks and saves`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            graph.connection.seekToBookSeconds(250.0)
            runCurrent()
            verify { graph.controller.seekTo(2, 50_000L) }
            coVerify { graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 0.0, 300.0) }
        }

    @Test
    fun `seekToBookSeconds without tracks seeks raw milliseconds`() =
        runTest {
            val graph = graph()
            graph.connection.seekToBookSeconds(12.5)
            verify { graph.controller.seekTo(12_500L) }
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `chapter skips resolve through the domain calculator`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 50_000L
            graph.connection.skipToNextChapter(testChapters())
            verify { graph.controller.seekTo(2, 0L) }
            graph.connection.skipToPreviousChapter(testChapters())
            verify { graph.controller.seekTo(1, 0L) }
        }

    @Test
    fun `next chapter past the end does nothing`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            every { graph.controller.currentMediaItemIndex } returns 2
            every { graph.controller.currentPosition } returns 90_000L
            graph.connection.skipToNextChapter(testChapters())
            verify(exactly = 0) { graph.controller.seekTo(any(), any()) }
        }

    @Test
    fun `seek forward and backward apply the default steps with a zero clamp`() =
        runTest {
            val graph = graph()
            every { graph.controller.currentPosition } returns 50_000L
            graph.connection.seekForward()
            verify { graph.controller.seekTo(80_000L) }
            graph.connection.seekBackward()
            verify { graph.controller.seekTo(35_000L) }
            every { graph.controller.currentPosition } returns 5_000L
            graph.connection.seekBackward()
            verify { graph.controller.seekTo(0L) }
        }

    @Test
    fun `setPlaybackSpeed updates the controller and the state`() =
        runTest {
            val graph = graph()
            graph.connection.setPlaybackSpeed(1.5f)
            verify { graph.controller.setPlaybackSpeed(1.5f) }
            graph.connection.playbackState.test {
                assertEquals(1.5f, awaitItem().playbackSpeed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `progress loop saves every fifteen seconds while playing`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(15_000)
            runCurrent()
            coVerify(exactly = 1) { graph.saveProgress(any(), any(), any(), any(), 0.0, 300.0) }
            advanceTimeBy(15_000)
            runCurrent()
            coVerify(exactly = 2) { graph.saveProgress(any(), any(), any(), any(), 0.0, 300.0) }
        }

    @Test
    fun `progress loop stays silent while paused`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            advanceTimeBy(60_000)
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the controller reports no current item`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            every { graph.controller.currentMediaItemIndex } returns C.INDEX_UNSET
            graph.connection.pause()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the session went blank mid-playback`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns testSession() andThen testSession().copy(accessToken = "")
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(15_000)
            runCurrent()
            every { graph.controller.isPlaying } returns false
            graph.connection.pause()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the session is gone mid-playback`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns testSession() andThen null
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            every { graph.controller.isPlaying } returns true
            advanceTimeBy(15_000)
            runCurrent()
            every { graph.controller.isPlaying } returns false
            graph.connection.pause()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `disconnected controller degrades gracefully`() =
        runTest {
            val graph = graph(connected = false)
            graph.connection.play()
            graph.connection.pause()
            graph.connection.seekToBookSeconds(10.0)
            graph.connection.setPlaybackSpeed(2.0f)
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any(), any()) }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `signing out stops playback and clears the loaded book`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            graph.session.value = null
            runCurrent()
            verify { graph.controller.stop() }
            verify { graph.controller.clearMediaItems() }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stopForLogout flushes progress before stopping`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            graph.connection.stopForLogout()
            coVerifyOrder {
                graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 0.0, 300.0)
                graph.controller.stop()
            }
            verify { graph.controller.clearMediaItems() }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stopForLogout still stops when the flush fails`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            coEvery { graph.saveProgress(any(), any(), any(), any(), any(), any()) } throws RuntimeException("save boom")
            try {
                graph.connection.stopForLogout()
                fail("expected the flush failure to propagate")
            } catch (_: RuntimeException) {
            }
            verify { graph.controller.stop() }
            verify { graph.controller.clearMediaItems() }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `player logout handler stops the connection for logout`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            PlaybackConnectionLogoutHandler(graph.connection).onLogout()
            verify { graph.controller.stop() }
            verify { graph.controller.clearMediaItems() }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `player listener playing changes update the state`() =
        runTest {
            val graph = graph()
            graph.connection.playbackState.test {
                assertFalse(awaitItem().isPlaying)
                graph.connection.PlayerListener().onIsPlayingChanged(true)
                assertTrue(awaitItem().isPlaying)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `progress updates reflect the controller position`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 50_000L
            graph.connection.playbackState.test {
                awaitItem()
                advanceTimeBy(1_000)
                val state = awaitItem()
                assertEquals(50_000L, state.currentPositionMs)
                assertEquals(150.0, state.currentBookPositionSeconds, 0.001)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `playing off the end of a chapter deletes its downloads when auto-delete is on`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock, autoDeleteEnabled = true)
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 98.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 99)
            coVerify(exactly = 0) { graph.deleteFinishedChapterDownloads(any(), any(), any()) }
            tickAt(graph, clock, bookSeconds = 100)
            coVerify(exactly = 1) { graph.deleteFinishedChapterDownloads("lib-1", "book-1", setOf(0)) }
        }

    @Test
    fun `skipping ahead to a later chapter deletes nothing`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock, autoDeleteEnabled = true)
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 10.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 200)
            tickAt(graph, clock, bookSeconds = 201)
            coVerify(exactly = 0) { graph.deleteFinishedChapterDownloads(any(), any(), any()) }
        }

    @Test
    fun `playing off the end of a chapter deletes nothing when auto-delete is off`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock)
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 98.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 99)
            tickAt(graph, clock, bookSeconds = 100)
            coVerify(exactly = 0) { graph.deleteFinishedChapterDownloads(any(), any(), any()) }
        }

    @Test
    fun `resuming in a later chapter deletes nothing`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock, autoDeleteEnabled = true)
            coEvery { graph.resolveResume(any(), any(), any()) } returns 200.0
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 200)
            tickAt(graph, clock, bookSeconds = 201)
            coVerify(exactly = 0) { graph.deleteFinishedChapterDownloads(any(), any(), any()) }
        }

    @Test
    fun `chapters finished in one book are not carried into the next`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock, autoDeleteEnabled = true)
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 99.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 100)
            val nextBook = testAudiobook().copy(id = "book-2")
            graph.connection.playAudiobook(nextBook, startPositionSeconds = 199.0)
            graph.connection.playbackState.first { it.audiobook == nextBook }
            tickAt(graph, clock, bookSeconds = 200)
            coVerify { graph.deleteFinishedChapterDownloads("lib-1", "book-1", setOf(0)) }
            coVerify { graph.deleteFinishedChapterDownloads("lib-1", "book-2", setOf(1)) }
        }

    @Test
    fun `a failed auto-delete leaves playback running`() =
        runTest {
            val clock = ManualClock()
            val graph = graph(clock = clock, autoDeleteEnabled = true)
            coEvery { graph.deleteFinishedChapterDownloads(any(), any(), any()) } throws
                IllegalStateException("storage unavailable")
            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 99.0)
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            tickAt(graph, clock, bookSeconds = 100)
            tickAt(graph, clock, bookSeconds = 101)
            coVerify { graph.deleteFinishedChapterDownloads("lib-1", "book-1", setOf(0)) }
            assertEquals(101.0, graph.connection.playbackState.value.currentBookPositionSeconds, 0.001)
        }

    private data class Graph(
        val connection: PlaybackConnection,
        val controller: Player,
        val session: MutableStateFlow<LoginSession?>,
        val getValidSession: GetValidSessionUseCase,
        val prepare: PreparePlaybackUseCase,
        val resolveResume: ResolveResumePositionUseCase,
        val saveProgress: SavePlaybackProgressUseCase,
        val deleteFinishedChapterDownloads: DeleteFinishedChapterDownloadsUseCase,
        val resolveLocalTracks: ResolveLocalTracksUseCase,
        val downloads: MutableStateFlow<List<TrackDownload>>,
        val pulledProgress: PulledProgressEvents,
        val connectivity: ConnectivityObserver,
    )

    private fun TestScope.graph(
        connected: Boolean = true,
        clock: PlaybackClock = PlaybackClock(System::currentTimeMillis),
        autoDeleteEnabled: Boolean = false,
    ): Graph {
        val controller = mockk<Player>()
        every { controller.addListener(any()) } just Runs
        every { controller.isPlaying } returns false
        every { controller.duration } returns 300_000L
        every { controller.currentPosition } returns 0L
        every { controller.currentMediaItemIndex } returns 0
        every { controller.playbackState } returns Player.STATE_READY
        every { controller.playWhenReady } returns false
        every { controller.playbackParameters } returns PlaybackParameters(1.0f)
        every { controller.play() } just Runs
        every { controller.pause() } just Runs
        every { controller.prepare() } just Runs
        every { controller.seekTo(any<Long>()) } just Runs
        every { controller.seekTo(any<Int>(), any<Long>()) } just Runs
        every { controller.setMediaItems(any<List<MediaItem>>(), any<Int>(), any<Long>()) } just Runs
        every { controller.replaceMediaItem(any(), any()) } just Runs
        every { controller.setPlaybackSpeed(any()) } just Runs
        every { controller.stop() } just Runs
        every { controller.clearMediaItems() } just Runs
        val getValidSession = mockk<GetValidSessionUseCase>()
        coEvery { getValidSession() } returns testSession()
        val prepare = mockk<PreparePlaybackUseCase>()
        coEvery { prepare(any(), any(), any(), any()) } returns PlaybackPreparation.Ready(testDetail())
        val resolveResume = mockk<ResolveResumePositionUseCase>()
        coEvery { resolveResume(any(), any(), any()) } returns 0.0
        val saveProgress = mockk<SavePlaybackProgressUseCase>()
        coEvery { saveProgress(any(), any(), any(), any(), any(), any()) } returns DomainResult.Success(Unit)
        val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        val session = MutableStateFlow<LoginSession?>(testSession())
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns session
        val appSettingsStore = mockk<AppSettingsStore>()
        every { appSettingsStore.settings } returns
            MutableStateFlow(AppSettings(autoDeleteFinishedDownloads = autoDeleteEnabled))
        val deleteFinishedChapterDownloads = mockk<DeleteFinishedChapterDownloadsUseCase>()
        coEvery { deleteFinishedChapterDownloads(any(), any(), any()) } returns
            DeleteFinishedChapterDownloadsResult.Deleted(0)
        val resolveLocalTracks = mockk<ResolveLocalTracksUseCase>()
        coEvery { resolveLocalTracks(any(), any()) } returns emptyMap()
        val downloads = MutableStateFlow<List<TrackDownload>>(emptyList())
        val observeBookDownloads = mockk<ObserveBookDownloadsUseCase>()
        every { observeBookDownloads(any()) } returns downloads
        val pulledProgress = PulledProgressEvents()
        val connectivity = mockk<ConnectivityObserver>()
        every { connectivity.isConnected() } returns true
        val connection =
            PlaybackConnection(
                getValidSessionUseCase = getValidSession,
                calculator = PlaybackPositionCalculator(),
                preparePlayback = prepare,
                resolveResumePosition = resolveResume,
                savePlaybackProgress = saveProgress,
                controllerConnector = FakeConnector(if (connected) controller else null),
                connectionScope = scope,
                observeSessionUseCase = observeSession,
                clock = clock,
                appSettingsStore = appSettingsStore,
                deleteFinishedChapterDownloads = deleteFinishedChapterDownloads,
                resolveLocalTracks = resolveLocalTracks,
                observeBookDownloads = observeBookDownloads,
                pulledProgressEvents = pulledProgress,
                connectivityObserver = connectivity,
            )
        runCurrent()
        return Graph(
            connection,
            controller,
            session,
            getValidSession,
            prepare,
            resolveResume,
            saveProgress,
            deleteFinishedChapterDownloads,
            resolveLocalTracks,
            downloads,
            pulledProgress,
            connectivity,
        )
    }

    private suspend fun TestScope.loadBook(
        graph: Graph,
        book: Audiobook = testAudiobook(),
    ) {
        graph.connection.playAudiobook(book)
        graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
        runCurrent()
        clearMocks(graph.saveProgress, graph.controller, answers = false)
    }

    private fun TestScope.listenPastSaveWait(graph: Graph) {
        every { graph.controller.isPlaying } returns true
        advanceTimeBy(ProgressSyncPolicy.INTERVAL_SECONDS * 1_000L)
        runCurrent()
        every { graph.controller.isPlaying } returns false
        clearMocks(graph.saveProgress, answers = false)
    }

    private class FakeConnector(
        private val controller: Player?,
    ) : MediaControllerConnector {
        override suspend fun connect(): Player? = controller
    }

    private class ManualClock : PlaybackClock {
        var timeMs = 1_000_000L

        override fun nowMs(): Long = timeMs
    }

    private fun TestScope.tickAt(
        graph: Graph,
        clock: ManualClock,
        bookSeconds: Int,
    ) {
        clock.timeMs += 1_000L
        every { graph.controller.currentMediaItemIndex } returns bookSeconds / 100
        every { graph.controller.currentPosition } returns bookSeconds % 100 * 1_000L
        advanceTimeBy(1_000L)
        runCurrent()
    }

    private fun TestScope.collectNotices(graph: Graph): List<PlaybackNotice> {
        val notices = mutableListOf<PlaybackNotice>()
        backgroundScope.launch { graph.connection.notices.toList(notices) }
        return notices
    }

    @OptIn(UnstableApi::class)
    private fun playbackError(errorCode: Int) = PlaybackException("test", null, errorCode)

    private fun progressAt(seconds: Double) = PlaybackProgress(seconds, durationSeconds = 300.0, isFinished = false)

    private fun testSession() =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    private fun testAudiobook() =
        Audiobook(
            id = "book-1",
            libraryId = "lib-1",
            title = "The Book",
            author = "Author",
            coverUrl = "",
            series = null,
            durationInSeconds = 300,
        )

    private fun testTrack(
        index: Int,
        start: Long,
    ) = AudiobookTrack(
        index = index,
        startOffsetInSeconds = start,
        durationInSeconds = 100,
        title = "Track $index",
        remoteUrl = "https://example.com/$index.mp3",
        mimeType = "audio/mpeg",
    )

    private fun testChapters() =
        listOf(
            AudiobookChapter(title = "Chapter One", startInSeconds = 0, endInSeconds = 100),
            AudiobookChapter(title = "Chapter Two", startInSeconds = 100, endInSeconds = 200),
            AudiobookChapter(title = "Chapter Three", startInSeconds = 200, endInSeconds = null),
        )

    private fun testDetail() =
        AudiobookDetail(
            audiobook = testAudiobook(),
            description = null,
            chapters = testChapters(),
            tracks = listOf(testTrack(0, 0), testTrack(1, 100), testTrack(2, 200)),
            refreshedAtMillis = 0L,
        )

    @Test
    fun `sleep timer arms and pauses when the duration elapses`() =
        runTest {
            val graph = graph()
            graph.connection.startSleepTimer(30)
            val armed = graph.connection.playbackState.value.sleepTimer as SleepTimerState.Timer
            assertTrue(armed.endsAtMs > System.currentTimeMillis())
            advanceTimeBy(30 * 60_000L)
            runCurrent()
            assertEquals(SleepTimerState.Off, graph.connection.playbackState.value.sleepTimer)
            verify { graph.controller.pause() }
        }

    @Test
    fun `cancelled sleep timer never fires`() =
        runTest {
            val graph = graph()
            graph.connection.startSleepTimer(30)
            graph.connection.cancelSleepTimer()
            advanceTimeBy(31 * 60_000L)
            runCurrent()
            assertEquals(SleepTimerState.Off, graph.connection.playbackState.value.sleepTimer)
            verify(exactly = 0) { graph.controller.pause() }
        }

    @Test
    fun `rearming replaces the previous timer`() =
        runTest {
            val graph = graph()
            graph.connection.startSleepTimer(30)
            graph.connection.startSleepTimer(60)
            advanceTimeBy(31 * 60_000L)
            runCurrent()
            assertTrue(graph.connection.playbackState.value.sleepTimer is SleepTimerState.Timer)
            advanceTimeBy(30 * 60_000L)
            runCurrent()
            assertEquals(SleepTimerState.Off, graph.connection.playbackState.value.sleepTimer)
            verify { graph.controller.pause() }
        }

    @Test
    fun `sleep at chapter end fires when the position crosses the target`() =
        runTest {
            val graph = graph()
            loadBook(graph)
            listenPastSaveWait(graph)
            graph.connection.startSleepAtChapterEnd(50.0)
            advanceTimeBy(2_000L)
            runCurrent()
            assertTrue(graph.connection.playbackState.value.sleepTimer is SleepTimerState.EndOfChapter)
            every { graph.controller.currentPosition } returns 60_000L
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(SleepTimerState.Off, graph.connection.playbackState.value.sleepTimer)
            verify { graph.controller.pause() }
            coVerify { graph.saveProgress("https://example.com", "access-1", "lib-1", "book-1", 60.0, 300.0) }
        }

    @Test
    fun `switching books disarms the sleep timer`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null && !it.isPreparing }
            graph.connection.startSleepTimer(30)
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()
            assertEquals(SleepTimerState.Off, graph.connection.playbackState.value.sleepTimer)
        }

    @Test
    fun `remaining time ticks every second while paused`() =
        runTest {
            var nowMs = 1_000_000L
            val graph = graph(clock = PlaybackClock { nowMs })
            graph.connection.playbackState.test {
                awaitItem()
                graph.connection.startSleepTimer(30)
                val armed = awaitItem()
                assertTrue(armed.sleepTimer is SleepTimerState.Timer)
                assertEquals(30 * 60_000L, armed.sleepRemainingMs)
                nowMs += 5_000L
                advanceTimeBy(1_000L)
                val tick = awaitItem()
                assertEquals(30 * 60_000L - 5_000L, tick.sleepRemainingMs)
                graph.connection.startSleepAtChapterEnd(50.0)
                assertNull(awaitItem().sleepRemainingMs)
                graph.connection.cancelSleepTimer()
                assertNull(awaitItem().sleepRemainingMs)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `remainingMs counts down to zero and nulls when idle`() {
        assertNull(SleepTimerState.Off.remainingMs(1_000L))
        assertNull(SleepTimerState.EndOfChapter(50.0).remainingMs(1_000L))
        assertEquals(30_000L, SleepTimerState.Timer(endsAtMs = 31_000L).remainingMs(1_000L))
        assertEquals(0L, SleepTimerState.Timer(endsAtMs = 500L).remainingMs(1_000L))
    }
}
