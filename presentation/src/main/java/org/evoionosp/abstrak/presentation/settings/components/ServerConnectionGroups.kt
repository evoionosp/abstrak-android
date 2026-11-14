package org.evoionosp.abstrak.presentation.settings.components
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.GroupDivider
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListGroup
import org.evoionosp.abstrak.presentation.designsystem.component.list.ListItemChevron
import org.evoionosp.abstrak.presentation.designsystem.component.list.NavigationListItem
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.settings.ServerConnectionAction

@Composable
internal fun HeadersGroup(
    headers: List<ServerRequestHeader>,
    onAction: (ServerConnectionAction) -> Unit,
) {
    ListGroup(title = stringResource(R.string.server_connection_headers)) {
        if (headers.isEmpty()) {
            Text(
                text = stringResource(R.string.server_connection_headers_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AbstrakSpacing.content, vertical = AbstrakSpacing.item),
            )
        } else {
            headers.forEachIndexed { index, header ->
                if (index > 0) GroupDivider()
                NavigationListItem(
                    headline = header.name,
                    supporting = header.value,
                    supportingMaxLines = 2,
                    onClick = { onAction(ServerConnectionAction.EditHeader(header)) },
                    trailing = {
                        IconButton(onClick = { onAction(ServerConnectionAction.DeleteHeader(header.id)) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.server_connection_delete_header),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            }
        }
        GroupDivider()
        FilledTonalButton(
            onClick = { onAction(ServerConnectionAction.AddHeader) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(R.string.server_connection_add_header))
        }
    }
}

@Composable
internal fun SecurityGroup(
    bypassSsl: Boolean,
    clientCertificateAlias: String?,
    onAction: (ServerConnectionAction) -> Unit,
) {
    ListGroup(title = stringResource(R.string.server_connection_security)) {
        SwitchListItem(
            headline = stringResource(R.string.server_connection_bypass_ssl),
            supporting = stringResource(R.string.server_connection_bypass_ssl_desc),
            checked = bypassSsl,
            onCheckedChange = { onAction(ServerConnectionAction.SetBypassSsl(it)) },
        )
        GroupDivider()
        NavigationListItem(
            headline = stringResource(R.string.server_connection_client_cert),
            supporting = clientCertificateAlias ?: stringResource(R.string.server_connection_client_cert_none),
            supportingMaxLines = 1,
            onClick = { onAction(ServerConnectionAction.PickClientCertificate) },
            trailing = {
                if (clientCertificateAlias != null) {
                    IconButton(onClick = { onAction(ServerConnectionAction.RemoveClientCertificate) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.server_connection_client_cert_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ListItemChevron()
                }
            },
        )
    }
}
