package org.evoionosp.abstrak.data.di
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.data.audiobook.repository.AudiobookRepositoryImpl
import org.evoionosp.abstrak.data.auth.repository.AuthRepositoryImpl
import org.evoionosp.abstrak.data.download.repository.DownloadRepositoryImpl
import org.evoionosp.abstrak.data.library.repository.LibraryRepositoryImpl
import org.evoionosp.abstrak.data.network.HttpServerUrlValidator
import org.evoionosp.abstrak.data.server.repository.ServerRepositoryImpl
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.auth.repository.AuthRepository
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository
import org.evoionosp.abstrak.domain.server.ServerUrlValidator
import org.evoionosp.abstrak.domain.server.repository.ServerRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    abstract fun bindServerUrlValidator(impl: HttpServerUrlValidator): ServerUrlValidator

    @Binds
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    abstract fun bindAudiobookRepository(impl: AudiobookRepositoryImpl): AudiobookRepository

    @Binds
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
}
