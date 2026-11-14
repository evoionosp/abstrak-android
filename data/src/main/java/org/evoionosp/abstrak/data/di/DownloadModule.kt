package org.evoionosp.abstrak.data.di
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Call
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.work.DownloadWorkScheduler
import org.evoionosp.abstrak.data.download.work.WorkManagerDownloadScheduler
import org.evoionosp.abstrak.data.network.ManagedOkHttpClients
import org.evoionosp.abstrak.data.storage.StorageMeter

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {
    @Binds
    abstract fun bindDownloadWorkScheduler(impl: WorkManagerDownloadScheduler): DownloadWorkScheduler

    companion object {
        @Provides
        @Singleton
        @Named("downloadRoot")
        fun provideDownloadRoot(
            @ApplicationContext context: Context,
        ): File = File(context.filesDir, DOWNLOADS_DIR)

        @Provides
        @Singleton
        fun provideDownloadFileStore(
            @Named("downloadRoot") root: File,
        ): DownloadFileStore = DownloadFileStore(root)

        @Provides
        @Singleton
        fun provideStorageMeter(
            @ApplicationContext context: Context,
            @Named("downloadRoot") root: File,
        ): StorageMeter = StorageMeter(cacheDir = context.cacheDir, downloadRoot = root)

        @Provides
        @Named("downloadHttpClient")
        fun provideDownloadHttpClient(clients: ManagedOkHttpClients): Call.Factory = clients.apiClient

        private const val DOWNLOADS_DIR = "downloads"
    }
}
