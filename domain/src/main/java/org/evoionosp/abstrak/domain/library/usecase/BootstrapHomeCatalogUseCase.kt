package org.evoionosp.abstrak.domain.library.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository

class BootstrapHomeCatalogUseCase
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
        private val audiobookRepository: AudiobookRepository,
    ) {
        /**
         * Gets the home catalog ready for startup. A library that has synced before opens as it is,
         * without waiting on the server: the daily library sync and the Continue Listening refresh
         * run in the background. Only a first start (or one after a failed first sync) loads from
         * the server here.
         */
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
        ): BootstrapHomeCatalogResult {
            val cachedSelection =
                libraryRepository.observeSelectedLibrary().first()
                    ?: libraryRepository.observeLibraries().first().firstOrNull()
            if (cachedSelection != null && hasSynced(cachedSelection.id)) {
                return openCached(cachedSelection)
            }
            val libraryRefreshResult =
                libraryRepository.refreshLibraries(
                    baseUrl = baseUrl,
                    accessToken = accessToken,
                )
            if (libraryRefreshResult.isAuthFailure()) {
                return BootstrapHomeCatalogResult.Failure(CatalogError.AUTH)
            }
            val libraries = libraryRepository.observeLibraries().first()
            if (libraries.isEmpty()) {
                return when (libraryRefreshResult) {
                    is DomainResult.Failure -> {
                        if (libraryRefreshResult.error == CatalogError.NO_AUDIOBOOK_LIBRARIES) {
                            BootstrapHomeCatalogResult.NoLibrariesAvailable
                        } else {
                            BootstrapHomeCatalogResult.Failure(libraryRefreshResult.error)
                        }
                    }

                    is DomainResult.Success -> {
                        BootstrapHomeCatalogResult.NoLibrariesAvailable
                    }
                }
            }
            val selectedLibrary =
                libraryRepository.observeSelectedLibrary().first()
                    ?: libraries.first()
            if (!selectedLibrary.isSelected) {
                val selectionResult = libraryRepository.selectLibrary(selectedLibrary.id)
                if (selectionResult is DomainResult.Failure) {
                    return BootstrapHomeCatalogResult.Failure(selectionResult.error)
                }
            }
            val audiobookRefreshResult =
                audiobookRepository.refreshAudiobooks(
                    baseUrl = baseUrl,
                    accessToken = accessToken,
                    libraryId = selectedLibrary.id,
                )
            audiobookRepository.refreshContinueListening(
                baseUrl = baseUrl,
                accessToken = accessToken,
                libraryId = selectedLibrary.id,
            )
            val cachedAudiobooks = audiobookRepository.observeAudiobooks(selectedLibrary.id).first()
            return when (audiobookRefreshResult) {
                is DomainResult.Success -> {
                    BootstrapHomeCatalogResult.Success(
                        selectedLibraryId = selectedLibrary.id,
                        usedCachedData = false,
                    )
                }

                is DomainResult.Failure -> {
                    if (cachedAudiobooks.isNotEmpty() && !audiobookRefreshResult.isAuthFailure()) {
                        BootstrapHomeCatalogResult.Success(
                            selectedLibraryId = selectedLibrary.id,
                            usedCachedData = true,
                        )
                    } else {
                        BootstrapHomeCatalogResult.Failure(audiobookRefreshResult.error)
                    }
                }
            }
        }

        private suspend fun hasSynced(libraryId: String): Boolean =
            audiobookRepository.observeLibrarySyncStatus(libraryId).first().lastSyncedAtMillis != null

        private suspend fun openCached(library: AudiobookLibrary): BootstrapHomeCatalogResult {
            if (!library.isSelected) {
                val selectionResult = libraryRepository.selectLibrary(library.id)
                if (selectionResult is DomainResult.Failure) {
                    return BootstrapHomeCatalogResult.Failure(selectionResult.error)
                }
            }
            return BootstrapHomeCatalogResult.Success(selectedLibraryId = library.id, usedCachedData = true)
        }

        private fun DomainResult<Unit>.isAuthFailure(): Boolean =
            this is DomainResult.Failure && error == CatalogError.AUTH
    }
