package org.evoionosp.abstrak.playback
import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.abstrak.domain.session.PlayerLogoutHandler

@Singleton
class PlaybackConnectionLogoutHandler
    @Inject
    constructor(
        private val playbackConnection: PlaybackConnection,
    ) : PlayerLogoutHandler {
        override suspend fun onLogout() {
            playbackConnection.stopForLogout()
        }
    }
