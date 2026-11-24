package org.evoionosp.abstrak.domain.catalog.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.SyncLibraryBooksUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase

sealed class RefreshHomeCatalogResult {
    data object Success : RefreshHomeCatalogResult()

    data object SessionExpired : RefreshHomeCatalogResult()

    data class Failure(
        val error: CatalogError,
    ) : RefreshHomeCatalogResult()
}

class RefreshHomeCatalogUseCase
    @Inject
    constructor(
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
        private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
        private val observeLibrariesUseCase: ObserveLibrariesUseCase,
        private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
        private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
    ) {
        /**
         * Syncs the library list and the selected library's books when the daily library sync is
         * due, or every time when [force]d (pull to refresh). Continue Listening refreshes either
         * way: it follows listening on other devices, so it does not wait for the daily sync.
         */
        suspend operator fun invoke(force: Boolean): RefreshHomeCatalogResult {
            val session = getValidSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired
            val libraryRefreshResult =
                if (force || isLibrarySyncDue()) refreshLibraries(session) else DomainResult.Success(Unit)
            if (libraryRefreshResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired
            val selectedLibraryId =
                selectedLibraryId() ?: return resultFor(libraryRefreshResult, CatalogError.NO_AUDIOBOOK_LIBRARIES)
            val audiobookRefreshResult =
                syncLibraryBooksUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = selectedLibraryId,
                    force = force,
                ) ?: DomainResult.Success(Unit)
            if (audiobookRefreshResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired
            val continueListeningResult = refreshContinueListening(session, selectedLibraryId)
            if (continueListeningResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired
            return when {
                libraryRefreshResult is DomainResult.Failure -> {
                    RefreshHomeCatalogResult.Failure(libraryRefreshResult.error)
                }

                audiobookRefreshResult is DomainResult.Failure -> {
                    RefreshHomeCatalogResult.Failure(audiobookRefreshResult.error)
                }

                else -> {
                    RefreshHomeCatalogResult.Success
                }
            }
        }

        private suspend fun selectedLibraryId(): String? =
            observeSelectedLibraryUseCase().first()?.id
                ?: observeLibrariesUseCase().first().firstOrNull()?.id

        /** Due when the library to sync is: with no library on the device yet, always. */
        private suspend fun isLibrarySyncDue(): Boolean {
            val libraryId = selectedLibraryId() ?: return true
            return syncLibraryBooksUseCase.isDue(libraryId)
        }

        private fun resultFor(
            libraryRefreshResult: DomainResult<Unit>,
            fallbackError: CatalogError,
        ): RefreshHomeCatalogResult =
            if (libraryRefreshResult is DomainResult.Failure) {
                RefreshHomeCatalogResult.Failure(libraryRefreshResult.error)
            } else {
                RefreshHomeCatalogResult.Failure(fallbackError)
            }

        private suspend fun refreshLibraries(session: LoginSession): DomainResult<Unit> =
            refreshLibrariesUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
            )

        private suspend fun refreshContinueListening(
            session: LoginSession,
            libraryId: String,
        ): DomainResult<Unit> =
            refreshContinueListeningUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = libraryId,
            )

        private fun DomainResult<Unit>.isAuthFailure(): Boolean =
            this is DomainResult.Failure && error == CatalogError.AUTH
    }
