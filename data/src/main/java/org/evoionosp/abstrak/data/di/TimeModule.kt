package org.evoionosp.abstrak.data.di
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.abstrak.domain.time.WallClock

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideWallClock(): WallClock = WallClock.System
}
