package org.evoionosp.abstrak.data.network
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/** What every request fails with while offline mode is on: an [IOException], so it is handled as no network. */
class OfflineModeException : IOException("Offline mode is on")

/**
 * Fails every request while offline mode is on, before it reaches the network, so nothing is sent
 * to the server, even by callers that do not check connectivity first (cover art, streaming,
 * downloads, token refresh).
 */
class OfflineModeInterceptor(
    private val isOfflineMode: () -> Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (isOfflineMode()) throw OfflineModeException()
        return chain.proceed(chain.request())
    }
}
