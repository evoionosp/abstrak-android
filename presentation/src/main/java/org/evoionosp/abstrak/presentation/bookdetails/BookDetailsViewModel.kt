package org.evoionosp.abstrak.presentation.bookdetails
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.abstrak.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.abstrak.domain.download.DownloadScopeCalculator
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.usecase.CancelBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.CancelChapterDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DeleteBookDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DeleteChapterDownloadUseCase
import org.evoionosp.abstrak.domain.download.usecase.DownloadBookUseCase
import org.evoionosp.abstrak.domain.download.usecase.DownloadChapterUseCase
import org.evoionosp.abstrak.domain.download.usecase.ObserveBookDownloadsUseCase
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.presentation.navigation.BOOK_DETAILS_ARG_AUDIOBOOK_ID
import org.evoionosp.abstrak.presentation.navigation.BOOK_DETAILS_ARG_LIBRARY_ID
import org.evoionosp.abstrak.presentation.ui.download.DownloadUiState

sealed interface DeleteDownloadTarget {
    val name: String
    val bytes: Long

    data class Chapter(
        val index: Int,
        override val name: String,
        override val bytes: Long,
    ) : DeleteDownloadTarget

    data class Book(
        override val name: String,
        override val bytes: Long,
    ) : DeleteDownloadTarget
}

