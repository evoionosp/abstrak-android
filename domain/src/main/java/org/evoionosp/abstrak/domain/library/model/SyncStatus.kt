package org.evoionosp.abstrak.domain.library.model

sealed interface SyncStatus {
    /** When the library's books last synced successfully, or null if they never have. */
    val lastSyncedAtMillis: Long?

    data object Idle : SyncStatus {
        override val lastSyncedAtMillis: Long? = null
    }

    data class Syncing(
        override val lastSyncedAtMillis: Long? = null,
    ) : SyncStatus

    data class Success(
        override val lastSyncedAtMillis: Long?,
    ) : SyncStatus

    data class Stale(
        override val lastSyncedAtMillis: Long?,
        val reason: CatalogError,
    ) : SyncStatus

    data class Failed(
        val reason: CatalogError,
        override val lastSyncedAtMillis: Long? = null,
    ) : SyncStatus
}
