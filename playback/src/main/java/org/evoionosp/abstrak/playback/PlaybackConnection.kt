package org.evoionosp.abstrak.playback
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.playback.ProgressSyncPolicy
import org.evoionosp.abstrak.domain.audiobook.sync.PulledProgressEvents
import org.evoionosp.abstrak.domain.audiobook.usecase.PlaybackPreparation
import org.evoionosp.abstrak.domain.audiobook.usecase.PreparePlaybackUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ResolveResumePositionUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.SavePlaybackProgressUseCase
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.usecase.DeleteFinishedChapterDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveBookDownloadsUseCase
import org.evoionosp.abstrak.domain.download.usecase.ResolveLocalTracksUseCase
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

interface MediaControllerConnector {
    suspend fun connect(): Player?
}

class SessionMediaControllerConnector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MediaControllerConnector {
        override suspend fun connect(): Player? =
            suspendCancellableCoroutine { continuation ->
                val sessionToken =
                    SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                controllerFuture.addListener({
                    continuation.resume(controllerFuture.get())
                }, MoreExecutors.directExecutor())
                continuation.invokeOnCancellation { controllerFuture.cancel(true) }
            }
    }

@Singleton
class PlaybackConnection
    @Inject
    constructor(
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val calculator: PlaybackPositionCalculator,
        private val preparePlayback: PreparePlaybackUseCase,
        private val resolveResumePosition: ResolveResumePositionUseCase,
        private val savePlaybackProgress: SavePlaybackProgressUseCase,
        private val controllerConnector: MediaControllerConnector,
        private val connectionScope: CoroutineScope,
        private val observeSessionUseCase: ObserveSessionUseCase,
        private val clock: PlaybackClock,
        private val appSettingsStore: AppSettingsStore,
        private val deleteFinishedChapterDownloads: DeleteFinishedChapterDownloadsUseCase,
        private val resolveLocalTracks: ResolveLocalTracksUseCase,
        private val observeBookDownloads: ObserveBookDownloadsUseCase,
        private val pulledProgressEvents: PulledProgressEvents,
        private val connectivityObserver: ConnectivityObserver,
    ) {
        private val _playbackState = MutableStateFlow(PlaybackState())
        val playbackState = _playbackState.asStateFlow()
        private val _notices = Channel<PlaybackNotice>(Channel.BUFFERED)
        val notices: Flow<PlaybackNotice> = _notices.receiveAsFlow()
        private var mediaController: Player? = null
        private var currentAudiobookId: String? = null
        private var currentLibraryId: String? = null
        private var currentChapters: List<AudiobookChapter> = emptyList()
        private var currentTracks: List<AudiobookTrack> = emptyList()
        private var currentTotalDurationSeconds: Double = 0.0
        private var secondsSinceServerSave: Int = 0
        private var secondsPlayedSinceLoad: Int = 0
        private var sleepJob: Job? = null
        private var prepareJob: Job? = null
        private var loadedAudiobook: Audiobook? = null
        private var localTrackPaths: Map<Int, String> = emptyMap()
        private var localTracksJob: Job? = null
        private var lastTickBookSeconds: Double = 0.0
        private var lastTickAtMs: Long = 0L
        private val finishedChapters = mutableSetOf<Int>()
        private var autoDeleteFinishedDownloads: Boolean = false

        init {
            connectionScope.launch {
                controllerConnector.connect()?.let { controller ->
                    mediaController = controller
                    controller.addListener(PlayerListener())
                    updateState()
                }
            }
            startProgressUpdateLoop()
            stopWhenSignedOut()
            observeAutoDeleteSetting()
            followPulledProgress()
        }

        private fun observeAutoDeleteSetting() {
            connectionScope.launch {
                appSettingsStore.settings
                    .map { it.autoDeleteFinishedDownloads }
                    .distinctUntilChanged()
                    .collect { autoDeleteFinishedDownloads = it }
            }
        }

        private fun followPulledProgress() {
            connectionScope.launch {
                pulledProgressEvents.pulled.collect { pulled ->
                    val controller = mediaController ?: return@collect
                    if (pulled.audiobookId != currentAudiobookId || currentTracks.isEmpty()) return@collect
                    if (_playbackState.value.isPreparing) return@collect
                    val target = pulled.progress.resumeSeconds ?: return@collect
                    if (target <= currentAbsoluteSeconds(controller)) return@collect
                    val position = calculator.resolveSeekPosition(target, currentTracks)
                    controller.seekTo(position.trackIndex, position.offsetMs)
                    _playbackState.update { it.copy(currentBookPositionSeconds = target) }
                    _notices.trySend(PlaybackNotice.MovedToServerProgress(target))
                }
            }
        }

        private fun stopWhenSignedOut() {
            connectionScope.launch {
                observeSessionUseCase().collect { session ->
                    if (session == null) stopPlayback()
                }
            }
        }

        fun playAudiobook(
            audiobook: Audiobook,
            startPositionSeconds: Double? = null,
        ) {
            _playbackState.update {
                it.copy(
                    audiobook = audiobook,
                    isPreparing = true,
                    isBuffering = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    currentBookPositionSeconds = startPositionSeconds ?: audiobook.progressSeconds ?: 0.0,
                )
            }
            prepareJob?.cancel()
            prepareJob =
                connectionScope.launch {
                    if (!load(audiobook, startPositionSeconds)) {
                        _playbackState.update { it.copy(audiobook = loadedAudiobook, isPreparing = false) }
                        updateState()
                    }
                }
        }

        private suspend fun load(
            audiobook: Audiobook,
            startPositionSeconds: Double?,
        ): Boolean {
            val controller = mediaController ?: return couldNotPlay(PlaybackFailure.UNKNOWN)
            controller.pause()
            saveCurrentProgressNow()
            cancelSleepTimer()
            val session = getValidSessionUseCase() ?: return false
            val detail =
                when (
                    val prepared =
                        preparePlayback(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = audiobook.libraryId,
                            audiobookId = audiobook.id,
                        )
                ) {
                    is PlaybackPreparation.Ready -> prepared.detail
                    PlaybackPreparation.NoAudio -> return couldNotPlay(PlaybackFailure.NO_AUDIO)
                    is PlaybackPreparation.Failed -> return couldNotPlay(prepared.error.toPlaybackFailure())
                }
            val localTracks = resolveLocalTracks(audiobook.id, detail.tracks)
            val mediaItems =
                detail.tracks.map { track ->
                    mediaItem(audiobook, track, detail.chapters, localTracks[track.index])
                }
            val startSeconds =
                startPositionSeconds
                    ?: resolveResumePosition(session.baseUrl, session.accessToken, audiobook)
            val start = calculator.resolveSeekPosition(startSeconds, detail.tracks)
            val startTrack = detail.tracks.getOrNull(start.trackIndex)
            if (startTrack != null && startTrack.index !in localTracks && !connectivityObserver.isConnected()) {
                return couldNotPlay(PlaybackFailure.OFFLINE)
            }
            currentAudiobookId = audiobook.id
            currentLibraryId = audiobook.libraryId
            currentChapters = detail.chapters
            currentTracks = detail.tracks
            localTrackPaths = localTracks
            currentTotalDurationSeconds =
                calculator.totalDurationSeconds(audiobook, detail.tracks)
            lastTickBookSeconds = startSeconds
            lastTickAtMs = clock.nowMs()
            finishedChapters.clear()
            secondsSinceServerSave = 0
            secondsPlayedSinceLoad = 0
            controller.setMediaItems(mediaItems, start.trackIndex, start.offsetMs)
            controller.prepare()
            controller.play()
            loadedAudiobook = audiobook
            _playbackState.update { it.copy(audiobook = audiobook, isPreparing = false) }
            updateState()
            streamTracksOnceDeleted(audiobook.id)
            return true
        }

        private fun couldNotPlay(failure: PlaybackFailure): Boolean {
            _notices.trySend(PlaybackNotice.CouldNotPlay(failure))
            return false
        }

        private fun CatalogError.toPlaybackFailure(): PlaybackFailure =
            when (this) {
                CatalogError.CONNECTIVITY_UNAVAILABLE -> PlaybackFailure.OFFLINE
                CatalogError.NOT_FOUND -> PlaybackFailure.NOT_FOUND
                CatalogError.AUTH, CatalogError.NETWORK, CatalogError.UNKNOWN -> PlaybackFailure.SERVER
                CatalogError.NO_AUDIOBOOK_LIBRARIES -> PlaybackFailure.UNKNOWN
            }

        private fun failureOf(error: PlaybackException): PlaybackFailure =
            when {
                error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackFailure.UNKNOWN
                error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlaybackFailure.UNKNOWN
                error.errorCode !in IO_ERROR_CODES -> PlaybackFailure.UNKNOWN
                connectivityObserver.isConnected() -> PlaybackFailure.SERVER
                else -> PlaybackFailure.OFFLINE
            }

        private fun mediaItem(
            audiobook: Audiobook,
            track: AudiobookTrack,
            chapters: List<AudiobookChapter>,
            localPath: String?,
        ): MediaItem {
            val chapterTitle = calculator.chapterTitleForTrack(track, chapters)
            val artworkUri = audiobook.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)
            return MediaItem
                .Builder()
                .setMediaId(track.index.toString())
                .setUri(localPath?.let { File(it).toURI().toString() } ?: track.remoteUrl)
                .setMediaMetadata(
                    MediaMetadata
                        .Builder()
                        .setTitle(audiobook.title)
                        .setArtist(chapterTitle)
                        .setAlbumTitle(audiobook.title)
                        .setAlbumArtist(audiobook.author)
                        .setDisplayTitle(audiobook.title)
                        .setSubtitle(chapterTitle)
                        .apply { artworkUri?.let(::setArtworkUri) }
                        .build(),
                ).build()
        }

        private fun streamTracksOnceDeleted(audiobookId: String) {
            localTracksJob?.cancel()
            if (localTrackPaths.isEmpty()) return
            localTracksJob =
                connectionScope.launch {
                    observeBookDownloads(audiobookId)
                        .map { rows ->
                            rows.filter { it.status == DownloadStatus.COMPLETED }.map { it.trackIndex }.toSet()
                        }.distinctUntilChanged()
                        .collect { onDevice ->
                            val deleted = localTrackPaths.keys - onDevice
                            if (deleted.isNotEmpty()) streamInstead(deleted)
                        }
                }
        }

        private fun streamInstead(deleted: Set<Int>) {
            val controller = mediaController ?: return
            val audiobook = loadedAudiobook ?: return
            localTrackPaths = localTrackPaths - deleted
            val positions = currentTracks.indices.filter { currentTracks[it].index in deleted }
            val current = controller.currentMediaItemIndex
            if (current in positions) {
                val items = currentTracks.map { mediaItem(audiobook, it, currentChapters, localTrackPaths[it.index]) }
                controller.setMediaItems(items, current, controller.currentPosition)
                controller.prepare()
            } else {
                positions.forEach { position ->
                    controller.replaceMediaItem(
                        position,
                        mediaItem(audiobook, currentTracks[position], currentChapters, null),
                    )
                }
            }
        }

        private fun currentAbsoluteSeconds(controller: Player): Double {
            val index = controller.currentMediaItemIndex
            if (index == C.INDEX_UNSET) return 0.0
            return calculator.absolutePosition(index, controller.currentPosition, currentTracks)
        }

        private fun saveCurrentProgressAsync(force: Boolean = false) {
            val snapshot = progressToSave(force) ?: return
            connectionScope.launch { save(snapshot) }
        }

        private fun deleteFinishedChapters(finishedChapterIndexes: Set<Int>) {
            val libraryId = currentLibraryId ?: return
            val audiobookId = currentAudiobookId ?: return
            connectionScope.launch {
                try {
                    deleteFinishedChapterDownloads(
                        libraryId = libraryId,
                        audiobookId = audiobookId,
                        finishedChapterIndexes = finishedChapterIndexes,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Deleting finished chapters of $audiobookId failed", e)
                }
            }
        }

        private suspend fun saveCurrentProgressNow() {
            progressToSave(force = false)?.let { save(it) }
        }

        private data class ProgressSnapshot(
            val libraryId: String,
            val audiobookId: String,
            val absoluteSeconds: Double,
            val totalSeconds: Double?,
        )

        private fun progressToSave(force: Boolean): ProgressSnapshot? {
            val controller = mediaController ?: return null
            val audiobookId = currentAudiobookId ?: return null
            val libraryId = currentLibraryId ?: return null
            if (currentTracks.isEmpty()) return null
            if (controller.currentMediaItemIndex == C.INDEX_UNSET) return null
            if (!force && secondsPlayedSinceLoad < ProgressSyncPolicy.INTERVAL_SECONDS) return null
            return ProgressSnapshot(
                libraryId = libraryId,
                audiobookId = audiobookId,
                absoluteSeconds = currentAbsoluteSeconds(controller),
                totalSeconds = currentTotalDurationSeconds.takeIf { it > 0 },
            )
        }

        private suspend fun save(snapshot: ProgressSnapshot) {
            val session = getValidSessionUseCase() ?: return
            if (session.baseUrl.isBlank() || session.accessToken.isBlank()) return
            savePlaybackProgress(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = snapshot.libraryId,
                audiobookId = snapshot.audiobookId,
                absoluteSeconds = snapshot.absoluteSeconds,
                totalSeconds = snapshot.totalSeconds,
            )
        }

        fun play() {
            if (_playbackState.value.isPreparing) return
            val controller = mediaController ?: return
            if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
            controller.play()
        }

        fun pause() {
            mediaController?.pause()
            saveCurrentProgressAsync()
        }

        fun stopPlayback() {
            saveCurrentProgressAsync()
            stopNow()
        }

        suspend fun stopForLogout() {
            try {
                saveCurrentProgressNow()
            } finally {
                stopNow()
            }
        }

        private fun stopNow() {
            prepareJob?.cancel()
            prepareJob = null
            localTracksJob?.cancel()
            localTracksJob = null
            localTrackPaths = emptyMap()
            loadedAudiobook = null
            mediaController?.stop()
            mediaController?.clearMediaItems()
            sleepJob?.cancel()
            sleepJob = null
            currentAudiobookId = null
            currentLibraryId = null
            currentChapters = emptyList()
            currentTracks = emptyList()
            currentTotalDurationSeconds = 0.0
            finishedChapters.clear()
            secondsSinceServerSave = 0
            secondsPlayedSinceLoad = 0
            _playbackState.value = PlaybackState()
        }

        fun seekTo(positionMs: Long) {
            mediaController?.seekTo(positionMs)
        }

        fun seekToBookSeconds(absoluteSeconds: Double) {
            val controller = mediaController ?: return
            if (currentTracks.isEmpty()) {
                controller.seekTo((absoluteSeconds * 1000).toLong().coerceAtLeast(0L))
                return
            }
            val target = calculator.resolveSeekPosition(absoluteSeconds, currentTracks)
            controller.seekTo(target.trackIndex, target.offsetMs)
            saveCurrentProgressAsync()
        }

        fun skipToNextChapter(chapters: List<AudiobookChapter>) {
            val controller = mediaController ?: return
            val position = currentAbsoluteSeconds(controller)
            val next = calculator.nextChapterStart(position, chapters) ?: return
            seekToBookSeconds(next)
        }

        fun skipToPreviousChapter(chapters: List<AudiobookChapter>) {
            val controller = mediaController ?: return
            val position = currentAbsoluteSeconds(controller)
            seekToBookSeconds(calculator.previousChapterTarget(position, chapters))
        }

        fun seekForward(amountMs: Long = SEEK_FORWARD_MS) {
            val controller = mediaController ?: return
            controller.seekTo(controller.currentPosition + amountMs)
        }

        fun seekBackward(amountMs: Long = SEEK_BACKWARD_MS) {
            val controller = mediaController ?: return
            controller.seekTo((controller.currentPosition - amountMs).coerceAtLeast(0))
        }

        private fun startProgressUpdateLoop() {
            connectionScope.launch {
                while (true) {
                    updateProgress()
                    delay(1000)
                }
            }
        }

        private fun updateProgress() {
            val controller = mediaController ?: return
            if (_playbackState.value.isPreparing) return
            val absoluteSeconds = currentAbsoluteSeconds(controller)
            val nowMs = clock.nowMs()
            _playbackState.update {
                it.copy(
                    currentPositionMs = controller.currentPosition,
                    currentBookPositionSeconds = absoluteSeconds,
                    sleepRemainingMs = (it.sleepTimer as? SleepTimerState.Timer)?.remainingMs(nowMs),
                )
            }
            val playedThrough =
                calculator.chaptersPlayedThrough(
                    fromSeconds = lastTickBookSeconds,
                    toSeconds = absoluteSeconds,
                    elapsedMs = nowMs - lastTickAtMs,
                    speed = _playbackState.value.playbackSpeed,
                    chapters = currentChapters,
                )
            lastTickBookSeconds = absoluteSeconds
            lastTickAtMs = nowMs
            if (finishedChapters.addAll(playedThrough) && autoDeleteFinishedDownloads) {
                deleteFinishedChapters(finishedChapters.toSet())
            }
            when (val sleep = _playbackState.value.sleepTimer) {
                is SleepTimerState.Timer -> {
                    if (nowMs >= sleep.endsAtMs) fireSleepTimer()
                }

                is SleepTimerState.EndOfChapter -> {
                    if (absoluteSeconds >= sleep.targetBookSeconds) fireSleepTimer()
                }

                SleepTimerState.Off -> {
                    Unit
                }
            }
            if (controller.isPlaying && currentAudiobookId != null) {
                secondsPlayedSinceLoad++
                secondsSinceServerSave++
                if (secondsSinceServerSave >= ProgressSyncPolicy.INTERVAL_SECONDS) {
                    secondsSinceServerSave = 0
                    saveCurrentProgressAsync()
                }
            }
        }

        fun setPlaybackSpeed(speed: Float) {
            val controller = mediaController ?: return
            controller.setPlaybackSpeed(speed)
            _playbackState.update { it.copy(playbackSpeed = speed) }
        }

        fun startSleepTimer(minutes: Int) {
            cancelSleepTimer()
            val durationMs = minutes.coerceAtLeast(1) * 60_000L
            _playbackState.update {
                it.copy(
                    sleepTimer = SleepTimerState.Timer(endsAtMs = clock.nowMs() + durationMs),
                    sleepRemainingMs = durationMs,
                )
            }
            sleepJob =
                connectionScope.launch {
                    delay(durationMs)
                    fireSleepTimer()
                }
        }

        fun startSleepAtChapterEnd(targetBookSeconds: Double) {
            cancelSleepTimer()
            _playbackState.update {
                it.copy(
                    sleepTimer = SleepTimerState.EndOfChapter(targetBookSeconds),
                    sleepRemainingMs = null,
                )
            }
        }

        fun cancelSleepTimer() {
            sleepJob?.cancel()
            sleepJob = null
            _playbackState.update { it.copy(sleepTimer = SleepTimerState.Off, sleepRemainingMs = null) }
        }

        private fun fireSleepTimer() {
            if (_playbackState.value.sleepTimer is SleepTimerState.Off) return
            sleepJob?.cancel()
            sleepJob = null
            _playbackState.update { it.copy(sleepTimer = SleepTimerState.Off, sleepRemainingMs = null) }
            pause()
        }

        private fun updateState() {
            val controller = mediaController ?: return
            if (_playbackState.value.isPreparing) return
            _playbackState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    isBuffering = controller.playbackState == Player.STATE_BUFFERING && controller.playWhenReady,
                    durationMs = controller.duration,
                    currentPositionMs = controller.currentPosition,
                    playbackSpeed = controller.playbackParameters.speed,
                )
            }
        }

        internal inner class PlayerListener : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                _notices.trySend(PlaybackNotice.Stopped(failureOf(error)))
            }

            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                if (events.containsAny(
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    )
                ) {
                    updateState()
                    if (player.playbackState == Player.STATE_ENDED) {
                        saveCurrentProgressAsync(force = true)
                    }
                }
            }
        }

        companion object {
            private const val TAG = "PlaybackConnection"
            const val SEEK_FORWARD_MS = 30_000L
            const val SEEK_BACKWARD_MS = 15_000L
            private val IO_ERROR_CODES = PlaybackException.ERROR_CODE_IO_UNSPECIFIED until 3000
        }
    }
