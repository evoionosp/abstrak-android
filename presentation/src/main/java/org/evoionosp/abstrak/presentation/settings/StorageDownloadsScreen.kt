package org.evoionosp.abstrak.presentation.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItem
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItemDefaults
import org.evoionosp.abstrak.presentation.designsystem.component.list.GroupDivider
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListGroup
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem

@Composable
fun StorageDownloadsScreen(
    state: StorageDownloadsUiState,
    onBackClick: () -> Unit,
    onDownloadWifiOnlyChange: (Boolean) -> Unit,
    onAutoDeleteFinishedDownloadsChange: (Boolean) -> Unit,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    SettingsScaffold(
        title = stringResource(R.string.preferences_storage_downloads),
        onBack = onBackClick,
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    ) {
        if (state.isLoaded) {
            item {
                ListGroup(title = stringResource(R.string.storage_downloads_group)) {
                    SwitchListItem(
                        headline = stringResource(R.string.storage_wifi_only),
                        supporting = stringResource(R.string.storage_wifi_only_desc),
                        checked = state.downloadWifiOnly,
                        onCheckedChange = onDownloadWifiOnlyChange,
                    )
                    GroupDivider()
                    SwitchListItem(
                        headline = stringResource(R.string.storage_auto_delete),
                        supporting = stringResource(R.string.storage_auto_delete_desc),
                        checked = state.autoDeleteFinishedDownloads,
                        onCheckedChange = onAutoDeleteFinishedDownloadsChange,
                    )
                }
            }
            item {
                ListGroup(title = stringResource(R.string.storage_manage_group)) {
                    StorageSpaceSummary(storage = state.storage)
                    GroupDivider()
                    ClearAllDownloadsItem(
                        enabled = state.hasDownloads && !state.isClearing,
                        onClick = onClearAllClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearAllDownloadsItem(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val disabledColor = MaterialTheme.colorScheme.onSurface
    AbstrakListItem(
        headline = stringResource(R.string.storage_clear_all),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        trailing = {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(
                            color =
                                if (enabled) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    disabledColor.copy(alpha = 0.12f)
                                },
                            shape = MaterialTheme.shapes.large,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint =
                        if (enabled) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            disabledColor.copy(alpha = 0.38f)
                        },
                )
            }
        },
        colors =
            AbstrakListItemDefaults.colors(
                headlineColor = if (enabled) Color.Unspecified else disabledColor.copy(alpha = 0.38f),
            ),
    )
}
