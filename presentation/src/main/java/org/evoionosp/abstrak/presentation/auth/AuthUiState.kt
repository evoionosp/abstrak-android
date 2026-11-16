package org.evoionosp.abstrak.presentation.auth
import org.evoionosp.abstrak.domain.server.model.ServerStatus

data class AuthUiState(
    val protocol: String = "https://",
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isChecking: Boolean = false,
    val isLoggingIn: Boolean = false,
    val showLoginFields: Boolean = false,
    val serverStatus: ServerStatus? = null,
    /** Whether [protocol] + [baseUrl] is a server URL the app can connect to. */
    val isBaseUrlValid: Boolean = false,
) {
    /** The Connect button is usable only for a well-formed server URL, and never while checking. */
    val isConnectEnabled: Boolean
        get() = !isChecking && isBaseUrlValid

    /** Explains a disabled Connect under the field, once something has been typed. */
    val showInvalidBaseUrl: Boolean
        get() = baseUrl.isNotBlank() && !isBaseUrlValid
}
