package org.evoionosp.abstrak.data.network
import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.evoionosp.abstrak.data.connectivity.OfflineModeState
import org.evoionosp.abstrak.domain.connection.ConnectionSettingsStore
import org.evoionosp.abstrak.domain.connection.model.ConnectionSettings

@Singleton
class ManagedOkHttpClients
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val connectionSettingsStore: ConnectionSettingsStore,
        private val tokenAuthenticator: Lazy<TokenAuthenticator>,
        private val offlineModeState: OfflineModeState,
        @param:Named("io") ioDispatcher: CoroutineDispatcher,
    ) {
        private data class ClientPair(
            val api: OkHttpClient,
            val auth: OkHttpClient,
        )

        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        @Volatile
        private var current: ClientPair? = null
        private val invalidators = CopyOnWriteArrayList<() -> Unit>()

        init {
            scope.launch {
                connectionSettingsStore.settings
                    .distinctUntilChangedBy { it.bypassSsl to it.clientCertificateAlias }
                    .collect { rebuild(it) }
            }
        }

        val apiClient: OkHttpClient
            get() = ensureCurrent().api
        val authClient: OkHttpClient
            get() = ensureCurrent().auth

        fun registerInvalidator(invalidator: () -> Unit) {
            invalidators.add(invalidator)
        }

        @Synchronized
        private fun ensureCurrent(): ClientPair =
            current ?: rebuild(runBlocking { connectionSettingsStore.settings.first() })

        @Synchronized
        private fun rebuild(settings: ConnectionSettings): ClientPair {
            val pair =
                ClientPair(
                    api =
                        OkHttpProvider.create(
                            authenticator = tokenAuthenticator.get(),
                            connectionSettingsStore = connectionSettingsStore,
                            context = context,
                            settings = settings,
                            isOfflineMode = offlineModeState::awaitIsEnabled,
                        ),
                    auth =
                        OkHttpProvider.create(
                            authenticator = null,
                            connectionSettingsStore = connectionSettingsStore,
                            context = context,
                            settings = settings,
                            isOfflineMode = offlineModeState::awaitIsEnabled,
                        ),
                )
            current = pair
            invalidators.forEach { it() }
            return pair
        }
    }
