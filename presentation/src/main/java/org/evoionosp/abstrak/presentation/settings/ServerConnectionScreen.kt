package org.evoionosp.abstrak.presentation.settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.dialog.ConfirmDialog
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListGroup
import org.evoionosp.abstrak.presentation.designsystem.component.list.NavigationListItem
import org.evoionosp.abstrak.presentation.settings.components.HeadersGroup
import org.evoionosp.abstrak.presentation.settings.components.SecurityGroup

sealed interface ServerConnectionAction {
    data object AddHeader : ServerConnectionAction

    data class EditHeader(
        val header: ServerRequestHeader,
    ) : ServerConnectionAction

    data class DeleteHeader(
        val id: String,
    ) : ServerConnectionAction

    data class SetBypassSsl(
        val enabled: Boolean,
    ) : ServerConnectionAction

    data object PickClientCertificate : ServerConnectionAction

    data object RemoveClientCertificate : ServerConnectionAction

    data object EditUserAgent : ServerConnectionAction
}

@Composable
fun ServerConnectionRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    viewModel: ConnectionSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var headerDialog by remember { mutableStateOf<HeaderDialogState>(HeaderDialogState.Hidden) }
    var showBypassConfirm by rememberSaveable { mutableStateOf(false) }
    var showUserAgentSheet by rememberSaveable { mutableStateOf(false) }
    val pickClientCertificate = rememberClientCertificatePicker(onPicked = viewModel::setClientCertificateAlias)
    ServerConnectionScreen(
        state = uiState,
        onBackClick = onBackClick,
        onAction = { action ->
            when (action) {
                ServerConnectionAction.AddHeader -> {
                    headerDialog = HeaderDialogState.Add
                }

                is ServerConnectionAction.EditHeader -> {
                    headerDialog = HeaderDialogState.Edit(action.header)
                }

                is ServerConnectionAction.DeleteHeader -> {
                    viewModel.removeHeader(action.id)
                }

                is ServerConnectionAction.SetBypassSsl -> {
                    if (action.enabled) showBypassConfirm = true else viewModel.setBypassSsl(false)
                }

                ServerConnectionAction.PickClientCertificate -> {
                    pickClientCertificate(uiState.clientCertificateAlias)
                }

                ServerConnectionAction.RemoveClientCertificate -> {
                    viewModel.setClientCertificateAlias(null)
                }

                ServerConnectionAction.EditUserAgent -> {
                    showUserAgentSheet = true
                }
            }
        },
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    )
    when (val dialog = headerDialog) {
        HeaderDialogState.Hidden -> {
            Unit
        }

        HeaderDialogState.Add, is HeaderDialogState.Edit -> {
            HeaderEditorDialog(
                initial = (dialog as? HeaderDialogState.Edit)?.header,
                headers = uiState.headers,
                onDismiss = { headerDialog = HeaderDialogState.Hidden },
                onSave = {
                    viewModel.saveHeader(it)
                    headerDialog = HeaderDialogState.Hidden
                },
            )
        }
    }
    if (showBypassConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.server_connection_bypass_ssl_confirm_title),
            message = stringResource(R.string.server_connection_bypass_ssl_confirm_message),
            confirmLabel = stringResource(R.string.server_connection_bypass_ssl_confirm),
            onConfirm = {
                viewModel.setBypassSsl(true)
                showBypassConfirm = false
            },
            onDismiss = { showBypassConfirm = false },
            destructive = true,
        )
    }
    if (showUserAgentSheet) {
        UserAgentSheet(
            current = uiState.userAgent,
            onSave = {
                viewModel.setUserAgent(it)
                showUserAgentSheet = false
            },
            onReset = {
                viewModel.resetUserAgent()
                showUserAgentSheet = false
            },
            onDismiss = { showUserAgentSheet = false },
        )
    }
}

@Composable
fun ServerConnectionScreen(
    state: ConnectionSettingsUiState,
    onBackClick: () -> Unit,
    onAction: (ServerConnectionAction) -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    SettingsScaffold(
        title = stringResource(R.string.preferences_server_connection),
        onBack = onBackClick,
        modifier = modifier,
        miniTrailPadding = miniTrailPadding,
    ) {
        item {
            SettingsIntro(text = stringResource(R.string.server_connection_intro))
        }
        item {
            HeadersGroup(headers = state.headers, onAction = onAction)
        }
        item {
            SecurityGroup(
                bypassSsl = state.bypassSsl,
                clientCertificateAlias = state.clientCertificateAlias,
                onAction = onAction,
            )
        }
        item {
            ListGroup(title = stringResource(R.string.server_connection_user_agent)) {
                NavigationListItem(
                    headline = stringResource(R.string.server_connection_user_agent),
                    supporting = state.userAgent,
                    supportingMaxLines = 2,
                    onClick = { onAction(ServerConnectionAction.EditUserAgent) },
                )
            }
        }
    }
}
