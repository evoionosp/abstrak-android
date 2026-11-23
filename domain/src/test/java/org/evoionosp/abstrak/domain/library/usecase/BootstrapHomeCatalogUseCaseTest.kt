package org.evoionosp.abstrak.domain.library.usecase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapHomeCatalogUseCaseTest {
    @Test
    fun `bootstrap reuses selected library and refreshes audiobooks`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(
                        AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true),
                        AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false),
                    ),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
            val audiobooks = MutableStateFlow(emptyList<Audiobook>())
            val libraryRepository =
                FakeLibraryRepository(
                    libraries = libraries,
                    selectedLibrary = selected,
                    refreshResult = DomainResult.Success(Unit),
                )
            val audiobookRepository =
                FakeAudiobookRepository(
                    audiobooksByLibraryId = mutableMapOf("lib-1" to audiobooks),
                    refreshResult = DomainResult.Success(Unit),
                )
            val result =
                BootstrapHomeCatalogUseCase(
                    libraryRepository = libraryRepository,
                    audiobookRepository = audiobookRepository,
                )(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(
                BootstrapHomeCatalogResult.Success(
                    selectedLibraryId = "lib-1",
                    usedCachedData = false,
                ),
                result,
            )
            assertEquals(listOf("lib-1"), audiobookRepository.refreshedLibraries)
        }

    @Test
    fun `bootstrap falls back to first library when selection missing`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(
                        AudiobookLibrary(id = "lib-1", name = "Main", isSelected = false),
                        AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false),
                    ),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(null)
            val libraryRepository =
                FakeLibraryRepository(
                    libraries = libraries,
                    selectedLibrary = selected,
                    refreshResult = DomainResult.Success(Unit),
                )
            val audiobookRepository =
                FakeAudiobookRepository(
                    audiobooksByLibraryId = mutableMapOf("lib-1" to MutableStateFlow(emptyList())),
                    refreshResult = DomainResult.Success(Unit),
                )
            val result =
                BootstrapHomeCatalogUseCase(libraryRepository, audiobookRepository)(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Success("lib-1", false), result)
            assertEquals("lib-1", libraryRepository.selectedLibrary.value?.id)
        }

    @Test
    fun `bootstrap returns cached success when audiobook refresh fails but cache exists`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true)),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
            val cachedBooks =
                MutableStateFlow(
                    listOf(
                        Audiobook(
                            id = "book-1",
                            libraryId = "lib-1",
                            title = "Title",
                            author = "Author",
                            coverUrl = "https://example.com/cover",
                            series = null,
                            durationInSeconds = null,
                        ),
                    ),
                )
            val result =
                BootstrapHomeCatalogUseCase(
                    libraryRepository =
                        FakeLibraryRepository(
                            libraries,
                            selected,
                            DomainResult.Success(Unit),
                        ),
                    audiobookRepository =
                        FakeAudiobookRepository(
                            audiobooksByLibraryId = mutableMapOf("lib-1" to cachedBooks),
                            refreshResult = DomainResult.Failure(CatalogError.NETWORK),
                        ),
                )(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Success("lib-1", true), result)
        }

    @Test
    fun `bootstrap reports auth failure instead of serving the audiobook cache`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true)),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
            val cachedBooks =
                MutableStateFlow(
                    listOf(
                        Audiobook(
                            id = "book-1",
                            libraryId = "lib-1",
                            title = "Title",
                            author = "Author",
                            coverUrl = "https://example.com/cover",
                            series = null,
                            durationInSeconds = null,
                        ),
                    ),
                )
            val result =
                BootstrapHomeCatalogUseCase(
                    libraryRepository =
                        FakeLibraryRepository(
                            libraries,
                            selected,
                            DomainResult.Success(Unit),
                        ),
                    audiobookRepository =
                        FakeAudiobookRepository(
                            audiobooksByLibraryId = mutableMapOf("lib-1" to cachedBooks),
                            refreshResult = DomainResult.Failure(CatalogError.AUTH),
                        ),
                )(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Failure(CatalogError.AUTH), result)
        }

    @Test
    fun `bootstrap reports auth failure from library refresh even with cached libraries`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true)),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
            val result =
                BootstrapHomeCatalogUseCase(
                    libraryRepository =
                        FakeLibraryRepository(
                            libraries = libraries,
                            selectedLibrary = selected,
                            refreshResult = DomainResult.Failure(CatalogError.AUTH),
                        ),
                    audiobookRepository =
                        FakeAudiobookRepository(
                            audiobooksByLibraryId =
                                mutableMapOf("lib-1" to MutableStateFlow(emptyList())),
                            refreshResult = DomainResult.Success(Unit),
                        ),
                )(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Failure(CatalogError.AUTH), result)
        }

    @Test
    fun `bootstrap returns no libraries when sync leaves library cache empty`() =
        runTest {
            val result =
                BootstrapHomeCatalogUseCase(
                    libraryRepository =
                        FakeLibraryRepository(
                            libraries = MutableStateFlow(emptyList()),
                            selectedLibrary = MutableStateFlow(null),
                            refreshResult =
                                DomainResult.Failure(
                                    CatalogError.NO_AUDIOBOOK_LIBRARIES,
                                ),
                        ),
                    audiobookRepository =
                        FakeAudiobookRepository(
                            audiobooksByLibraryId = mutableMapOf(),
                            refreshResult = DomainResult.Success(Unit),
                        ),
                )(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertTrue(result is BootstrapHomeCatalogResult.NoLibrariesAvailable)
        }
}

