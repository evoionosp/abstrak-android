package org.evoionosp.abstrak.domain.catalog.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.RefreshLibrariesUseCase
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
        private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
        private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
    ) {
        suspend operator fun invoke(): RefreshHomeCatalogResult {
            val session = getValidSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired
            val libraryRefreshResult = refreshLibraries(session)
            if (libraryRefreshResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired
            val selectedLibraryId =
                observeSelectedLibraryUseCase().first()?.id
                    ?: observeLibrariesUseCase().first().firstOrNull()?.id
                    ?: return resultFor(libraryRefreshResult, CatalogError.NO_AUDIOBOOK_LIBRARIES)
            val audiobookRefreshResult =
                refreshSelectedLibraryAudiobooks(session, selectedLibraryId)
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

        private suspend fun refreshSelectedLibraryAudiobooks(
            session: LoginSession,
            libraryId: String,
        ): DomainResult<Unit> =
            refreshSelectedLibraryAudiobooksUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = libraryId,
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