data class BookDetailsUiState(
    val audiobook: Audiobook? = null,
    val chapters: List<AudiobookChapter> = emptyList(),
    val progressSeconds: Double = 0.0,
    val currentChapterIndex: Int = -1,
    val totalSeconds: Double = 0.0,
    val chapterDownloads: List<DownloadUiState> = emptyList(),
    val bookDownload: DownloadUiState = DownloadUiState(),
    val pendingDelete: DeleteDownloadTarget? = null,
    val pendingChapterSwitch: AudiobookChapter? = null,
) {
    val bookProgress: Float =
        if (totalSeconds > 0) {
            (progressSeconds / totalSeconds).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    val hasProgress: Boolean = progressSeconds > 0
}

internal fun resolveDetailsProgress(
    isPlayingBook: Boolean,
    livePositionSeconds: Double,
    savedProgressSeconds: Double,
): Double = if (isPlayingBook) livePositionSeconds else savedProgressSeconds

@HiltViewModel
class BookDetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val playbackConnection: PlaybackConnection,
        private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
        private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
        private val fetchPlaybackProgress: FetchPlaybackProgressUseCase,
        private val observeContinueListening: ObserveContinueListeningUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val observeBookDownloads: ObserveBookDownloadsUseCase,
        private val downloadBook: DownloadBookUseCase,
        private val downloadChapter: DownloadChapterUseCase,
        private val cancelBookDownload: CancelBookDownloadUseCase,
        private val cancelChapterDownload: CancelChapterDownloadUseCase,
        private val deleteBookDownload: DeleteBookDownloadUseCase,
        private val deleteChapterDownload: DeleteChapterDownloadUseCase,
        private val scopeCalculator: DownloadScopeCalculator,
        private val positionCalculator: PlaybackPositionCalculator = PlaybackPositionCalculator(),
    ) : ViewModel() {
        private val libraryId: String = checkNotNull(savedStateHandle[BOOK_DETAILS_ARG_LIBRARY_ID])
        private val audiobookId: String = checkNotNull(savedStateHandle[BOOK_DETAILS_ARG_AUDIOBOOK_ID])
        private val book = MutableStateFlow<Audiobook?>(null)
        private val chapters = MutableStateFlow<List<AudiobookChapter>>(emptyList())
        private val tracks = MutableStateFlow<List<AudiobookTrack>>(emptyList())
        private val downloads = observeBookDownloads(audiobookId)
        private val savedProgress = MutableStateFlow(0.0)
        private val totalSeconds = MutableStateFlow(0.0)
        private val isFinished = MutableStateFlow(false)
        private val pendingDelete = MutableStateFlow<DeleteDownloadTarget?>(null)
        private val pendingChapterSwitch = MutableStateFlow<AudiobookChapter?>(null)
        private val _events = MutableSharedFlow<BookDetailsUiEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<BookDetailsUiEvent> = _events.asSharedFlow()
        private var loadJob: Job? = null

        private data class DownloadInputs(
            val tracks: List<AudiobookTrack>,
            val downloads: List<TrackDownload>,
            val totalSeconds: Double,
            val isFinished: Boolean,
            val pendingDelete: DeleteDownloadTarget?,
        )

        private val downloadInputs: StateFlow<DownloadInputs> =
            combine(
                tracks,
                downloads,
                totalSeconds,
                isFinished,
                pendingDelete,
            ) { trackList, downloadList, total, finished, deleteTarget ->
                DownloadInputs(trackList, downloadList, total, finished, deleteTarget)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DownloadInputs(emptyList(), emptyList(), 0.0, false, null),
            )
        val uiState: StateFlow<BookDetailsUiState> =
            combine(
                playbackConnection.playbackState,
                book,
                chapters,
                savedProgress,
                downloadInputs,
            ) { playback, book, chapterList, saved, inputs ->
                val isPlayingBook = book != null && playback.audiobook?.id == book.id
                val progress =
                    resolveDetailsProgress(
                        isPlayingBook,
                        playback.currentBookPositionSeconds,
                        saved,
                    )
                BookDetailsUiState(
                    audiobook = book,
                    chapters = chapterList,
                    progressSeconds = progress,
                    currentChapterIndex = positionCalculator.chapterIndexAt(progress, chapterList),
                    totalSeconds = inputs.totalSeconds,
                    chapterDownloads =
                        chapterDownloadUiStates(
                            chapters = chapterList,
                            tracks = inputs.tracks,
                            downloads = inputs.downloads,
                            totalSeconds = inputs.totalSeconds,
                            calculator = scopeCalculator,
                        ),
                    bookDownload =
                        bookDownloadUiState(
                            chapters = chapterList,
                            tracks = inputs.tracks,
                            downloads = inputs.downloads,
                            positionSeconds = progress,
                            isFinished = inputs.isFinished,
                            calculator = scopeCalculator,
                        ),
                    pendingDelete = inputs.pendingDelete,
                )
            }.combine(pendingChapterSwitch) { state, chapter ->
                state.copy(pendingChapterSwitch = chapter)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailsUiState())

        init {
            loadBook()
        }

        fun playBook() {
            val book = book.value ?: return
            if (playbackConnection.playbackState.value.audiobook
                    ?.id == book.id
            ) {
                playbackConnection.play()
            } else {
                val progress = uiState.value.progressSeconds.takeIf { it > 0 }
                playbackConnection.playAudiobook(book.copy(progressSeconds = progress))
            }
            _events.tryEmit(BookDetailsUiEvent.OpenPlayer)
        }

        fun playChapter(chapter: AudiobookChapter) {
            val state = uiState.value
            when {
                chapter == state.chapters.getOrNull(state.currentChapterIndex) -> playBook()
                state.progressSeconds > 0 -> pendingChapterSwitch.value = chapter
                else -> startChapter(chapter)
            }
        }

        fun confirmChapterSwitch() {
            val chapter = pendingChapterSwitch.value ?: return
            pendingChapterSwitch.value = null
            startChapter(chapter)
        }

        fun dismissChapterSwitch() {
            pendingChapterSwitch.value = null
        }

        private fun startChapter(chapter: AudiobookChapter) {
            val book = book.value ?: return
            val start = chapter.startInSeconds.toDouble()
            if (playbackConnection.playbackState.value.audiobook
                    ?.id == book.id
            ) {
                playbackConnection.seekToBookSeconds(start)
                playbackConnection.play()
            } else {
                playbackConnection.playAudiobook(book, startPositionSeconds = start)
            }
            _events.tryEmit(BookDetailsUiEvent.OpenPlayer)
        }

        fun downloadBook() {
            val book = book.value ?: return
            viewModelScope.launch {
                downloadBook(
                    libraryId = libraryId,
                    audiobookId = book.id,
                    positionSeconds = uiState.value.progressSeconds,
                    isFinished = isFinished.value,
                )
            }
        }

        fun cancelBookDownload() {
            val book = book.value ?: return
            viewModelScope.launch {
                cancelBookDownload(book.id)
            }
        }

        fun downloadChapter(index: Int) {
            val book = book.value ?: return
            viewModelScope.launch {
                downloadChapter(
                    libraryId = libraryId,
                    audiobookId = book.id,
                    chapterIndex = index,
                )
            }
        }

        fun cancelChapterDownload(index: Int) {
            val book = book.value ?: return
            viewModelScope.launch {
                cancelChapterDownload(
                    libraryId = libraryId,
                    audiobookId = book.id,
                    chapterIndex = index,
                )
            }
        }

        fun requestDeleteChapter(index: Int) {
            val chapter = chapters.value.getOrNull(index) ?: return
            val bytes =
                uiState.value.chapterDownloads
                    .getOrNull(index)
                    ?.downloadedBytes
                    ?: 0L
            pendingDelete.value = DeleteDownloadTarget.Chapter(index, chapter.title, bytes)
        }

        fun requestDeleteBook() {
            val book = book.value ?: return
            pendingDelete.value =
                DeleteDownloadTarget.Book(book.title, uiState.value.bookDownload.downloadedBytes)
        }

        fun confirmDelete() {
            val book = book.value ?: return
            val target = pendingDelete.value ?: return
            when (target) {
                is DeleteDownloadTarget.Chapter -> {
                    viewModelScope.launch {
                        deleteChapterDownload(
                            libraryId = libraryId,
                            audiobookId = book.id,
                            chapterIndex = target.index,
                        )
                        pendingDelete.value = null
                    }
                }

                is DeleteDownloadTarget.Book -> {
                    viewModelScope.launch {
                        deleteBookDownload(book.id)
                        pendingDelete.value = null
                    }
                }
            }
        }

        fun dismissDelete() {
            pendingDelete.value = null
        }

        private fun loadBook() {
            loadJob?.cancel()
            chapters.value = emptyList()
            loadJob =
                viewModelScope.launch {
                    launch {
                        observeAudiobookDetail(libraryId, audiobookId)
                            .collect { detail ->
                                if (detail != null) {
                                    book.value = detail.audiobook
                                    chapters.value = detail.chapters
                                    tracks.value = detail.tracks
                                    val total =
                                        scopeCalculator.totalDurationSeconds(
                                            tracks = detail.tracks,
                                            declaredDurationSeconds = detail.audiobook.durationInSeconds,
                                        )
                                    if (total != null && total > 0) {
                                        totalSeconds.update { total }
                                    }
                                }
                            }
                    }
                    observeContinueListening(libraryId)
                        .first()
                        .firstOrNull { it.id == audiobookId }
                        ?.progressSeconds
                        ?.let { cached -> savedProgress.update { cached } }
                    val session = getValidSessionUseCase() ?: return@launch
                    refreshAudiobookDetail(
                        baseUrl = session.baseUrl,
                        accessToken = session.accessToken,
                        libraryId = libraryId,
                        audiobookId = audiobookId,
                    )
                    val progress =
                        fetchPlaybackProgress(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            audiobookId = audiobookId,
                        )
                    val resume = progress?.resumeSeconds
                    if (progress != null && resume != null) {
                        savedProgress.update { resume }
                        isFinished.update { progress.isFinished }
                        if (totalSeconds.value <= 0) {
                            totalSeconds.update { progress.durationSeconds ?: 0.0 }
                        }
                    } else if (progress != null) {
                        savedProgress.update { 0.0 }
                    }
                }
        }
    }
