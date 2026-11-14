package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import org.evoionosp.abstrak.domain.session.CoverArtCache
import org.evoionosp.abstrak.domain.session.DownloadStore
import org.evoionosp.abstrak.domain.session.LocalCatalogCleaner
import org.evoionosp.abstrak.domain.session.LogoutResult
import org.evoionosp.abstrak.domain.session.LogoutStep
import org.evoionosp.abstrak.domain.session.PlayerLogoutHandler
import org.evoionosp.abstrak.domain.session.SessionStore

class LogoutUserUseCase
    @Inject
    constructor(
        private val playerLogoutHandler: PlayerLogoutHandler,
        private val sessionStore: SessionStore,
        private val downloadStore: DownloadStore,
        private val coverArtCache: CoverArtCache,
        private val localCatalogCleaner: LocalCatalogCleaner,
    ) {
        suspend operator fun invoke(): LogoutResult {
            val failedSteps = mutableListOf<LogoutStep>()
            runStep(LogoutStep.STOP_PLAYBACK, failedSteps) { playerLogoutHandler.onLogout() }
            runStep(LogoutStep.CLEAR_SESSION, failedSteps) { sessionStore.clearSession() }
            runStep(LogoutStep.DELETE_DOWNLOADS, failedSteps) { downloadStore.deleteAll() }
            runStep(LogoutStep.CLEAR_COVER_ART_CACHE, failedSteps) { coverArtCache.clear() }
            runStep(LogoutStep.CLEAR_CATALOG_CACHE, failedSteps) { localCatalogCleaner.clear() }
            return LogoutResult(failedSteps)
        }

        private suspend fun runStep(
            step: LogoutStep,
            failedSteps: MutableList<LogoutStep>,
            action: suspend () -> Unit,
        ) {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failedSteps += step
            }
        }
    }
