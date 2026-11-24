package org.evoionosp.abstrak.presentation.library.components
import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItem
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem
import org.evoionosp.abstrak.presentation.designsystem.component.sheet.AbstrakBottomSheet

private val RowPadding = PaddingValues(vertical = 8.dp)

/** The library's sync status in full, and the switch to use the app offline. */
@Composable
internal fun LibrarySyncSheet(
    syncStatus: SyncStatus,
    isOfflineMode: Boolean,
    nowMillis: Long,
    onOfflineModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val model = librarySyncChipModel(syncStatus, nowMillis)
    val lastSyncedAtMillis = syncStatus.lastSyncedAtMillis
    AbstrakBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.library_sync_sheet_title),
    ) {
        AbstrakListItem(
            headline = model.label.text(),
            supporting =
                lastSyncedAtMillis?.let {
                    stringResource(R.string.library_sync_last_synced, formatSyncTime(it))
                },
            leading = { SyncToneIcon(tone = model.tone) },
            contentPadding = RowPadding,
        )
        Text(
            text = stringResource(R.string.library_sync_schedule),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchListItem(
            headline = stringResource(R.string.library_sync_offline_mode),
            supporting = stringResource(R.string.library_sync_offline_mode_desc),
            checked = isOfflineMode,
            onCheckedChange = onOfflineModeChange,
            contentPadding = RowPadding,
        )
    }
}

@Composable
private fun SyncToneIcon(tone: LibrarySyncTone) {
    val (containerColor, contentColor) = tone.colors()
    Surface(shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Icon(
            imageVector = tone.icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp).size(24.dp),
        )
    }
}

@Composable
private fun formatSyncTime(millis: Long): String =
    DateUtils.formatDateTime(
        LocalContext.current,
        millis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
