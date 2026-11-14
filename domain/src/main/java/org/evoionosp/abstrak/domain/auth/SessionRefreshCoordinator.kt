package org.evoionosp.abstrak.domain.auth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.evoionosp.abstrak.domain.auth.model.AuthError
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.auth.repository.AuthRepository
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

sealed interface TokenRefreshResult {
    data class Refreshed(
        val session: LoginSession,
    ) : TokenRefreshResult

    data object LoggedOut : TokenRefreshResult

    data object Retryable : TokenRefreshResult
}

@Singleton
class SessionRefreshCoordinator
    @Inject
    constructor(
        private val sessionStore: SessionStore,
        private val authRepository: AuthRepository,
    ) {
        private val mutex = Mutex()

        suspend fun refresh(staleAccessToken: String? = null): TokenRefreshResult =
            mutex.withLock {
                val current =
                    sessionStore.session.first()
                        ?: return@withLock TokenRefreshResult.LoggedOut
                if (staleAccessToken != null && current.accessToken != staleAccessToken) {
                    return@withLock TokenRefreshResult.Refreshed(current)
                }
                val refreshToken =
                    current.refreshToken?.takeIf { it.isNotBlank() }
                        ?: return@withLock logOut()
                when (val result = authRepository.refreshSession(current.baseUrl, refreshToken)) {
                    is LoginResult.Success -> {
                        persistRotatedSession(current, result)
                    }

                    is LoginResult.Failure -> {
                        if (isUnrecoverable(result)) {
                            logOut()
                        } else {
                            TokenRefreshResult.Retryable
                        }
                    }
                }
            }

        private fun isUnrecoverable(failure: LoginResult.Failure): Boolean {
            if (failure.error == AuthError.INVALID_BASE_URL) return true
            if (failure.error != AuthError.HTTP) return false
            val code = failure.code ?: return false
            return UNRECOVERABLE_HTTP_CODES.contains(code)
        }

        private suspend fun persistRotatedSession(
            current: LoginSession,
            result: LoginResult.Success,
        ): TokenRefreshResult {
            val accessToken =
                result.data.accessToken
                    ?.trim()
                    .orEmpty()
            if (accessToken.isBlank()) return logOut()
            val rotated =
                current.copy(
                    accessToken = accessToken,
                    refreshToken =
                        result.data.refreshToken
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.refreshToken,
                    userId = result.data.userId?.trim() ?: current.userId,
                    accessTokenExpiresAtEpochSeconds = null,
                )
            return TokenRefreshResult.Refreshed(sessionStore.saveSession(rotated))
        }

        private suspend fun logOut(): TokenRefreshResult {
            sessionStore.clearSession()
            return TokenRefreshResult.LoggedOut
        }

        private companion object {
            val UNRECOVERABLE_HTTP_CODES = setOf(400, 401, 403, 404)
        }
    }
