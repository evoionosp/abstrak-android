package org.evoionosp.abstrak.domain.connectivity.usecase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress
import org.evoionosp.abstrak.domain.audiobook.usecase.SchedulePendingProgressSyncUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.usecase.ObserveSessionUseCase
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.evoionosp.abstrak.domain.testing.FakePendingProgressStore
import org.evoionosp.abstrak.domain.testing.FakeProgressSyncScheduler
import org.evoionosp.abstrak.domain.testing.FakeSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FollowOfflineModeUseCaseTest {
    private val session =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )
    private val sessionStore = FakeSessionStore(session)
    private val downloads = FakeDownloadRepository()
    private val pendingProgress = FakePendingProgressStore()
    private val progressSync = FakeProgressSyncScheduler()

    @Test
    fun `starting online leaves downloads and progress delivery alone`() =
        runTest {
            follow(FakeAppSettingsStore())
            assertEquals(0, downloads.pauseCount)
            assertEquals(0, downloads.resumeCount)
            assertEquals(0, progressSync.scheduleNowCount)
        }

    @Test
    fun `starting in offline mode keeps downloads paused`() =
        runTest {
            follow(FakeAppSettingsStore(AppSettings(useOfflineMode = true)))
            assertEquals(1, downloads.pauseCount)
        }

    @Test
    fun `turning offline mode on pauses downloads, and off resumes them and delivers queued progress now`() =
        runTest {
            pendingProgress.put("lib-1", "book-1", PlaybackProgress(60.0, durationSeconds = 300.0, isFinished = false))
            val settings = FakeAppSettingsStore()
            follow(settings)
            settings.setOfflineMode(true)
            runCurrent()
            assertEquals(1, downloads.pauseCount)
            assertEquals(0, downloads.resumeCount)
            settings.setOfflineMode(false)
            runCurrent()
            assertEquals(1, downloads.resumeCount)
            assertEquals(1, progressSync.scheduleNowCount)
        }

    @Test
    fun `signing out turns offline mode off`() =
        runTest {
            val settings = FakeAppSettingsStore(AppSettings(useOfflineMode = true))
            follow(settings)
            sessionStore.clearSession()
            runCurrent()
            assertFalse(settings.state.value.useOfflineMode)
        }

    private fun TestScope.follow(settings: FakeAppSettingsStore) {
        val useCase =
            FollowOfflineModeUseCase(
                appSettingsStore = settings,
                observeSessionUseCase = ObserveSessionUseCase(sessionStore),
                downloadRepository = downloads,
                schedulePendingProgressSyncUseCase = SchedulePendingProgressSyncUseCase(pendingProgress, progressSync),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect {} }
    }
}
