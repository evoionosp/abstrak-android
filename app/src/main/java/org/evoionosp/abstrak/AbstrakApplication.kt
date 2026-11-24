package org.evoionosp.abstrak
import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.Call
import org.evoionosp.abstrak.appicon.AppIconCoordinator
import org.evoionosp.abstrak.catalog.CatalogSyncCoordinator
import org.evoionosp.abstrak.data.network.ManagedOkHttpClients
import org.evoionosp.abstrak.domain.audiobook.usecase.SchedulePendingProgressSyncUseCase
import org.evoionosp.abstrak.domain.connectivity.usecase.FollowOfflineModeUseCase
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference
import org.evoionosp.abstrak.presentation.designsystem.theme.applyAppNightMode
import org.evoionosp.abstrak.presentation.ui.image.newCoverArtInterceptor

@HiltAndroidApp
class AbstrakApplication :
    Application(),
    ImageLoaderFactory {
    @Inject
    lateinit var catalogSyncCoordinator: CatalogSyncCoordinator

    @Inject
    lateinit var httpClients: ManagedOkHttpClients

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var schedulePendingProgressSync: SchedulePendingProgressSyncUseCase

    @Inject
    lateinit var appSettingsStore: AppSettingsStore

    @Inject
    lateinit var followOfflineMode: FollowOfflineModeUseCase

    @Inject
    lateinit var appIconCoordinator: AppIconCoordinator

    @Inject
    @Named("application_scope")
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        followThemePreferenceWithNightMode()
        catalogSyncCoordinator.start()
        appIconCoordinator.start()
        applicationScope.launch {
            downloadRepository.reconcileStorage()
        }
        applicationScope.launch {
            schedulePendingProgressSync()
        }
        applicationScope.launch {
            followOfflineMode().collect()
        }
    }

    private companion object {
        const val TAG = "AbstrakApplication"
    }

    /**
     * Keeps the system's per-app night mode in step with the theme preference, so the splash
     * screen, drawn before any app code runs, matches the theme the app then shows. The system
     * persists that mode, so startup never waits on it: the first value only catches up a
     * preference stored before this existed (a no-op when they already match), and later values
     * follow changes made in Settings.
     */
    private fun followThemePreferenceWithNightMode() {
        appSettingsStore.settings
            .map { ThemePreference.fromName(it.themePreference) }
            .distinctUntilChanged()
            .onEach { applyAppNightMode(this, it) }
            .catch { Log.w(TAG, "Could not follow the theme preference with the night mode", it) }
            .launchIn(applicationScope)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(newCoverArtInterceptor()) }
            .callFactory(Call.Factory { request -> httpClients.apiClient.newCall(request) })
            .build()
}
