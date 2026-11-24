package org.evoionosp.abstrak.domain.library.sync

/** How fresh a library's books are, by the time since they last synced. */
enum class SyncFreshness {
    /** Synced within the last [LibrarySyncPolicy.INTERVAL_MILLIS]. */
    RECENT,

    /** The daily sync is overdue, by up to a day. */
    AGING,

    /** Not synced for more than [LibrarySyncPolicy.OUTDATED_AFTER_MILLIS], or never. */
    OUTDATED,
}

/**
 * When the library syncs on its own. A library's books (and the library list with them) sync at
 * most once per [INTERVAL_MILLIS] by themselves (app start, return to the app, network back, library
 * switch, search); a pull to refresh syncs them at any time.
 */
object LibrarySyncPolicy {
    const val INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000

    const val OUTDATED_AFTER_MILLIS: Long = 2 * INTERVAL_MILLIS

    /**
     * Whether an automatic sync should run: the library never synced, last synced at least
     * [INTERVAL_MILLIS] ago, or the clock has moved back before its last sync.
     */
    fun isDue(
        lastSyncedAtMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (lastSyncedAtMillis == null) return true
        val elapsed = nowMillis - lastSyncedAtMillis
        return elapsed < 0 || elapsed >= INTERVAL_MILLIS
    }

    fun freshness(
        lastSyncedAtMillis: Long?,
        nowMillis: Long,
    ): SyncFreshness {
        if (lastSyncedAtMillis == null) return SyncFreshness.OUTDATED
        val elapsed = (nowMillis - lastSyncedAtMillis).coerceAtLeast(0)
        return when {
            elapsed < INTERVAL_MILLIS -> SyncFreshness.RECENT
            elapsed <= OUTDATED_AFTER_MILLIS -> SyncFreshness.AGING
            else -> SyncFreshness.OUTDATED
        }
    }
}
