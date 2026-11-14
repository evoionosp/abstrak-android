package org.evoionosp.abstrak.domain.auth.repository
import org.evoionosp.abstrak.domain.auth.model.LoginResult

interface AuthRepository {
    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): LoginResult

    suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String,
    ): LoginResult
}
