package org.evoionosp.abstrak.domain.session

fun interface PlayerLogoutHandler {
    suspend fun onLogout()
}
