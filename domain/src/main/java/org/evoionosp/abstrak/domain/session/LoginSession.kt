package org.evoionosp.abstrak.domain.session

data class LoginSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String?,
    val username: String,
    val baseUrl: String,
    val accessTokenExpiresAtEpochSeconds: Long? = null,
) {
    fun isValid(): Boolean = accessToken.isNotBlank() && username.isNotBlank() && baseUrl.isNotBlank()

    fun isAccessTokenExpired(
        nowEpochSeconds: Long,
        skewSeconds: Long = DEFAULT_EXPIRY_SKEW_SECONDS,
    ): Boolean {
        val expiresAt = accessTokenExpiresAtEpochSeconds ?: return false
        return nowEpochSeconds >= expiresAt - skewSeconds
    }

    companion object {
        const val DEFAULT_EXPIRY_SKEW_SECONDS = 60L
    }
}
