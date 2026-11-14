package org.evoionosp.abstrak.presentation.settings
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader

data class ConnectionSettingsUiState(
    val headers: List<ServerRequestHeader> = emptyList(),
    val bypassSsl: Boolean = false,
    val clientCertificateAlias: String? = null,
    val userAgent: String = "",
)
