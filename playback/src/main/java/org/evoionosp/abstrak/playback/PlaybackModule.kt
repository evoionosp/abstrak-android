package org.evoionosp.abstrak.playback
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import org.evoionosp.abstrak.data.network.ManagedOkHttpClients
import org.evoionosp.abstrak.domain.session.PlayerLogoutHandler
import org.evoionosp.abstrak.domain.session.SessionStore

internal const val AUTHORIZATION_HEADER = "Authorization"

internal suspend fun SessionStore.currentAccessToken(): String = session.first()?.accessToken.orEmpty()

internal fun authorizationHeaders(accessToken: String): Map<String, String> =
    mapOf(AUTHORIZATION_HEADER to "Bearer $accessToken")

@Module
@InstallIn(ServiceComponent::class)
object PlaybackModule {
    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        sessionStore: SessionStore,
        httpClients: ManagedOkHttpClients,
    ): DataSource.Factory {
        val upstreamFactory =
            OkHttpDataSource.Factory(
                Call.Factory { request -> httpClients.apiClient.newCall(request) },
            )
        val authorizedHttp =
            ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
                val token = runBlocking { sessionStore.currentAccessToken() }
                dataSpec.withRequestHeaders(authorizationHeaders(token))
            }
        return DefaultDataSource.Factory(context, authorizedHttp)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        dataSourceFactory: DataSource.Factory,
    ): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
internal object PlaybackConnectionBindings {
    @Provides
    @Singleton
    fun provideConnectionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    fun provideControllerConnector(impl: SessionMediaControllerConnector): MediaControllerConnector = impl

    @Provides
    @Singleton
    fun providePlayerLogoutHandler(impl: PlaybackConnectionLogoutHandler): PlayerLogoutHandler = impl

    @Provides
    @Singleton
    fun providePlaybackClock(): PlaybackClock = PlaybackClock(System::currentTimeMillis)
}
