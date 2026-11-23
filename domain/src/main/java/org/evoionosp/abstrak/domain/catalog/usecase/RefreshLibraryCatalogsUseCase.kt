package org.evoionosp.abstrak.domain.catalog.usecase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository
import org.evoionosp.abstrak.domain.library.usecase.SyncLibraryBooksUseCase
import org.evoionosp.abstrak.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase

enum class CatalogRefreshStage {
    SELECTED_LIBRARY,
    OTHER_LIBRARIES,
}

class RefreshLibraryCatalogsUseCase
    @Inject
    constructor(
        private val connectivityObserver: ConnectivityObserver,
        private val observeSessionUseCase: ObserveSessionUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val libraryRepository: LibraryRepository,
        private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
    ) {
        /**
         * Once online, syncs every library whose daily sync is due, the selected one first,
         * reporting each stage that has a library to sync. Logging out stops the pass.
         */
        operator fun invoke(): Flow<CatalogRefreshStage> =
            channelFlow {
                connectivityObserver.observe().first { it }
                if (getValidSessionUseCase() == null) return@channelFlow
                val pass =
                    launch {
                        val libraries =
                            libraryRepository.observeLibraries().first().filter { syncLibraryBooksUseCase.isDue(it.id) }
                        val (selected, others) = libraries.partition { it.isSelected }
                        if (selected.isNotEmpty()) send(CatalogRefreshStage.SELECTED_LIBRARY)
                        selected.forEach { refresh(it.id) }
                        if (others.isNotEmpty()) send(CatalogRefreshStage.OTHER_LIBRARIES)
                        others.forEach { refresh(it.id) }
                    }
                val logoutWatch =
                    launch {
                        observeSessionUseCase().first { it == null }
                        pass.cancel()
                    }
                pass.join()
                logoutWatch.cancel()
            }

        private suspend fun refresh(libraryId: String) {
            val session = getValidSessionUseCase() ?: return
            try {
                syncLibraryBooksUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = libraryId,
                    force = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
