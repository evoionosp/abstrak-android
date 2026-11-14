package org.evoionosp.abstrak.data.di
import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.abstrak.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.abstrak.data.audiobook.local.dao.PendingProgressDao
import org.evoionosp.abstrak.data.connectivity.AndroidConnectivityObserver
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.library.local.dao.LibraryDao
import org.evoionosp.abstrak.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.data.library.local.db.ResettingOpenHelperFactory
import org.evoionosp.abstrak.domain.connectivity.ConnectivityObserver

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    @Provides
    @Singleton
    fun provideAbstrakDatabase(
        @ApplicationContext context: Context,
    ): AbstrakDatabase =
        Room
            .databaseBuilder(
                context,
                AbstrakDatabase::class.java,
                "abstrak.db",
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .openHelperFactory(ResettingOpenHelperFactory())
            .build()

    @Provides
    fun provideLibraryDao(database: AbstrakDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideAudiobookDao(database: AbstrakDatabase): AudiobookDao = database.audiobookDao()

    @Provides
    fun provideAudiobookDetailDao(database: AbstrakDatabase): AudiobookDetailDao = database.audiobookDetailDao()

    @Provides
    fun provideContinueListeningDao(database: AbstrakDatabase): ContinueListeningDao = database.continueListeningDao()

    @Provides
    fun providePendingProgressDao(database: AbstrakDatabase): PendingProgressDao = database.pendingProgressDao()

    @Provides
    fun provideLibrarySyncStateDao(database: AbstrakDatabase): LibrarySyncStateDao = database.librarySyncStateDao()

    @Provides
    fun provideTrackDownloadDao(database: AbstrakDatabase): TrackDownloadDao = database.trackDownloadDao()
}
