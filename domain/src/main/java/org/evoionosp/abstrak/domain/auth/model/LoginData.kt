package org.evoionosp.abstrak.domain.auth.model

data class LoginData(
    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
)
