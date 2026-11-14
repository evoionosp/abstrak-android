package org.evoionosp.abstrak.domain.testing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.auth.repository.AuthRepository
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class FakeSessionStore(
    initialSession: LoginSession?,
) : SessionStore {
    private val backing = MutableStateFlow(initialSession)
    private val lastServer = MutableStateFlow(initialSession?.baseUrl)
    override val session: Flow<LoginSession?> = backing
    override val lastServerUrl: Flow<String?> = lastServer

    override suspend fun saveSession(session: LoginSession): LoginSession {
        backing.value = session
        lastServer.value = session.baseUrl
        return session
    }

    override suspend fun clearSession() {
        backing.value = null
    }
}

class FakeAuthRepository(
    private val refreshResult: LoginResult,
) : AuthRepository {
    var refreshCallCount = 0
        private set

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): LoginResult = refreshResult

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String,
    ): LoginResult {
        refreshCallCount++
        return refreshResult
    }
}
