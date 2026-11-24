package org.evoionosp.abstrak.domain.testing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.audiobook.model.PendingProgress
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.audiobook.repository.PendingProgressStore
import org.evoionosp.abstrak.domain.audiobook.sync.ProgressSyncScheduler
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver
import org.evoionosp.abstrak.domain.download.model.BookDownloadSummary
import org.evoionosp.abstrak.domain.download.model.DownloadStatus
import org.evoionosp.abstrak.domain.download.model.DownloadedBook
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.model.TrackDownload
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository
import org.evoionosp.abstrak.domain.server.model.ServerCheckResult
import org.evoionosp.abstrak.domain.server.model.ServerStatus
import org.evoionosp.abstrak.domain.server.repository.ServerRepository
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

class FakeLibraryRepository(
    libraries: List<AudiobookLibrary> = emptyList(),
    selectedLibrary: AudiobookLibrary? = null,
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var selectResultOverride: DomainResult<Unit>? = null,
) : LibraryRepository {
    val librariesFlow = MutableStateFlow(libraries)
    val selectedLibraryFlow = MutableStateFlow(selectedLibrary)
    var refreshCallCount = 0
        private set
    val selectedLibraryIds = mutableListOf<String>()

    override fun observeLibraries(): Flow<List<AudiobookLibrary>> = librariesFlow

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> = selectedLibraryFlow

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String,
    ): DomainResult<Unit> {
        refreshCallCount++
        return refreshResult
    }

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        selectedLibraryIds += libraryId
        selectResultOverride?.let { return it }
        val library =
            librariesFlow.value.firstOrNull { it.id == libraryId }
                ?: return DomainResult.Failure(org.evoionosp.abstrak.domain.library.model.CatalogError.NOT_FOUND)
        librariesFlow.value = librariesFlow.value.map { it.copy(isSelected = it.id == libraryId) }
        selectedLibraryFlow.value = library.copy(isSelected = true)
        return DomainResult.Success(Unit)
    }
}

class FakeAudiobookRepository(
    var refreshBooksResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var refreshContinueResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var refreshDetailResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var fetchProgressResult: DomainResult<PlaybackProgress?> = DomainResult.Success(null),
    var saveProgressResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var fetchProgressByBook: Map<String, DomainResult<PlaybackProgress?>> = emptyMap(),
    var saveProgressByBook: Map<String, DomainResult<Unit>> = emptyMap(),
    var onRefreshDetail: (suspend (libraryId: String, audiobookId: String) -> Unit)? = null,
    var onRefreshBooks: (suspend (libraryId: String) -> Unit)? = null,
) : AudiobookRepository {
    private val booksFlows = mutableMapOf<String, MutableStateFlow<List<Audiobook>>>()
    private val detailFlows = mutableMapOf<Pair<String, String>, MutableStateFlow<AudiobookDetail?>>()
    val allBooksFlow = MutableStateFlow<List<Audiobook>>(emptyList())
    var continueListening: List<Audiobook> = emptyList()
    var syncStatus: SyncStatus = SyncStatus.Idle
    val syncStatusByLibrary = mutableMapOf<String, SyncStatus>()
    val refreshedBookLibraries = mutableListOf<String>()
    val refreshedContinueLibraries = mutableListOf<String>()
    val refreshedDetails = mutableListOf<Pair<String, String>>()
    val savedProgress = mutableListOf<PlaybackProgress>()
    val savedProgressBooks = mutableListOf<String>()
    val fetchedProgressBooks = mutableListOf<String>()
    val recordedProgress = mutableListOf<RecordedListeningProgress>()

    fun setAudiobooks(
        libraryId: String,
        books: List<Audiobook>,
    ) {
        booksFlows.getOrPut(libraryId) { MutableStateFlow(emptyList()) }.value = books
        allBooksFlow.value = booksFlows.values.flatMap { it.value }
    }

    fun setDetail(
        libraryId: String,
        audiobookId: String,
        detail: AudiobookDetail?,
    ) {
        detailFlows.getOrPut(libraryId to audiobookId) { MutableStateFlow(null) }.value = detail
    }

    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> =
        booksFlows.getOrPut(libraryId) { MutableStateFlow(emptyList()) }

    override fun observeAllAudiobooks(): Flow<List<Audiobook>> = allBooksFlow

    override fun observeAudiobook(
        libraryId: String,
        audiobookId: String,
    ): Flow<Audiobook?> = MutableStateFlow(null)

    override fun observeAudiobookDetail(
        libraryId: String,
        audiobookId: String,
    ): Flow<AudiobookDetail?> = detailFlows.getOrPut(libraryId to audiobookId) { MutableStateFlow(null) }

    override fun observeContinueListening(libraryId: String): Flow<List<Audiobook>> =
        MutableStateFlow(continueListening)

    override suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String,
    ): DomainResult<Unit> {
        refreshedDetails += libraryId to audiobookId
        if (refreshDetailResult is DomainResult.Success) {
            onRefreshDetail?.invoke(libraryId, audiobookId)
        }
        return refreshDetailResult
    }

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> =
        MutableStateFlow(syncStatusByLibrary[libraryId] ?: syncStatus)

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedBookLibraries += libraryId
        onRefreshBooks?.invoke(libraryId)
        return refreshBooksResult
    }

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedContinueLibraries += libraryId
        return refreshContinueResult
    }

    override suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
    ): DomainResult<PlaybackProgress?> {
        fetchedProgressBooks += audiobookId
        return fetchProgressByBook[audiobookId] ?: fetchProgressResult
    }

    override suspend fun recordListeningProgress(
        libraryId: String,
        audiobookId: String,
        positionSeconds: Double,
        isFinished: Boolean,
    ) {
        recordedProgress += RecordedListeningProgress(libraryId, audiobookId, positionSeconds, isFinished)
    }

    override suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress,
    ): DomainResult<Unit> {
        savedProgress += progress
        savedProgressBooks += audiobookId
        return saveProgressByBook[audiobookId] ?: saveProgressResult
    }
}

