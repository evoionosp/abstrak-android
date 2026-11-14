package org.evoionosp.abstrak.data.download.work
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class DownloadBookLocks
    @Inject
    constructor() {
        private val locks = ConcurrentHashMap<String, Mutex>()

        fun lockFor(audiobookId: String): Mutex = locks.computeIfAbsent(audiobookId) { Mutex() }
    }
