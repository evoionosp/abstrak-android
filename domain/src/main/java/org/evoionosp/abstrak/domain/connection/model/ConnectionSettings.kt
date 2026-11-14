package org.evoionosp.abstrak.domain.connection.model

data class ConnectionSettings(
    val customHeaders: List<ServerRequestHeader> = emptyList(),
    val bypassSsl: Boolean = false,
    val clientCertificateAlias: String? = null,
    val userAgent: String,
)