/**
 * Keeps the time each progress carries, as the real store does. Progress with no time gets the
 * next tick of a counter starting at 1, standing in for the real store's current-time fallback.
 */
class FakePendingProgressStore : PendingProgressStore {
    private val rows = LinkedHashMap<String, PendingProgress>()
    private var lastTick = 0L
    var beforeRemove: (suspend (PendingProgress) -> Unit)? = null
    val queued: List<PendingProgress> get() = rows.values.sortedBy { it.progress.lastUpdateMillis }

    override suspend fun put(
        libraryId: String,
        audiobookId: String,
        progress: PlaybackProgress,
    ) {
        val dated = if (progress.lastUpdateMillis > 0L) progress else progress.copy(lastUpdateMillis = ++lastTick)
        rows[audiobookId] = PendingProgress(libraryId, audiobookId, dated)
    }

    override suspend fun get(audiobookId: String): PendingProgress? = rows[audiobookId]

    override suspend fun getAll(): List<PendingProgress> = queued

    override suspend fun remove(pending: PendingProgress): Boolean {
        beforeRemove?.invoke(pending)
        if (rows[pending.audiobookId] != pending) return false
        rows.remove(pending.audiobookId)
        return true
    }
}

class FakeProgressSyncScheduler : ProgressSyncScheduler {
    var scheduleCount = 0
        private set
    var scheduleNowCount = 0
        private set

    override fun schedule() {
        scheduleCount++
    }

    override fun scheduleNow() {
        scheduleNowCount++
    }
}

data class RecordedListeningProgress(
    val libraryId: String,
    val audiobookId: String,
    val positionSeconds: Double,
    val isFinished: Boolean,
)

class FakeDownloadRepository : DownloadRepository {
    private val trackFlows = mutableMapOf<String, MutableStateFlow<List<TrackDownload>>>()
    private val summaryFlows = mutableMapOf<String, MutableStateFlow<BookDownloadSummary?>>()
    val downloadedBooksFlow = MutableStateFlow<List<DownloadedBook>>(emptyList())
    val storageFlow = MutableStateFlow(StorageUsage(downloadsBytes = 0L, cacheBytes = 0L, deviceFreeBytes = 0L))
    val enqueued = mutableListOf<Triple<String, List<Int>, Boolean>>()
    val cancelledBooks = mutableListOf<String>()
    val cancelledTracks = mutableListOf<Pair<String, List<Int>>>()
    val retriedBooks = mutableListOf<String>()
    val deletedBooks = mutableListOf<String>()
    val deletedTracks = mutableListOf<Pair<String, Int>>()
    var deleteAllCount = 0
        private set
    val localFiles = mutableMapOf<Pair<String, Int>, String>()
    var reconcileCount = 0

