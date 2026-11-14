package org.evoionosp.abstrak.presentation.player
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.playback.PlaybackState
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController

@HiltViewModel
class NowPlayingViewModel
    @Inject
    constructor(
        private val playbackConnection: PlaybackConnection,
        private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
        private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val appSettingsStore: AppSettingsStore,
        private val snackbar: SnackbarController,
        private val positionCalculator: PlaybackPositionCalculator = PlaybackPositionCalculator(),
    ) : ViewModel() {
        val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState
        val playingAudiobook: StateFlow<Audiobook?> =
            playbackConnection.playbackState
                .map { it.audiobook }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    playbackConnection.playbackState.value.audiobook,
                )
        val sleepTimerMinutes: StateFlow<Int> =
            appSettingsStore.settings
                .map { it.sleepTimerMinutes }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    AppSettings.DEFAULT_SLEEP_TIMER_MINUTES,
                )
        private val totalSeconds = MutableStateFlow(0.0)
        private val chapters = MutableStateFlow<List<AudiobookChapter>>(emptyList())
        private var detailJob: Job? = null
        val uiState: StateFlow<NowPlayingUiState> =
            combine(
                playbackConnection.playbackState,
                totalSeconds,
                chapters,
            ) { playback, total, chapterList ->
                nowPlayingUiState(playback, total, chapterList, positionCalculator)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

        init {
            viewModelScope.launch {
                playbackConnection.notices.collect { snackbar.show(it.toSnackbarMessage()) }
            }
            viewModelScope.launch {
                playbackConnection.playbackState
                    .map { it.audiobook }
                    .distinctUntilChangedBy { it?.id }
                    .collect { audiobook ->
                        if (audiobook != null) {
                            loadPlaying(audiobook)
                        } else {
                            detailJob?.cancel()
                            chapters.value = emptyList()
                            totalSeconds.value = 0.0
                        }
                    }
            }
        }

        fun togglePlayPause() {
            val playback = playbackState.value
            if (playback.isPlaying || playback.isBuffering) {
                playbackConnection.pause()
            } else {
                playbackConnection.play()
            }
        }

        fun stopPlayback() {
            playbackConnection.stopPlayback()
        }

        fun seekTo(positionMs: Long) {
            playbackConnection.seekTo(positionMs)
        }

        fun seekForward() {
            playbackConnection.seekForward()
        }

        fun seekBackward() {
            playbackConnection.seekBackward()
        }

        fun setPlaybackSpeed(speed: Float) {
            playbackConnection.setPlaybackSpeed(speed)
        }

        fun armSleepTimer(minutes: Int) {
            playbackConnection.startSleepTimer(minutes)
        }

        fun saveSleepTimerMinutes(minutes: Int) {
            viewModelScope.launch {
                appSettingsStore.setSleepTimerMinutes(minutes)
            }
        }

        fun armSleepEndOfChapter() {
            val target =
                sleepChapterTargetSeconds(
                    chapters.value,
                    playbackState.value.currentBookPositionSeconds,
                    totalSeconds.value,
                ) ?: return
            playbackConnection.startSleepAtChapterEnd(target)
        }

        fun cancelSleepTimer() {
            playbackConnection.cancelSleepTimer()
        }

        fun nextChapter() {
            playbackConnection.skipToNextChapter(chapters.value)
        }

        fun previousChapter() {
            playbackConnection.skipToPreviousChapter(chapters.value)
        }

        fun playChapter(chapter: AudiobookChapter) {
            playbackConnection.seekToBookSeconds(chapter.startInSeconds.toDouble())
        }

        private fun loadPlaying(audiobook: Audiobook) {
            detailJob?.cancel()
            chapters.value = emptyList()
            totalSeconds.value = audiobook.durationInSeconds?.toDouble() ?: 0.0
            detailJob =
                viewModelScope.launch {
                    val session = getValidSessionUseCase()
                    if (session != null) {
                        refreshAudiobookDetail(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = audiobook.libraryId,
                            audiobookId = audiobook.id,
                        )
                    }
                    launch {
                        observeAudiobookDetail(audiobook.libraryId, audiobook.id)
                            .collect { detail ->
                                if (detail != null) {
                                    chapters.value = detail.chapters
                                    val total =
                                        detail.audiobook.durationInSeconds?.toDouble()
                                            ?: detail.tracks
                                                .lastOrNull()
                                                ?.let {
                                                    (it.startOffsetInSeconds + it.durationInSeconds)
                                                        .toDouble()
                                                }
                                    if (total != null && total > 0) {
                                        totalSeconds.update { total }
                                    }
                                }
                            }
                    }
                }
        }
    }
