package org.evoionosp.abstrak.data.network
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.evoionosp.abstrak.domain.connection.ConnectionSettingsStore

class ConnectionHeadersInterceptor(
    private val connectionSettingsStore: ConnectionSettingsStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val settings = runBlocking { connectionSettingsStore.settings.first() }
        val request =
            chain
                .request()
                .newBuilder()
                .apply {
                    settings.customHeaders.forEach { header ->
                        if (!header.name.equals(AUTHORIZATION_HEADER, ignoreCase = true)) {
                            header(header.name, header.value)
                        }
                    }
                    header(USER_AGENT_HEADER, settings.userAgent)
                }.build()
        return chain.proceed(request)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val USER_AGENT_HEADER = "User-Agent"
    }
}
