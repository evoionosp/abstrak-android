package org.evoionosp.abstrak.domain.catalog.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver

/**
 * Background catalog sync for as long as it is collected: each time the device comes online, app
 * start included, the daily library sync runs if it is due and Continue Listening refreshes. A
 * library switch syncs from where it is made, and a pull to refresh forces a sync.
 */
class ObserveAndSyncCatalogUseCase
    @Inject
    constructor(
        private val connectivityObserver: ConnectivityObserver,
        private val refreshHomeCatalogUseCase: RefreshHomeCatalogUseCase,
    ) {
        operator fun invoke(): Flow<Unit> =
            connectivityObserver
                .observe()
                .distinctUntilChanged()
                .filter { isConnected -> isConnected }
                .map { refreshHomeCatalogUseCase(force = false) }
                .map {}
    }
