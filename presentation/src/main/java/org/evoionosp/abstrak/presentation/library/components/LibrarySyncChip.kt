package org.evoionosp.abstrak.presentation.library.components
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.domain.library.sync.LibrarySyncPolicy
import org.evoionosp.abstrak.domain.library.sync.SyncFreshness
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.extendedColors

/** How the library sync chip looks: syncing now, or how fresh the last sync is. */
internal enum class LibrarySyncTone {
    SYNCING,
    RECENT,
    AGING,
    OUTDATED,
}

/** What the library sync chip says. */
internal sealed interface LibrarySyncLabel {
    data object Syncing : LibrarySyncLabel

    data object Never : LibrarySyncLabel

    data object JustNow : LibrarySyncLabel

    data class MinutesAgo(
        val minutes: Int,
    ) : LibrarySyncLabel

    data class HoursAgo(
        val hours: Int,
    ) : LibrarySyncLabel

    data class DaysAgo(
        val days: Int,
    ) : LibrarySyncLabel
}

internal data class LibrarySyncChipModel(
    val tone: LibrarySyncTone,
    val label: LibrarySyncLabel,
)

/** Syncing shows as such; otherwise the time since the last successful sync picks look and words. */
internal fun librarySyncChipModel(
    syncStatus: SyncStatus,
    nowMillis: Long,
): LibrarySyncChipModel {
    if (syncStatus is SyncStatus.Syncing) {
        return LibrarySyncChipModel(LibrarySyncTone.SYNCING, LibrarySyncLabel.Syncing)
    }
    val lastSyncedAtMillis = syncStatus.lastSyncedAtMillis
    val tone =
        when (LibrarySyncPolicy.freshness(lastSyncedAtMillis, nowMillis)) {
            SyncFreshness.RECENT -> LibrarySyncTone.RECENT
            SyncFreshness.AGING -> LibrarySyncTone.AGING
            SyncFreshness.OUTDATED -> LibrarySyncTone.OUTDATED
        }
    return LibrarySyncChipModel(tone, syncedAgoLabel(lastSyncedAtMillis, nowMillis))
}

private const val MINUTE_MILLIS = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

private fun syncedAgoLabel(
    lastSyncedAtMillis: Long?,
    nowMillis: Long,
): LibrarySyncLabel {
    if (lastSyncedAtMillis == null) return LibrarySyncLabel.Never
    val minutes = (nowMillis - lastSyncedAtMillis).coerceAtLeast(0) / MINUTE_MILLIS
    return when {
        minutes < 1 -> LibrarySyncLabel.JustNow
        minutes < MINUTES_PER_HOUR -> LibrarySyncLabel.MinutesAgo(minutes.toInt())
        minutes < MINUTES_PER_DAY -> LibrarySyncLabel.HoursAgo((minutes / MINUTES_PER_HOUR).toInt())
        else -> LibrarySyncLabel.DaysAgo((minutes / MINUTES_PER_DAY).toInt())
    }
}

/**
 * The library's sync status as a chip: green with a done cloud when it synced within the day,
 * amber with a cloud when the daily sync is overdue, red with a crossed-out cloud after two days
 * (or before any sync), and a syncing cloud on the card color while a sync runs.
 */
@Composable
internal fun LibrarySyncChip(
    syncStatus: SyncStatus,
    isOfflineMode: Boolean,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = librarySyncChipModel(syncStatus, nowMillis)
    val (containerColor, contentColor) = model.tone.colors()
    val status = model.label.text()
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = if (isOfflineMode) stringResource(R.string.library_sync_offline_label, status) else status,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = model.tone.icon,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = containerColor,
                labelColor = contentColor,
                leadingIconContentColor = contentColor,
            ),
        border = null,
    )
}

internal val LibrarySyncTone.icon: ImageVector
    get() =
        when (this) {
            LibrarySyncTone.SYNCING -> Icons.Rounded.CloudSync
            LibrarySyncTone.RECENT -> Icons.Rounded.CloudDone
            LibrarySyncTone.AGING -> Icons.Rounded.Cloud
            LibrarySyncTone.OUTDATED -> Icons.Rounded.CloudOff
        }

/** Container and content colors for the tone. */
@Composable
internal fun LibrarySyncTone.colors(): Pair<Color, Color> =
    when (this) {
        LibrarySyncTone.SYNCING -> {
            MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
        }

        LibrarySyncTone.RECENT -> {
            MaterialTheme.extendedColors.successContainer to MaterialTheme.extendedColors.onSuccessContainer
        }

        LibrarySyncTone.AGING -> {
            MaterialTheme.extendedColors.warningContainer to MaterialTheme.extendedColors.onWarningContainer
        }

        LibrarySyncTone.OUTDATED -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }
    }

@Composable
internal fun LibrarySyncLabel.text(): String =
    when (this) {
        LibrarySyncLabel.Syncing -> stringResource(R.string.library_sync_syncing)
        LibrarySyncLabel.Never -> stringResource(R.string.library_sync_never)
        LibrarySyncLabel.JustNow -> stringResource(R.string.library_sync_just_now)
        is LibrarySyncLabel.MinutesAgo -> pluralStringResource(R.plurals.library_sync_minutes_ago, minutes, minutes)
        is LibrarySyncLabel.HoursAgo -> pluralStringResource(R.plurals.library_sync_hours_ago, hours, hours)
        is LibrarySyncLabel.DaysAgo -> pluralStringResource(R.plurals.library_sync_days_ago, days, days)
    }
