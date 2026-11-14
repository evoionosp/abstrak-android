package org.evoionosp.abstrak.presentation.settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.dialog.ConfirmDialog
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItemDefaults
import org.evoionosp.abstrak.presentation.designsystem.component.list.GroupDivider
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListGroup
import org.evoionosp.abstrak.presentation.designsystem.component.list.NavigationListItem
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem

@Composable
fun PreferencesRoute(
    onBackClick: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenServerConnection: () -> Unit,
    onOpenStorageDownloads: () -> Unit,
    onLoggedOut: () -> Unit,
    showLogout: Boolean,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    viewModel: PreferencesViewModel = hiltViewModel(),
) {
    val isLoggingOut by viewModel.isLoggingOut.collectAsStateWithLifecycle()
    val searchAllLibraries by viewModel.searchAllLibraries.collectAsStateWithLifecycle()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    PreferencesScreen(
        showLogout = showLogout,
        searchAllLibraries = searchAllLibraries,
        onBackClick = onBackClick,
        onOpenAppearance = onOpenAppearance,
        onOpenServerConnection = onOpenServerConnection,
        onOpenStorageDownloads = onOpenStorageDownloads,
        onSearchAllLibrariesChange = viewModel::onSearchAllLibrariesChange,
        onLogoutClick = { showLogoutDialog = true },
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    )
    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.logout_confirm_title),
            message = stringResource(R.string.logout_confirm_message),
            confirmLabel = stringResource(R.string.preferences_logout),
            onConfirm = {
                viewModel.logout {
                    showLogoutDialog = false
                    onLoggedOut()
                }
            },
            onDismiss = { showLogoutDialog = false },
            destructive = true,
            inProgress = isLoggingOut,
        )
    }
}

@Composable
fun PreferencesScreen(
    showLogout: Boolean,
    searchAllLibraries: Boolean?,
    onBackClick: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenServerConnection: () -> Unit,
    onOpenStorageDownloads: () -> Unit,
    onSearchAllLibrariesChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    SettingsScaffold(
        title = stringResource(R.string.preferences_title),
        onBack = onBackClick,
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    ) {
        item {
            ListGroup(title = null) {
                NavigationListItem(
                    headline = stringResource(R.string.preferences_appearance),
                    supporting = stringResource(R.string.preferences_appearance_summary),
                    onClick = onOpenAppearance,
                    leading = { PreferenceIcon(Icons.Rounded.Palette) },
                )
                GroupDivider()
                NavigationListItem(
                    headline = stringResource(R.string.preferences_server_connection),
                    supporting = stringResource(R.string.preferences_server_connection_summary),
                    onClick = onOpenServerConnection,
                    leading = { PreferenceIcon(Icons.Rounded.Dns) },
                )
                GroupDivider()
                NavigationListItem(
                    headline = stringResource(R.string.preferences_storage_downloads),
                    supporting = stringResource(R.string.preferences_storage_downloads_summary),
                    onClick = onOpenStorageDownloads,
                    leading = { PreferenceIcon(Icons.Rounded.Download) },
                )
            }
        }
        searchAllLibraries?.let { allLibraries ->
            item {
                ListGroup(title = stringResource(R.string.preferences_search)) {
                    SwitchListItem(
                        headline = stringResource(R.string.preferences_search_all_libraries),
                        supporting = stringResource(R.string.preferences_search_all_libraries_desc),
                        checked = allLibraries,
                        onCheckedChange = onSearchAllLibrariesChange,
                    )
                }
            }
        }
        if (showLogout) {
            item {
                val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
                ListGroup(
                    title = null,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    NavigationListItem(
                        headline = stringResource(R.string.preferences_logout),
                        supporting = stringResource(R.string.preferences_logout_summary),
                        onClick = onLogoutClick,
                        leading = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = onErrorContainer,
                            )
                        },
                        trailing = null,
                        colors =
                            AbstrakListItemDefaults.colors(
                                headlineColor = onErrorContainer,
                                supportingColor = onErrorContainer.copy(alpha = 0.8f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}
