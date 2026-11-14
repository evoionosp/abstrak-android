package org.evoionosp.abstrak.data.network
import android.annotation.SuppressLint
import android.content.Context
import android.security.KeyChain
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient

@SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
fun OkHttpClient.Builder.withConnectionSecurity(
    bypassSsl: Boolean,
    clientCertificateAlias: String?,
    context: Context,
): OkHttpClient.Builder {
    if (!bypassSsl && clientCertificateAlias == null) {
        return this
    }
    val trustManager =
        if (bypassSsl) {
            TrustAllCertificates
        } else {
            systemTrustManager
        }
    val keyManagers =
        clientCertificateAlias?.let { buildKeyManagers(context.applicationContext, it) }
    val sslContext =
        SSLContext.getInstance("TLS").apply {
            init(keyManagers, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
    sslSocketFactory(sslContext.socketFactory, trustManager)
    if (bypassSsl) {
        hostnameVerifier { _, _ -> true }
    }
    return this
}

private val systemTrustManager: X509TrustManager by lazy {
    val keyStore = KeyStore.getInstance("AndroidCAStore")
    keyStore.load(null)
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)
    trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager
}

private object TrustAllCertificates : X509TrustManager {
    override fun checkClientTrusted(
        chain: Array<X509Certificate>,
        authType: String,
    ) {
    }

    override fun checkServerTrusted(
        chain: Array<X509Certificate>,
        authType: String,
    ) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

private fun buildKeyManagers(
    context: Context,
    alias: String,
): Array<KeyManager> =
    arrayOf(
        ClientCertificateKeyManager(
            alias = alias,
            privateKeyLoader = {
                runCatching { KeyChain.getPrivateKey(context, alias) }.getOrNull()
            },
            certificateChainLoader = {
                runCatching { KeyChain.getCertificateChain(context, alias) }.getOrNull()
            },
        ),
    )

internal class ClientCertificateKeyManager(
    private val alias: String,
    privateKeyLoader: () -> PrivateKey?,
    certificateChainLoader: () -> Array<X509Certificate>?,
) : X509ExtendedKeyManager() {
    private val privateKey: PrivateKey? by lazy { privateKeyLoader() }
    private val certificateChain: Array<X509Certificate>? by lazy { certificateChainLoader() }
    private val isReady: Boolean
        get() = privateKey != null && certificateChain != null

    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? = if (isReady) alias else null

    override fun chooseEngineClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String? = if (isReady) alias else null

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        if (alias == this.alias) certificateChain else null

    override fun getPrivateKey(alias: String?): PrivateKey? = if (alias == this.alias) privateKey else null

    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? = if (isReady) arrayOf(alias) else null

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? = null

    override fun chooseEngineServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String? = null

    override fun getServerAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? = null
}
