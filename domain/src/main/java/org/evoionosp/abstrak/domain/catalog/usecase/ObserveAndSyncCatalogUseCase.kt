package org.evoionosp.abstrak.domain.catalog.usecase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.abstrak.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase

class ObserveAndSyncCatalogUseCase
    @Inject
    constructor(
        private val connectivityObserver: ConnectivityObserver,
        private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
        private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
        private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<Unit> {
            val connectivitySync =
                connectivityObserver
                    .observe()
                    .distinctUntilChanged()
                    .flatMapLatest { isConnected ->
                        if (isConnected) {
                            flowOf(Unit)
                        } else {
                            flowOf()
                        }
                    }.map {
                        syncCurrentSelection()
                    }
            val libraryChangeSync =
                observeSelectedLibraryUseCase()
                    .filterNotNull()
                    .flatMapLatest { library ->
                        if (connectivityObserver.isConnected()) {
                            flowOf(library)
                        } else {
                            flowOf()
                        }
                    }.map { library ->
                        val session = getValidSessionUseCase() ?: return@map
                        refreshSelectedLibraryAudiobooksUseCase(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = library.id,
                        )
                        refreshContinueListeningUseCase(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = library.id,
                        )
                    }
            return combine(connectivitySync, libraryChangeSync) { _, _ -> }
        }

        private suspend fun syncCurrentSelection() {
            val session = getValidSessionUseCase() ?: return
            refreshLibrariesUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
            )
            val selectedLibrary = observeSelectedLibraryUseCase().first()
            if (selectedLibrary != null) {
                refreshSelectedLibraryAudiobooksUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = selectedLibrary.id,
                )
                refreshContinueListeningUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = selectedLibrary.id,
                )
            }
        }
    }
