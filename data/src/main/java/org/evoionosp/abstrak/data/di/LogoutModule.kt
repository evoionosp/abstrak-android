package org.evoionosp.abstrak.data.di
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.data.download.DownloadStoreImpl
import org.evoionosp.abstrak.data.session.LocalCatalogCleanerImpl
import org.evoionosp.abstrak.domain.session.DownloadStore
import org.evoionosp.abstrak.domain.session.LocalCatalogCleaner

@Module
@InstallIn(SingletonComponent::class)
abstract class LogoutModule {
    @Binds
    abstract fun bindLocalCatalogCleaner(impl: LocalCatalogCleanerImpl): LocalCatalogCleaner

    @Binds
    abstract fun bindDownloadStore(impl: DownloadStoreImpl): DownloadStore
}
