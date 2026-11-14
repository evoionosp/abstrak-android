package org.evoionosp.abstrak
import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Call
import org.evoionosp.abstrak.catalog.CatalogSyncCoordinator
import org.evoionosp.abstrak.data.network.ManagedOkHttpClients
import org.evoionosp.abstrak.domain.audiobook.usecase.SchedulePendingProgressSyncUseCase
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
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
    @Named("application_scope")
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        catalogSyncCoordinator.start()
        applicationScope.launch {
            downloadRepository.reconcileStorage()
        }
        applicationScope.launch {
            schedulePendingProgressSync()
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(newCoverArtInterceptor()) }
            .callFactory(Call.Factory { request -> httpClients.apiClient.newCall(request) })
            .build()
}
