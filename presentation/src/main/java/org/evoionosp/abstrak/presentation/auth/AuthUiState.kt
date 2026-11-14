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
)
