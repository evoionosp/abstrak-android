package org.evoionosp.abstrak.data.network
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.TokenRefreshResult

@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val sessionRefreshCoordinator: SessionRefreshCoordinator,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (priorResponseCount(response) >= MAX_ATTEMPTS) return null
            val staleToken =
                response.request
                    .header(AUTHORIZATION_HEADER)
                    ?.removePrefix(BEARER_PREFIX)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            val refreshed = runBlocking { sessionRefreshCoordinator.refresh(staleToken) }
            val newToken =
                when (refreshed) {
                    is TokenRefreshResult.Refreshed -> refreshed.session.accessToken
                    TokenRefreshResult.LoggedOut, TokenRefreshResult.Retryable -> return null
                }
            if (newToken.isBlank() || newToken == staleToken) return null
            return response.request
                .newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newToken")
                .build()
        }

        private fun priorResponseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            const val AUTHORIZATION_HEADER = "Authorization"
            const val BEARER_PREFIX = "Bearer "
            const val MAX_ATTEMPTS = 2
        }
    }
