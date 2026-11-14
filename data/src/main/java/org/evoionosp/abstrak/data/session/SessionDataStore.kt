package org.evoionosp.abstrak.data.session
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.evoionosp.abstrak.data.auth.JwtExpiry
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class SessionDataStore(
    private val preferences: SharedPreferences,
) : SessionStore {
    private object Keys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val BASE_URL = "base_url"
        const val LAST_SERVER_URL = "last_server_url"
    }

    private val sessionKeys =
        listOf(
            Keys.ACCESS_TOKEN,
            Keys.REFRESH_TOKEN,
            Keys.ACCESS_TOKEN_EXPIRES_AT,
            Keys.USER_ID,
            Keys.USERNAME,
            Keys.BASE_URL,
        )
    private val sessionState by lazy { MutableStateFlow(readSession()) }
    private val lastServerUrlState by lazy { MutableStateFlow(readLastServerUrl()) }
    override val session: Flow<LoginSession?> = sessionState.asStateFlow()
    override val lastServerUrl: Flow<String?> = lastServerUrlState.asStateFlow()

    override suspend fun saveSession(session: LoginSession): LoginSession {
        val stored =
            session.copy(
                accessTokenExpiresAtEpochSeconds =
                    JwtExpiry.expiresAtEpochSeconds(
                        session.accessToken,
                    ),
            )
        preferences.edit(commit = true) {
            putString(Keys.ACCESS_TOKEN, stored.accessToken)
            putString(Keys.USERNAME, stored.username)
            putString(Keys.BASE_URL, stored.baseUrl)
            putOptional(Keys.REFRESH_TOKEN, stored.refreshToken)
            putOptional(Keys.USER_ID, stored.userId)
            putOptional(Keys.ACCESS_TOKEN_EXPIRES_AT, stored.accessTokenExpiresAtEpochSeconds)
            putOptional(Keys.LAST_SERVER_URL, stored.baseUrl)
        }
        sessionState.value = stored
        lastServerUrlState.value = stored.baseUrl.takeIf { it.isNotBlank() }
        return stored
    }

    override suspend fun clearSession() {
        preferences.edit(commit = true) {
            sessionKeys.forEach(::remove)
        }
        sessionState.value = null
    }

    private fun readSession(): LoginSession? {
        val accessToken = preferences.getString(Keys.ACCESS_TOKEN, null).orEmpty()
        val session =
            LoginSession(
                accessToken = accessToken,
                refreshToken = preferences.getString(Keys.REFRESH_TOKEN, null),
                userId = preferences.getString(Keys.USER_ID, null),
                username = preferences.getString(Keys.USERNAME, null).orEmpty(),
                baseUrl = preferences.getString(Keys.BASE_URL, null).orEmpty(),
                accessTokenExpiresAtEpochSeconds =
                    readExpiry()
                        ?: accessToken
                            .takeIf { it.isNotBlank() }
                            ?.let { JwtExpiry.expiresAtEpochSeconds(it) },
            )
        return session.takeIf { it.isValid() }
    }

    private fun readLastServerUrl(): String? =
        preferences.getString(Keys.LAST_SERVER_URL, null)
            ?: preferences.getString(Keys.BASE_URL, null)

    private fun readExpiry(): Long? =
        preferences
            .getLong(Keys.ACCESS_TOKEN_EXPIRES_AT, NO_EXPIRY)
            .takeIf { it != NO_EXPIRY }

    private fun SharedPreferences.Editor.putOptional(
        key: String,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value)
        }
    }

    private fun SharedPreferences.Editor.putOptional(
        key: String,
        value: Long?,
    ) {
        if (value == null) {
            remove(key)
        } else {
            putLong(key, value)
        }
    }

    private companion object {
        const val NO_EXPIRY = -1L
    }
}
