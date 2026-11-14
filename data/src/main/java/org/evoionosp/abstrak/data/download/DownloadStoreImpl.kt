package org.evoionosp.abstrak.data.download
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.abstrak.data.download.file.DownloadFileStore
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.work.DownloadWorkScheduler
import org.evoionosp.abstrak.domain.session.DownloadStore

@Singleton
class DownloadStoreImpl
    @Inject
    constructor(
        private val trackDownloadDao: TrackDownloadDao,
        private val fileStore: DownloadFileStore,
        private val scheduler: DownloadWorkScheduler,
        @Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : DownloadStore {
        override suspend fun deleteAll() {
            withContext(ioDispatcher) {
                scheduler.cancelAll()
                fileStore.deleteAll()
                trackDownloadDao.deleteAll()
            }
        }
    }
