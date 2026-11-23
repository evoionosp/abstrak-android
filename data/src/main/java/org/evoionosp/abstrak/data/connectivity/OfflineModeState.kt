package org.evoionosp.abstrak.data.connectivity
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

/**
 * The offline mode setting, kept in memory for code that cannot wait on the settings store: the
 * connectivity check and every HTTP request. It starts loading as soon as anything depends on it.
 */
@Singleton
class OfflineModeState
    @Inject
    constructor(
        appSettingsStore: AppSettingsStore,
        @param:Named("io") ioDispatcher: CoroutineDispatcher,
    ) {
        private val stored: StateFlow<Boolean?> =
            appSettingsStore.settings
                .map { it.useOfflineMode }
                .stateIn(CoroutineScope(SupervisorJob() + ioDispatcher), SharingStarted.Eagerly, initialValue = null)

        /** Offline mode as stored, from the moment it has loaded. */
        val isEnabled: Flow<Boolean> = stored.filterNotNull()

        /** Offline mode now, without waiting: off until the stored setting has loaded, moments after start. */
        fun isEnabledNow(): Boolean = stored.value ?: false

        /** Offline mode now, waiting for the stored setting to load. Blocks, so background threads only. */
        fun awaitIsEnabled(): Boolean = stored.value ?: runBlocking { isEnabled.first() }
    }
