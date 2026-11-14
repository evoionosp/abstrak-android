package org.evoionosp.abstrak.domain.connection
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.connection.model.ConnectionSettings
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader

interface ConnectionSettingsStore {
    val settings: Flow<ConnectionSettings>

    suspend fun setCustomHeaders(headers: List<ServerRequestHeader>)

    suspend fun setBypassSsl(enabled: Boolean)

    suspend fun setClientCertificateAlias(alias: String?)

    suspend fun setUserAgent(userAgent: String)

    suspend fun resetUserAgent()
}
