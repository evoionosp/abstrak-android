package org.evoionosp.abstrak.presentation.library.components
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.presentation.R

@Composable
internal fun LibrarySyncStatus(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
) {
    Text(
        text =
            stringResource(
                when (syncStatus) {
                    SyncStatus.Idle, is SyncStatus.Success -> R.string.home_synced
                    SyncStatus.Syncing -> R.string.home_syncing
                    is SyncStatus.Stale -> R.string.home_showing_cached
                    is SyncStatus.Failed -> R.string.home_sync_failed
                },
            ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}
