package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.auth.SessionRefreshCoordinator
import org.evoionosp.abstrak.domain.auth.TokenRefreshResult
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class GetValidSessionUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
        private val sessionRefreshCoordinator: SessionRefreshCoordinator,
    ) {
        suspend operator fun invoke(): LoginSession? {
            val session = sessionStore.session.first() ?: return null
            if (!session.isAccessTokenExpired(nowEpochSeconds())) return session
            return when (
                val result =
                    sessionRefreshCoordinator.refresh(
                        staleAccessToken = session.accessToken,
                    )
            ) {
                is TokenRefreshResult.Refreshed -> result.session
                TokenRefreshResult.LoggedOut -> null
                TokenRefreshResult.Retryable -> session
            }
        }

        private fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000
    }
