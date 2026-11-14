package org.evoionosp.abstrak.logout
import android.content.Context
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.domain.session.CoverArtCache

@Singleton
class CoilCoverArtCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @param:Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : CoverArtCache {
        override suspend fun clear() {
            withContext(ioDispatcher) {
                val imageLoader = context.imageLoader
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            }
        }
    }
