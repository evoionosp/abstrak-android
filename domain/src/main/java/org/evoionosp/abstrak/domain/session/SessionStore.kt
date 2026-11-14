package org.evoionosp.abstrak.domain.session
import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val session: Flow<LoginSession?>
    val lastServerUrl: Flow<String?>

    suspend fun saveSession(session: LoginSession): LoginSession

    suspend fun clearSession()
}
