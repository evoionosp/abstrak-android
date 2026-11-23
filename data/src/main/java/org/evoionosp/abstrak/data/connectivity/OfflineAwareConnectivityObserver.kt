package org.evoionosp.abstrak.data.connectivity
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver

/**
 * The app's view of connectivity: the device's network, reported as gone while offline mode is
 * on. Everything that already copes with having no network (catalog and progress sync, streaming,
 * error messages) treats offline mode the same way.
 */
@Singleton
class OfflineAwareConnectivityObserver
    @Inject
    constructor(
        @param:Named(DEVICE_NETWORK) private val deviceNetwork: ConnectivityObserver,
        private val offlineMode: OfflineModeState,
    ) : ConnectivityObserver {
        override fun observe(): Flow<Boolean> =
            combine(deviceNetwork.observe(), offlineMode.isEnabled) { isConnected, isOffline ->
                isConnected && !isOffline
            }.distinctUntilChanged()

        override fun isConnected(): Boolean = !offlineMode.isEnabledNow() && deviceNetwork.isConnected()

        companion object {
            /** Qualifies the device's own network state, offline mode aside. */
            const val DEVICE_NETWORK = "device_network"
        }
    }