class BootstrapHomeCatalogFromCacheTest {
    @Test
    fun `a library that synced before opens from the device without the server`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(
                        AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true),
                        AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false),
                    ),
                )
            val libraryRepository =
                FakeLibraryRepository(
                    libraries = libraries,
                    selectedLibrary = MutableStateFlow(libraries.value.first()),
                    refreshResult = DomainResult.Success(Unit),
                )
            val audiobookRepository =
                FakeAudiobookRepository(
                    audiobooksByLibraryId = mutableMapOf(),
                    refreshResult = DomainResult.Success(Unit),
                )
            audiobookRepository.syncStatusByLibrary["lib-1"] = SyncStatus.Success(lastSyncedAtMillis = 1_000L)
            val result =
                BootstrapHomeCatalogUseCase(libraryRepository, audiobookRepository)(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Success(selectedLibraryId = "lib-1", usedCachedData = true), result)
            assertEquals(0, libraryRepository.refreshCount)
            assertEquals(emptyList<String>(), audiobookRepository.refreshedLibraries)
            assertEquals(emptyList<String>(), audiobookRepository.refreshedContinueListeningLibraries)
        }

    @Test
    fun `with no library selected the first one opens from the device, selected`() =
        runTest {
            val libraries =
                MutableStateFlow(
                    listOf(
                        AudiobookLibrary(id = "lib-1", name = "Main", isSelected = false),
                        AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false),
                    ),
                )
            val selected = MutableStateFlow<AudiobookLibrary?>(null)
            val libraryRepository =
                FakeLibraryRepository(
                    libraries = libraries,
                    selectedLibrary = selected,
                    refreshResult = DomainResult.Success(Unit),
                )
            val audiobookRepository =
                FakeAudiobookRepository(
                    audiobooksByLibraryId = mutableMapOf(),
                    refreshResult = DomainResult.Success(Unit),
                )
            audiobookRepository.syncStatusByLibrary["lib-1"] =
                SyncStatus.Stale(lastSyncedAtMillis = 1_000L, reason = CatalogError.NETWORK)
            val result =
                BootstrapHomeCatalogUseCase(libraryRepository, audiobookRepository)(
                    baseUrl = "https://example.com",
                    accessToken = "token",
                )
            assertEquals(BootstrapHomeCatalogResult.Success(selectedLibraryId = "lib-1", usedCachedData = true), result)
            assertEquals("lib-1", selected.value?.id)
            assertEquals(0, libraryRepository.refreshCount)
        }
}

private class FakeLibraryRepository(
    val libraries: MutableStateFlow<List<AudiobookLibrary>>,
    val selectedLibrary: MutableStateFlow<AudiobookLibrary?>,
    private val refreshResult: DomainResult<Unit>,
) : LibraryRepository {
    override fun observeLibraries(): Flow<List<AudiobookLibrary>> = libraries

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> = selectedLibrary

    var refreshCount = 0
        private set

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String,
    ): DomainResult<Unit> {
        refreshCount++
        return refreshResult
    }

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        val library =
            libraries.value.firstOrNull { it.id == libraryId }
                ?: return DomainResult.Failure(CatalogError.NOT_FOUND)
        val updatedLibraries =
            libraries.value.map { item ->
                item.copy(isSelected = item.id == libraryId)
            }
        libraries.value = updatedLibraries
        selectedLibrary.value = library.copy(isSelected = true)
        return DomainResult.Success(Unit)
    }
}

private class FakeAudiobookRepository(
    val audiobooksByLibraryId: MutableMap<String, MutableStateFlow<List<Audiobook>>>,
    private val refreshResult: DomainResult<Unit>,
) : AudiobookRepository {
    val refreshedLibraries = mutableListOf<String>()
    val refreshedContinueListeningLibraries = mutableListOf<String>()
    val syncStatusByLibrary = mutableMapOf<String, SyncStatus>()

    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> =
        audiobooksByLibraryId.getOrPut(libraryId) {
            MutableStateFlow(emptyList())
        }

    override fun observeAllAudiobooks(): Flow<List<Audiobook>> = MutableStateFlow(emptyList())

    override fun observeAudiobook(
        libraryId: String,
        audiobookId: String,
    ): Flow<Audiobook?> = MutableStateFlow(null)

    override fun observeAudiobookDetail(
        libraryId: String,
        audiobookId: String,
    ): Flow<AudiobookDetail?> = MutableStateFlow(null)

    override fun observeContinueListening(libraryId: String): Flow<List<Audiobook>> = MutableStateFlow(emptyList())

    override suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String,
    ): DomainResult<Unit> = DomainResult.Success(Unit)

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> =
        MutableStateFlow(syncStatusByLibrary[libraryId] ?: SyncStatus.Idle)

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedLibraries += libraryId
        return refreshResult
    }

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedContinueListeningLibraries += libraryId
        return DomainResult.Success(Unit)
    }

    override suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
    ): DomainResult<PlaybackProgress?> = DomainResult.Success(null)

    override suspend fun recordListeningProgress(
        libraryId: String,
        audiobookId: String,
        positionSeconds: Double,
        isFinished: Boolean,
    ) = Unit

    override suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress,
    ): DomainResult<Unit> = DomainResult.Success(Unit)
}
