package org.evoionosp.abstrak.domain.library.usecase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.model.DomainResult
import org.evoionosp.abstrak.domain.library.sync.LibrarySyncPolicy
import org.evoionosp.abstrak.domain.time.WallClock

/**
 * The one way a library's books sync from the server. Syncs run one at a time and check whether
 * they are due only once their turn comes, so triggers firing together sync a library once.
 */
@Singleton
class SyncLibraryBooksUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val clock: WallClock,
    ) {
        private val mutex = Mutex()

        /**
         * Refreshes [libraryId]'s books when a sync is due by [LibrarySyncPolicy], or always when
         * [force]d. Returns null when it was not due.
         */
        suspend operator fun invoke(
            baseUrl: String,
            accessToken: String,
            libraryId: String,
            force: Boolean,
        ): DomainResult<Unit>? =
            mutex.withLock {
                if (!force && !isDue(libraryId)) return@withLock null
                audiobookRepository.refreshAudiobooks(baseUrl, accessToken, libraryId)
            }

        suspend fun isDue(libraryId: String): Boolean {
            val status = audiobookRepository.observeLibrarySyncStatus(libraryId).first()
            return LibrarySyncPolicy.isDue(status.lastSyncedAtMillis, clock.nowMillis())
        }
    }