    fun setTracks(
        audiobookId: String,
        downloads: List<TrackDownload>,
    ) {
        trackFlows.getOrPut(audiobookId) { MutableStateFlow(emptyList()) }.value = downloads
    }

    fun setSummary(
        audiobookId: String,
        summary: BookDownloadSummary?,
    ) {
        summaryFlows.getOrPut(audiobookId) { MutableStateFlow(null) }.value = summary
    }

    override fun observeTrackDownloads(audiobookId: String): Flow<List<TrackDownload>> =
        trackFlows.getOrPut(audiobookId) { MutableStateFlow(emptyList()) }

    override fun observeBookSummary(audiobookId: String): Flow<BookDownloadSummary?> =
        summaryFlows.getOrPut(audiobookId) { MutableStateFlow(null) }

    override fun observeDownloadedBooks(): Flow<List<DownloadedBook>> = downloadedBooksFlow

    override fun observeStorageUsage(): Flow<StorageUsage> = storageFlow

    override suspend fun enqueueTracks(
        audiobookId: String,
        trackIndexes: List<Int>,
        wifiOnly: Boolean,
    ) {
        enqueued += Triple(audiobookId, trackIndexes, wifiOnly)
    }

    override suspend fun cancelBook(audiobookId: String) {
        cancelledBooks += audiobookId
    }

    override suspend fun cancelTracks(
        audiobookId: String,
        trackIndexes: List<Int>,
    ) {
        cancelledTracks += audiobookId to trackIndexes
    }

    override suspend fun retryBook(audiobookId: String) {
        retriedBooks += audiobookId
    }

    override suspend fun deleteBook(audiobookId: String) {
        deletedBooks += audiobookId
    }

    override suspend fun deleteTrack(
        audiobookId: String,
        trackIndex: Int,
    ) {
        deletedTracks += audiobookId to trackIndex
    }

    override suspend fun deleteAll() {
        deleteAllCount++
    }

    override suspend fun localFileForTrack(
        audiobookId: String,
        trackIndex: Int,
    ): String? = localFiles[audiobookId to trackIndex]

    override suspend fun reconcileStorage() {
        reconcileCount++
    }

    var pauseCount = 0
        private set
    var resumeCount = 0
        private set

    override suspend fun pauseDownloads() {
        pauseCount++
    }

    override suspend fun resumeDownloads() {
        resumeCount++
    }
}

class FakeAppSettingsStore(
    initial: AppSettings = AppSettings(),
) : AppSettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state

    override suspend fun setThemePreference(themePreference: String) {
        state.value = state.value.copy(themePreference = themePreference)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(useDynamicColor = enabled)
    }

    override suspend fun setCoverTheme(enabled: Boolean) {
        state.value = state.value.copy(useCoverTheme = enabled)
    }

    override suspend fun setMoonlessNight(enabled: Boolean) {
        state.value = state.value.copy(useMoonlessNight = enabled)
    }

    override suspend fun setSleepTimerMinutes(minutes: Int) {
        state.value = state.value.copy(sleepTimerMinutes = minutes)
    }

    override suspend fun setSearchAllLibraries(enabled: Boolean) {
        state.value = state.value.copy(searchAllLibraries = enabled)
    }

    override suspend fun setDownloadWifiOnly(enabled: Boolean) {
        state.value = state.value.copy(downloadWifiOnly = enabled)
    }

    override suspend fun setAutoDeleteFinishedDownloads(enabled: Boolean) {
        state.value = state.value.copy(autoDeleteFinishedDownloads = enabled)
    }

    override suspend fun setOfflineMode(enabled: Boolean) {
        state.value = state.value.copy(useOfflineMode = enabled)
    }

    override suspend fun setAppIcon(icon: AppIcon) {
        state.value = state.value.copy(appIcon = icon)
    }
}

