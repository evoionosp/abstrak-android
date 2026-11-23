package org.evoionosp.abstrak.data.network
import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.evoionosp.abstrak.data.BuildConfig
import org.evoionosp.abstrak.domain.connection.ConnectionSettingsStore
import org.evoionosp.abstrak.domain.connection.model.ConnectionSettings

object OkHttpProvider {
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    fun create(
        authenticator: Authenticator?,
        connectionSettingsStore: ConnectionSettingsStore,
        context: Context,
        settings: ConnectionSettings,
        isOfflineMode: () -> Boolean,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(OfflineModeInterceptor(isOfflineMode))
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(ConnectionHeadersInterceptor(connectionSettingsStore))
                .withConnectionSecurity(
                    bypassSsl = settings.bypassSsl,
                    clientCertificateAlias = settings.clientCertificateAlias,
                    context = context,
                )
        if (authenticator != null) {
            builder.authenticator(authenticator)
        }
        if (BuildConfig.DEBUG) {
            val loggingInterceptor =
                HttpLoggingInterceptor()
                    .apply {
                        redactHeader("Authorization")
                        redactHeader("x-refresh-token")
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }
}
