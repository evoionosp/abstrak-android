package org.evoionosp.abstrak.data.session
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase
import org.evoionosp.abstrak.domain.session.LocalCatalogCleaner

@Singleton
class LocalCatalogCleanerImpl
    @Inject
    constructor(
        private val database: AbstrakDatabase,
        @param:Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : LocalCatalogCleaner {
        override suspend fun clear() {
            withContext(ioDispatcher) {
                database.clearAllTables()
            }
        }
    }
