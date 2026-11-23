package org.evoionosp.abstrak.domain.connectivity.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.withIndex
import org.evoionosp.abstrak.domain.audiobook.usecase.SchedulePendingProgressSyncUseCase
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

/**
 * Carries out what offline mode means beyond requests failing, for as long as it is collected.
 * Turning it on pauses downloads, which would otherwise spend their retries on requests that
 * cannot go out; turning it off resumes them and delivers queued progress straight away (the
 * daily library sync follows from connectivity coming back). Signing out turns offline mode off,
 * since signing back in needs the server.
 */
class FollowOfflineModeUseCase
    @Inject
    constructor(
        private val appSettingsStore: AppSettingsStore,
        private val observeSessionUseCase: ObserveSessionUseCase,
        private val downloadRepository: DownloadRepository,
        private val schedulePendingProgressSyncUseCase: SchedulePendingProgressSyncUseCase,
    ) {
        operator fun invoke(): Flow<Unit> = merge(followOfflineMode(), endOfflineModeOnSignOut())

        private fun followOfflineMode(): Flow<Unit> =
            appSettingsStore.settings
                .map { it.useOfflineMode }
                .distinctUntilChanged()
                .withIndex()
                .map { (index, isOffline) ->
                    if (isOffline) {
                        downloadRepository.pauseDownloads()
                    } else if (index > 0) {
                        downloadRepository.resumeDownloads()
                        schedulePendingProgressSyncUseCase(immediately = true)
                    }
                }

        private fun endOfflineModeOnSignOut(): Flow<Unit> =
            observeSessionUseCase()
                .filter { it == null }
                .map {
                    if (appSettingsStore.settings.first().useOfflineMode) {
                        appSettingsStore.setOfflineMode(false)
                    }
                }
    }
