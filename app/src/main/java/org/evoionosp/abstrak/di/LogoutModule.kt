package org.evoionosp.abstrak.di
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.domain.session.CoverArtCache
import org.evoionosp.abstrak.logout.CoilCoverArtCache

@Module
@InstallIn(SingletonComponent::class)
abstract class LogoutModule {
    @Binds
    abstract fun bindCoverArtCache(impl: CoilCoverArtCache): CoverArtCache
}