class FakeServerRepository(
    var pingResult: ServerCheckResult<Boolean> = ServerCheckResult.Success(true),
    var statusResult: ServerCheckResult<ServerStatus> =
        ServerCheckResult.Success(
            ServerStatus(
                app = "audiobookshelf",
                serverVersion = "2.0.0",
                isInit = true,
                language = "en-US",
                authMethods = listOf("local"),
                authLoginCustomMessage = "",
            ),
        ),
    var healthResult: ServerCheckResult<Boolean> = ServerCheckResult.Success(true),
) : ServerRepository {
    var pingCallCount = 0
        private set
    var statusCallCount = 0
        private set
    var healthCallCount = 0
        private set

    override suspend fun ping(baseUrl: String): ServerCheckResult<Boolean> {
        pingCallCount++
        return pingResult
    }

    override suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus> {
        statusCallCount++
        return statusResult
    }

    override suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean> {
        healthCallCount++
        return healthResult
    }
}

class FakeConnectivityObserver(
    initiallyConnected: Boolean = false,
) : ConnectivityObserver {
    private val connected = MutableStateFlow(initiallyConnected)

    override fun observe(): Flow<Boolean> = connected

    override fun isConnected(): Boolean = connected.value

    fun setConnected(value: Boolean) {
        connected.value = value
    }
}

fun testLibrary(
    id: String = "lib-1",
    name: String = "Library $id",
    isSelected: Boolean = false,
) = AudiobookLibrary(
    id = id,
    name = name,
    isSelected = isSelected,
)

fun testAudiobook(
    id: String = "book-1",
    libraryId: String = "lib-1",
) = Audiobook(
    id = id,
    libraryId = libraryId,
    title = "Title $id",
    author = "Author",
    coverUrl = "https://example.com/$id.jpg",
    series = null,
    durationInSeconds = 300,
)

fun testTrack(
    index: Int = 0,
    startOffsetInSeconds: Long = 0,
    durationInSeconds: Long = 300,
) = AudiobookTrack(
    index = index,
    startOffsetInSeconds = startOffsetInSeconds,
    durationInSeconds = durationInSeconds,
    title = "Track $index",
    remoteUrl = "https://example.com/$index.mp3",
    mimeType = "audio/mpeg",
)

fun testDetail(
    audiobook: Audiobook = testAudiobook(),
    tracks: List<AudiobookTrack> = listOf(testTrack()),
    chapters: List<AudiobookChapter> = emptyList(),
) = AudiobookDetail(
    audiobook = audiobook,
    description = null,
    chapters = chapters,
    tracks = tracks,
    refreshedAtMillis = 0L,
)

fun testChapter(
    title: String = "Chapter",
    startInSeconds: Long = 0L,
    endInSeconds: Long? = null,
) = AudiobookChapter(
    title = title,
    startInSeconds = startInSeconds,
    endInSeconds = endInSeconds,
)

fun testTrackDownload(
    audiobookId: String = "book-1",
    trackIndex: Int = 0,
    status: DownloadStatus = DownloadStatus.QUEUED,
    bytesDownloaded: Long = 0L,
    totalBytes: Long? = null,
    localPath: String? = null,
) = TrackDownload(
    audiobookId = audiobookId,
    trackIndex = trackIndex,
    status = status,
    bytesDownloaded = bytesDownloaded,
    totalBytes = totalBytes,
    localPath = localPath,
)

fun testBookDownloadSummary(
    audiobookId: String = "book-1",
    totalTracks: Int = 1,
    completedTracks: Int = 0,
    activeTracks: Int = 0,
    failedTracks: Int = 0,
    bytesDownloaded: Long = 0L,
    totalBytes: Long? = null,
    status: DownloadStatus = DownloadStatus.QUEUED,
) = BookDownloadSummary(
    audiobookId = audiobookId,
    totalTracks = totalTracks,
    completedTracks = completedTracks,
    activeTracks = activeTracks,
    failedTracks = failedTracks,
    bytesDownloaded = bytesDownloaded,
    totalBytes = totalBytes,
    status = status,
)
