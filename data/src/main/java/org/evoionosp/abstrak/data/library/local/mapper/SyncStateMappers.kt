package org.evoionosp.abstrak.data.library.local.mapper
import org.evoionosp.abstrak.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.library.model.SyncStatus

internal fun LibrarySyncStateEntity?.toDomain(): SyncStatus {
    if (this == null) {
        return SyncStatus.Idle
    }
    val error =
        error?.let {
            runCatching { CatalogError.valueOf(it) }.getOrDefault(CatalogError.UNKNOWN)
        }
    return when (status) {
        "SYNCING" -> {
            SyncStatus.Syncing(lastSyncedAtMillis = lastSyncedAtMillis)
        }

        "SUCCESS" -> {
            SyncStatus.Success(lastSyncedAtMillis = lastSyncedAtMillis)
        }

        "STALE" -> {
            SyncStatus.Stale(
                lastSyncedAtMillis = lastSyncedAtMillis,
                reason = error ?: CatalogError.UNKNOWN,
            )
        }

        "FAILED" -> {
            SyncStatus.Failed(
                reason = error ?: CatalogError.UNKNOWN,
                lastSyncedAtMillis = lastSyncedAtMillis,
            )
        }

        else -> {
            SyncStatus.Idle
        }
    }
}
